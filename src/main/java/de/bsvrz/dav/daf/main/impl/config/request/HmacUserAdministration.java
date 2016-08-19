/*
 * Copyright 2016 by Kappich Systemberatung Aachen
 * 
 * This file is part of de.bsvrz.dav.daf.
 * 
 * de.bsvrz.dav.daf is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * de.bsvrz.dav.daf is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with de.bsvrz.dav.daf; If not, see <http://www.gnu.org/licenses/>.

 * Contact Information:
 * Kappich Systemberatung
 * Martin-Luther-Straße 14
 * 52062 Aachen, Germany
 * phone: +49 241 4090 436 
 * mail: <info@kappich.de>
 */

package de.bsvrz.dav.daf.main.impl.config.request;

import de.bsvrz.dav.daf.communication.srpAuthentication.SrpNotSupportedException;
import de.bsvrz.dav.daf.communication.srpAuthentication.SrpVerifierAndUser;
import de.bsvrz.dav.daf.communication.srpAuthentication.SrpVerifierData;
import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.DataAndATGUsageInformation;
import de.bsvrz.dav.daf.main.config.ConfigurationChangeException;
import de.bsvrz.dav.daf.main.config.ConfigurationTaskException;
import de.bsvrz.dav.daf.main.config.MutableCollectionChangeListener;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dav.daf.main.config.management.UserAdministration;
import de.bsvrz.dav.daf.main.impl.config.DafDynamicObjectType;
import de.bsvrz.dav.daf.main.impl.config.request.telegramManager.SenderReceiverCommunication;
import de.bsvrz.sys.funclib.crypt.EncryptDecryptProcedure;
import de.bsvrz.sys.funclib.crypt.encrypt.EncryptFactory;
import de.bsvrz.sys.funclib.dataSerializer.Deserializer;
import de.bsvrz.sys.funclib.dataSerializer.NoSuchVersionException;
import de.bsvrz.sys.funclib.dataSerializer.Serializer;
import de.bsvrz.sys.funclib.dataSerializer.SerializingFactory;
import de.bsvrz.sys.funclib.debug.Debug;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * Klasse für die Benutzerverwaltung nach dem alten Verfahren Hmac
 *
 * @author Kappich Systemberatung
 */
public class HmacUserAdministration implements UserAdministration {
	protected static final Debug _debug = Debug.getLogger();
	
	/** Verbidung zum Datenverteiler. Wird benötigt um die Verbindung zum Datenverteiler abzumelden, falls es bei Anfragen zu schweren Fehlern gekommen ist. */
	protected final ClientDavInterface _connection;
	
	/** Objekt, das es ermöglicht die Benutzer einer Konfigurations zu verwalten (Benutzer erstellen, Passwörter ändern, usw.). */
	private final SenderReceiverCommunication _senderUserAdministration;
	
	private EncryptDecryptProcedure _encryptDecryptProcedure = EncryptDecryptProcedure.PBEWithMD5AndDES;

	public HmacUserAdministration(ClientDavInterface connection, final SenderReceiverCommunication senderUserAdministration) {
		_connection = connection;
		_senderUserAdministration = senderUserAdministration;
	}

	/**
	 * Prüft ein Data ob es den richtigen Nachrichtentyp enthält. Ist das Data vom richtigen Typ, wird das Byte-Array des DataŽs genommen und einem
	 * Deserialisierer übergeben.
	 *
	 * @param reply               Antwort der Konfiguration auf einen Konfigurationsanfrage
	 * @param expectedMessageType Typ des Telegramms, den die Konfiguration verschickt, wenn der Auftrag ohne Probleme bearbeitet werden konnte
	 *
	 * @return Objekt, über das Daten ausgelesen werden können
	 *
	 * @throws RequestException             Technischer Fehler auf Seiten der Konfiguration oder auf Seiten des Clients bei der Übertragung des Auftrags. Dieser
	 *                                      Fehler ist nicht zu beheben.
	 * @throws de.bsvrz.dav.daf.main.config.ConfigurationChangeException Der Auftrag wurde von der Konfiguration empfangen, allerdings weigert sich die Konfiguration die Änderung auszuführen.
	 *                                      Dies kann unterschiedliche Gründe haben (mangelnde Rechte, Randbediengungen nicht erfüllt, usw.), aber in allen Fällen
	 *                                      können weitere Anfragen gestellt werden.
	 * @throws de.bsvrz.dav.daf.main.config.ConfigurationTaskException   Der Auftrag wurde von der Konfiguration empfangen, allerdings konnte die Konfiguration den Auftrag nicht ausführen,
	 *                                      weil bestimmte aufgabenspezifische Randbediengungen nicht erfüllt wurde.
	 */
	Deserializer getMessageDeserializer2(Data reply, String expectedMessageType)
			throws RequestException, ConfigurationTaskException, ConfigurationChangeException {
		String messageType = reply.getTextValue("nachrichtenTyp").getValueText();
		final byte[] message = reply.getScaledArray("daten").getByteArray();
		final Deserializer deserializer;
		try {
			deserializer = SerializingFactory.createDeserializer(2, new ByteArrayInputStream(message));
		}
		catch(NoSuchVersionException e) {
			throw new RequestException(e);
		}
		if(messageType.equals(expectedMessageType)) {
			return deserializer;
		}
		else if(messageType.equals("FehlerAntwort")) {
			try {
				String errorMessage = deserializer.readString();
				throw new RequestException(errorMessage);
			}
			catch(IOException e) {
				throw new RequestException("fehlerhafte FehlerAntwort empfangen");
			}
		}
		else if("KonfigurationsänderungVerweigert".equals(messageType)) {
			// Die Konfiguration verweigert nur den Auftrag, weil diverse Randbediengungen nicht erfüllt sind.
			try {
				final String reason = deserializer.readString();
				throw new ConfigurationChangeException(reason);
			}
			catch(IOException e) {
				// Die Antwort konnte nicht entschlüsselt werden
				throw new RequestException(
						"Die Konfiguration verweigert die Ausführung einer Konfigurationsänderung, aber der Grund konnte nicht entschlüsselt werden: " + e
				);
			}
		}
		else if("KonfigurationsauftragVerweigert".equals(messageType)) {
			// Die Konfiguration verweigert nur den Auftrag, weil diverse Randbediengungen nicht erfüllt sind.
			try {
				final String reason = deserializer.readString();
				throw new ConfigurationTaskException(reason);
			}
			catch(IOException e) {
				// Die Antwort konnte nicht entschlüsselt werden
				throw new RequestException(
						"Die Konfiguration verweigert die Ausführung eines Auftrages, aber der Grund konnte nicht entschlüsselt werden: " + e
				);
			}
		}
		else {
			throw new RequestException("falsche Antwort empfangen: " + messageType);
		}
	}

	/**
	 * Diese Methode verschickt ein Telegramm vom Typ "AuftragBenutzerverwaltung" und wartet anschließend auf die Antwort.
	 *
	 * @param message Nachricht, die verschickt werden soll
	 *
	 * @return Statusmeldung oder Antwort der Benutzerverwaltung auf die Anfrage. -1 falls die Anfrage keinen Rückgabewert liefert.
	 * 
	 * @throws RequestException Fehler bei der Bearbeitung des Telegramms (Der Benutzer hatte nicht die nötigen Rechte diesen Auftrag zu erteilen, usw.)
	 * @throws ConfigurationTaskException Fehler bei Bearbeitung des Auftrags auf Konfigurationsseite
	 */
	private int sendUserAdministrationTask(final byte[] message) throws RequestException, ConfigurationTaskException {
		int requestIndex;
		try {
			requestIndex = _senderUserAdministration.sendData("AuftragBenutzerverwaltung", message);

//					Data request = createRequestData("AuftragBenutzerverwaltung", message);
//					requestIndex = request.getScaledValue("anfrageIndex").intValue();
//					_debug.finer("sending request: ", request);
//					_connection.sendData(
//							new ResultData(
//									_configurationAuthority, _requestDescription, System.currentTimeMillis(), request
//							)
//					);
		}
		catch(Exception e) {
			e.printStackTrace();
			throw new RequestException(e);
		}
		Data reply = _senderUserAdministration.waitForReply(requestIndex);
		// Diese Methode wirft eine Exception, wenn der Auftrag nicht ausgeführt werden konnte.
		// Die Antwort (ein Integer), ist für bestimmte Anfragen von Interesse und wird zurückgegeben
		try {
			return getMessageDeserializer2(reply, "AuftragBenutzerverwaltungAntwort").readInt();
		}
		catch(IOException e) {
			// Falls readInt fehlschlägt, sendet die Konfiguration wohl ein leeres Datenpaket als Antwort. Daraus lässt sich schließen,
			// dass diese noch keine Antworten auf Benutzerverwaltungsaufträge unterstützt und der Rückgabewert daher auf -1 gesetzt werden kann.
			return -1;
		}
		// Andere Exceptions an aufrufende Funktion weitergeben
	}

	/**
	 * Nimmt eine beliebige Exception entgegen und meldet dann die Verbindung zum Datenverteiler ab. Nach der Abmeldung wird eine IllegalStateException geworfen.
	 *
	 * @param e Grund, warum die Verbindung abgebrochen werden muss
	 */
	private void closeConnectionAndThrowException(Exception e) {
		_connection.disconnect(true, e.getMessage());
		throw new IllegalStateException("Kommunikationsprobleme mit der Konfiguration, Verbindung zum Datenverteiler wird abgemeldet. Grund: " + e);
	}

	public void createSingleServingPassword(String orderer, String ordererPassword, String username, String singleServingPassword)
			throws ConfigurationTaskException {

		try {
			// Es wird ein Serializer zum serialisieren der übergebenen Parameter benötigt
			final ByteArrayOutputStream parameters = new ByteArrayOutputStream();
			final Serializer serializerParameters = SerializingFactory.createSerializer(parameters);
			serializerParameters.writeString(username);
			serializerParameters.writeString(singleServingPassword);
			// Verschlüsselter Auftrag
			final byte[] encryptedMessage = createTelegramByteArray(1, serializerParameters.getVersion(), parameters.toByteArray(), ordererPassword);

			final ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
			final Serializer serializer = SerializingFactory.createSerializer(serializerParameters.getVersion(), byteArrayStream);

			// Auftraggeber in Klarschrift
			serializer.writeString(orderer);

			// Benutztes Verschlüsselungsverfahren in Klarschrift
			serializer.writeString(_encryptDecryptProcedure.getName());

			// Der verschlüsselte Text
			serializer.writeInt(encryptedMessage.length);
			serializer.writeBytes(encryptedMessage);
			// Daten verschicken und auf Antwort warten
			sendUserAdministrationTask(byteArrayStream.toByteArray());
		}
		catch(IOException e) {
			e.printStackTrace();
			_debug.error("Fehler beim Serialisieren der Konfigurationsanfrage zum Ändern von Einmalpasswörtern", e);
			closeConnectionAndThrowException(e);
		}
		catch(RequestException e) {
			e.printStackTrace();
			_debug.error("Fehler beim Senden der Konfigurationsanfrage zum Ändern von Einmalpasswörtern", e);
			closeConnectionAndThrowException(e);
		}
		catch(NoSuchVersionException e) {
			e.printStackTrace();
			_debug.error("Fehler beim Serialisieren der Konfigurationsanfrage zum Ändern von Einmalpasswörtern", e);
			closeConnectionAndThrowException(e);
		}
	}

	public int getSingleServingPasswordCount(final String orderer, final String ordererPassword, final String username) throws ConfigurationTaskException {
		try {
			// Es wird ein Serializer zum serialisieren der übergebenen Parameter benötigt
			final ByteArrayOutputStream parameters = new ByteArrayOutputStream();
			final Serializer serializerParameters = SerializingFactory.createSerializer(parameters);
			serializerParameters.writeString(username);
			// Verschlüsselter Auftrag
			final byte[] encryptedMessage = createTelegramByteArray(8, serializerParameters.getVersion(), parameters.toByteArray(), ordererPassword);

			final ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
			final Serializer serializer = SerializingFactory.createSerializer(serializerParameters.getVersion(), byteArrayStream);

			// Auftraggeber in Klarschrift
			serializer.writeString(orderer);

			// Benutztes Verschlüsselungsverfahren in Klarschrift
			serializer.writeString(_encryptDecryptProcedure.getName());

			// Der verschlüsselte Text
			serializer.writeInt(encryptedMessage.length);
			serializer.writeBytes(encryptedMessage);
			// Daten verschicken und auf Antwort warten
			return sendUserAdministrationTask(byteArrayStream.toByteArray());
		}
		catch(IOException e) {
			e.printStackTrace();
			_debug.error("Fehler beim Serialisieren der Konfigurationsanfrage zum Ändern von Einmalpasswörtern", e);
			closeConnectionAndThrowException(e);
		}
		catch(RequestException e) {
			e.printStackTrace();
			_debug.error("Fehler beim Senden der Konfigurationsanfrage zum Ändern von Einmalpasswörtern", e);
			closeConnectionAndThrowException(e);
		}
		catch(NoSuchVersionException e) {
			e.printStackTrace();
			_debug.error("Fehler beim Serialisieren der Konfigurationsanfrage zum Ändern von Einmalpasswörtern", e);
			closeConnectionAndThrowException(e);
		}
		return -1;
	}

	public void clearSingleServingPasswords(final String orderer, final String ordererPassword, final String username) throws ConfigurationTaskException {
		try {
			// Es wird ein Serializer zum serialisieren der übergebenen Parameter benötigt
			final ByteArrayOutputStream parameters = new ByteArrayOutputStream();
			final Serializer serializerParameters = SerializingFactory.createSerializer(parameters);
			serializerParameters.writeString(username);
			// Verschlüsselter Auftrag
			final byte[] encryptedMessage = createTelegramByteArray(6, serializerParameters.getVersion(), parameters.toByteArray(), ordererPassword);

			final ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
			final Serializer serializer = SerializingFactory.createSerializer(serializerParameters.getVersion(), byteArrayStream);

			// Auftraggeber in Klarschrift
			serializer.writeString(orderer);

			// Benutztes Verschlüsselungsverfahren in Klarschrift
			serializer.writeString(_encryptDecryptProcedure.getName());

			// Der verschlüsselte Text
			serializer.writeInt(encryptedMessage.length);
			serializer.writeBytes(encryptedMessage);
			// Daten verschicken und auf Antwort warten
			sendUserAdministrationTask(byteArrayStream.toByteArray());
		}
		catch(IOException e) {
			e.printStackTrace();
			_debug.error("Fehler beim Serialisieren der Konfigurationsanfrage zum Ändern von Einmalpasswörtern", e);
			closeConnectionAndThrowException(e);
		}
		catch(RequestException e) {
			e.printStackTrace();
			_debug.error("Fehler beim Senden der Konfigurationsanfrage zum Ändern von Einmalpasswörtern", e);
			closeConnectionAndThrowException(e);
		}
		catch(NoSuchVersionException e) {
			e.printStackTrace();
			_debug.error("Fehler beim Serialisieren der Konfigurationsanfrage zum Ändern von Einmalpasswörtern", e);
			closeConnectionAndThrowException(e);
		}
	}

	public void createNewUser(
			String orderer,
			String ordererPassword,
			String newUsername,
			String newUserPid,
			String newPassword,
			boolean adminRights,
			String pidConfigurationArea
	) throws ConfigurationTaskException {
		try {
			// Es wird ein Serializer zum serialisieren der übergebenen Parameter benötigt
			final ByteArrayOutputStream parameters = new ByteArrayOutputStream();
			final Serializer serializerParameters = SerializingFactory.createSerializer(parameters);
			serializerParameters.writeString(newUsername);
			serializerParameters.writeString(newUserPid);
			serializerParameters.writeString(newPassword);
			serializerParameters.writeBoolean(adminRights);
			serializerParameters.writeString(pidConfigurationArea);
			// Verschlüsselter Auftrag
			final byte[] encryptedMessage = createTelegramByteArray(2, serializerParameters.getVersion(), parameters.toByteArray(), ordererPassword);

			final ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
			final Serializer serializer = SerializingFactory.createSerializer(serializerParameters.getVersion(), byteArrayStream);

			// Auftraggeber in Klarschrift
			serializer.writeString(orderer);

			// Benutztes Verschlüsselungsverfahren in Klarschrift
			serializer.writeString(_encryptDecryptProcedure.getName());

			// Der verschlüsselte Text
			serializer.writeInt(encryptedMessage.length);
			serializer.writeBytes(encryptedMessage);

			// Daten verschicken und auf Antwort warten
			sendUserAdministrationTask(byteArrayStream.toByteArray());
		}
		catch(IOException e) {
			e.printStackTrace();
			_debug.error("Fehler beim Serialisieren der Konfigurationsanfrage zum Anlegen eines neuen Benutzers", e);
			closeConnectionAndThrowException(e);
		}
		catch(RequestException e) {
			e.printStackTrace();
			_debug.error("Fehler beim Senden der Konfigurationsanfrage zum Anlegen eines neuen Benutzers", e);
			closeConnectionAndThrowException(e);
		}
		catch(NoSuchVersionException e) {
			e.printStackTrace();
			_debug.error("Fehler beim Serialisieren der Konfigurationsanfrage zum Anlegen eines neuen Benutzers", e);
			closeConnectionAndThrowException(e);
		}
	}

	public void deleteUser(final String orderer, final String ordererPassword, final String userToDelete) throws ConfigurationTaskException {
		try {
			// Es wird ein Serializer zum serialisieren der übergebenen Parameter benötigt
			final ByteArrayOutputStream parameters = new ByteArrayOutputStream();
			final Serializer serializerParameters = SerializingFactory.createSerializer(parameters);
			serializerParameters.writeString(userToDelete);
			// Verschlüsselter Auftrag
			final byte[] encryptedMessage = createTelegramByteArray(5, serializerParameters.getVersion(), parameters.toByteArray(), ordererPassword);

			final ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
			final Serializer serializer = SerializingFactory.createSerializer(serializerParameters.getVersion(), byteArrayStream);

			// Auftraggeber in Klarschrift
			serializer.writeString(orderer);

			// Benutztes Verschlüsselungsverfahren in Klarschrift
			serializer.writeString(_encryptDecryptProcedure.getName());

			// Der verschlüsselte Text
			serializer.writeInt(encryptedMessage.length);
			serializer.writeBytes(encryptedMessage);
			// Daten verschicken und auf Antwort warten
			sendUserAdministrationTask(byteArrayStream.toByteArray());
		}
		catch(IOException e) {
			e.printStackTrace();
			_debug.error("Fehler beim Serialisieren der Konfigurationsanfrage zum Ändern von Einmalpasswörtern", e);
			closeConnectionAndThrowException(e);
		}
		catch(RequestException e) {
			e.printStackTrace();
			_debug.error("Fehler beim Senden der Konfigurationsanfrage zum Ändern von Einmalpasswörtern", e);
			closeConnectionAndThrowException(e);
		}
		catch(NoSuchVersionException e) {
			e.printStackTrace();
			_debug.error("Fehler beim Serialisieren der Konfigurationsanfrage zum Ändern von Einmalpasswörtern", e);
			closeConnectionAndThrowException(e);
		}
	}

	public boolean isUserAdmin(final String orderer, final String ordererPassword, final String username) throws ConfigurationTaskException {
		try {
			// Es wird ein Serializer zum serialisieren der übergebenen Parameter benötigt
			final ByteArrayOutputStream parameters = new ByteArrayOutputStream();
			final Serializer serializerParameters = SerializingFactory.createSerializer(parameters);
			serializerParameters.writeString(username);
			// Verschlüsselter Auftrag
			final byte[] encryptedMessage = createTelegramByteArray(7, serializerParameters.getVersion(), parameters.toByteArray(), ordererPassword);

			final ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
			final Serializer serializer = SerializingFactory.createSerializer(serializerParameters.getVersion(), byteArrayStream);

			// Auftraggeber in Klarschrift
			serializer.writeString(orderer);

			// Benutztes Verschlüsselungsverfahren in Klarschrift
			serializer.writeString(_encryptDecryptProcedure.getName());

			// Der verschlüsselte Text
			serializer.writeInt(encryptedMessage.length);
			serializer.writeBytes(encryptedMessage);
			// Daten verschicken und auf Antwort warten
			return sendUserAdministrationTask(byteArrayStream.toByteArray()) == 1;
		}
		catch(IOException e) {
			e.printStackTrace();
			_debug.error("Fehler beim Serialisieren der Konfigurationsanfrage zum Ändern von Einmalpasswörtern", e);
			closeConnectionAndThrowException(e);
		}
		catch(RequestException e) {
			e.printStackTrace();
			_debug.error("Fehler beim Senden der Konfigurationsanfrage zum Ändern von Einmalpasswörtern", e);
			closeConnectionAndThrowException(e);
		}
		catch(NoSuchVersionException e) {
			e.printStackTrace();
			_debug.error("Fehler beim Serialisieren der Konfigurationsanfrage zum Ändern von Einmalpasswörtern", e);
			closeConnectionAndThrowException(e);
		}
		return false; //Sollte nicht erreicht werden
	}

	public boolean isUserValid(final String orderer, final String ordererPassword, final String username) throws ConfigurationTaskException {
		try {
			// Es wird ein Serializer zum serialisieren der übergebenen Parameter benötigt
			final ByteArrayOutputStream parameters = new ByteArrayOutputStream();
			final Serializer serializerParameters = SerializingFactory.createSerializer(parameters);
			serializerParameters.writeString(username);
			// Verschlüsselter Auftrag
			final byte[] encryptedMessage = createTelegramByteArray(10, serializerParameters.getVersion(), parameters.toByteArray(), ordererPassword);

			final ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
			final Serializer serializer = SerializingFactory.createSerializer(serializerParameters.getVersion(), byteArrayStream);

			// Auftraggeber in Klarschrift
			serializer.writeString(orderer);

			// Benutztes Verschlüsselungsverfahren in Klarschrift
			serializer.writeString(_encryptDecryptProcedure.getName());

			// Der verschlüsselte Text
			serializer.writeInt(encryptedMessage.length);
			serializer.writeBytes(encryptedMessage);
			// Daten verschicken und auf Antwort warten
			return sendUserAdministrationTask(byteArrayStream.toByteArray()) == 1;
		}
		catch(IOException e) {
			e.printStackTrace();
			_debug.error("Fehler beim Serialisieren der Konfigurationsanfrage zum Ändern von Einmalpasswörtern", e);
			closeConnectionAndThrowException(e);
		}
		catch(RequestException e) {
			e.printStackTrace();
			_debug.error("Fehler beim Senden der Konfigurationsanfrage zum Ändern von Einmalpasswörtern", e);
			closeConnectionAndThrowException(e);
		}
		catch(NoSuchVersionException e) {
			e.printStackTrace();
			_debug.error("Fehler beim Serialisieren der Konfigurationsanfrage zum Ändern von Einmalpasswörtern", e);
			closeConnectionAndThrowException(e);
		}
		return false; //Sollte nicht erreicht werden
	}

	public void createNewUser(
			final String orderer,
			final String ordererPassword,
			final String newUsername,
			final String newUserPid,
			final String newPassword,
			final boolean adminRights,
			final String pidConfigurationArea,
			final Collection<DataAndATGUsageInformation> data) throws ConfigurationTaskException {
		try {

			if(data == null || data.size() == 0) {
				//Wenn keine Konfigurationsdaten mitgeliefert werden sollen, createNewUser-Methode aufrufen, die diese nicht mitüberträgt.
				createNewUser(orderer, ordererPassword, newUsername, newUserPid, newPassword, adminRights, pidConfigurationArea);
				return;
			}

			// Es wird ein Serializer zum serialisieren der übergebenen Parameter benötigt
			final ByteArrayOutputStream parameters = new ByteArrayOutputStream();
			final Serializer serializerParameters = SerializingFactory.createSerializer(parameters);
			serializerParameters.writeString(newUsername);
			serializerParameters.writeString(newUserPid);
			serializerParameters.writeString(newPassword);
			serializerParameters.writeBoolean(adminRights);
			serializerParameters.writeString(pidConfigurationArea);
			//DataAndATGUsageInformation serialisieren, es wurde am Anfang der Funktion geprüft, ob data null ist.
			serializerParameters.writeInt(data.size());
			for(DataAndATGUsageInformation dataAndATGUsageInformation : data) {
				serializerParameters.writeObjectReference(dataAndATGUsageInformation.getAttributeGroupUsage());
				serializerParameters.writeData(dataAndATGUsageInformation.getData());
			}
			
			// Verschlüsselter Auftrag
			final byte[] encryptedMessage = createTelegramByteArray(9, serializerParameters.getVersion(), parameters.toByteArray(), ordererPassword);

			final ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
			final Serializer serializer = SerializingFactory.createSerializer(serializerParameters.getVersion(), byteArrayStream);

			// Auftraggeber in Klarschrift
			serializer.writeString(orderer);

			// Benutztes Verschlüsselungsverfahren in Klarschrift
			serializer.writeString(_encryptDecryptProcedure.getName());

			// Der verschlüsselte Text
			serializer.writeInt(encryptedMessage.length);
			serializer.writeBytes(encryptedMessage);

			// Daten verschicken und auf Antwort warten
			sendUserAdministrationTask(byteArrayStream.toByteArray());
		}
		catch(IOException e) {
			e.printStackTrace();
			_debug.error("Fehler beim Serialisieren der Konfigurationsanfrage zum Anlegen eines neuen Benutzers", e);
			closeConnectionAndThrowException(e);
		}
		catch(RequestException e) {
			e.printStackTrace();
			_debug.error("Fehler beim Senden der Konfigurationsanfrage zum Anlegen eines neuen Benutzers", e);
			closeConnectionAndThrowException(e);
		}
		catch(NoSuchVersionException e) {
			e.printStackTrace();
			_debug.error("Fehler beim Serialisieren der Konfigurationsanfrage zum Anlegen eines neuen Benutzers", e);
			closeConnectionAndThrowException(e);
		}
	}

	public List<SystemObject> subscribeUserChangeListener(MutableCollectionChangeListener listener) {
		final DafDynamicObjectType userType = (DafDynamicObjectType)_connection.getDataModel().getType("typ.benutzer");
		if(listener != null){
			userType.addChangeListener((short)0, listener);
		}
		return userType.getElements();
	}

	public void unsubscribeUserChangeListener(final MutableCollectionChangeListener listener) {
		final DafDynamicObjectType userType = (DafDynamicObjectType)_connection.getDataModel().getType("typ.benutzer");
		if(listener != null){
			userType.removeChangeListener((short)0, listener) ;
		}
	}

	@Override
	public SrpVerifierAndUser getSrpVerifier(final String orderer, final String ordererPassword, final String username, final int passwordIndex) throws ConfigurationTaskException, SrpNotSupportedException {
		throw new SrpNotSupportedException("Die verwendete Konfiguration unterstützt keine SRP-Authentifizierung und muss aktualisiert werden");
	}

	@Override
	public void disableOneTimePassword(final String orderer, final String ordererPassword, final String username, final int passwordIndex) throws ConfigurationTaskException {
		throw new SrpNotSupportedException("Die verwendete Konfiguration unterstützt diese Anfrage nicht und muss aktualisiert werden");
	}

	@Override
	public int createOneTimePasswords(final String orderer, final String ordererPassword, final String username, final String... passwords) throws ConfigurationTaskException {
		throw new SrpNotSupportedException("Die verwendete Konfiguration unterstützt diese Anfrage nicht und muss aktualisiert werden");
	}

	@Override
	public int[] getValidOneTimePasswordIDs(final String orderer, final String ordererPassword, final String username) throws ConfigurationTaskException {
		throw new SrpNotSupportedException("Die verwendete Konfiguration unterstützt diese Anfrage nicht und muss aktualisiert werden");
	}

	@Override
	public void setSrpVerifier(final String orderer, final String ordererPassword, final String user, final SrpVerifierData verifier) throws ConfigurationTaskException {
		throw new SrpNotSupportedException("Die verwendete Konfiguration unterstützt diese Anfrage nicht und muss aktualisiert werden");
	}

	public void changeUserRights(String orderer, String ordererPassword, String user, boolean adminRights) throws ConfigurationTaskException {
		try {
			// Es wird ein Serializer zum serialisieren der übergebenen Parameter benötigt
			final ByteArrayOutputStream parameters = new ByteArrayOutputStream();
			final Serializer serializerParameters = SerializingFactory.createSerializer(parameters);
			serializerParameters.writeString(user);
			serializerParameters.writeBoolean(adminRights);
			// Verschlüsselter Auftrag
			final byte[] encryptedMessage = createTelegramByteArray(4, serializerParameters.getVersion(), parameters.toByteArray(), ordererPassword);

			final ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
			final Serializer serializer = SerializingFactory.createSerializer(serializerParameters.getVersion(), byteArrayStream);

			// Auftraggeber in Klarschrift
			serializer.writeString(orderer);

			// Benutztes Verschlüsselungsverfahren in Klarschrift
			serializer.writeString(_encryptDecryptProcedure.getName());

			// Der verschlüsselte Text
			serializer.writeInt(encryptedMessage.length);
			serializer.writeBytes(encryptedMessage);

			// Daten verschicken und auf Antwort warten
			sendUserAdministrationTask(byteArrayStream.toByteArray());
		}
		catch(IOException e) {
			e.printStackTrace();
			_debug.error("Fehler beim Serialisieren der Konfigurationsanfrage zum Ändern von Benutzerrechten", e);
			closeConnectionAndThrowException(e);
		}
		catch(RequestException e) {
			e.printStackTrace();
			_debug.error("Fehler beim Senden der Konfigurationsanfrage zum Ändern von Benutzerrechten", e);
			closeConnectionAndThrowException(e);
		}
		catch(NoSuchVersionException e) {
			e.printStackTrace();
			_debug.error("Fehler beim Serialisieren der Konfigurationsanfrage zum Ändern von Benutzerrechten", e);
			closeConnectionAndThrowException(e);
		}
	}

	public void changeUserPassword(String orderer, String ordererPassword, String user, String newPassword) throws ConfigurationTaskException {
		try {
			// Es wird ein Serializer zum serialisieren der übergebenen Parameter benötigt
			final ByteArrayOutputStream parameters = new ByteArrayOutputStream();
			final Serializer serializerParameters = SerializingFactory.createSerializer(parameters);
			serializerParameters.writeString(user);
			serializerParameters.writeString(newPassword);
			// Verschlüsselter Auftrag
			final byte[] encryptedMessage = createTelegramByteArray(3, serializerParameters.getVersion(), parameters.toByteArray(), ordererPassword);

			final ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
			final Serializer serializer = SerializingFactory.createSerializer(serializerParameters.getVersion(), byteArrayStream);

			// Auftraggeber in Klarschrift
			serializer.writeString(orderer);

			// Benutztes Verschlüsselungsverfahren in Klarschrift
			serializer.writeString(_encryptDecryptProcedure.getName());

			// Der verschlüsselte Text
			serializer.writeInt(encryptedMessage.length);
			serializer.writeBytes(encryptedMessage);

			// Daten verschicken und auf Antwort warten
			sendUserAdministrationTask(byteArrayStream.toByteArray());
		}
		catch(IOException e) {
			e.printStackTrace();
			_debug.error("Fehler beim Serialisieren der Konfigurationsanfrage zum Ändern des Benutzerpassworts", e);
			closeConnectionAndThrowException(e);
		}
		catch(RequestException e) {
			e.printStackTrace();
			_debug.error("Fehler beim Senden der Konfigurationsanfrage zum Ändern des Benutzerpassworts", e);
			closeConnectionAndThrowException(e);
		}
		catch(NoSuchVersionException e) {
			e.printStackTrace();
			_debug.error("Fehler beim Serialisieren der Konfigurationsanfrage zum Ändern des Benutzerpassworts", e);
			closeConnectionAndThrowException(e);
		}
	}

	/**
	 * Fordert von der Konfiguration ein Zufallstext an, dieser wird dann mit einem Auftrag versendet
	 *
	 * @return Zufallstext, der durch die Konfiguration erzeugt wurde
	 */
	private byte[] getRandomText() throws IOException, RequestException, NoSuchVersionException {
		int requestIndex;
		try {
			requestIndex = _senderUserAdministration.sendData("AuftragZufallstext", new byte[]{1});
		}
		catch(Exception e) {
			e.printStackTrace();
			throw new RequestException(e);
		}
		final Data reply = _senderUserAdministration.waitForReply(requestIndex);
		// Das ist der Zufallstext
		final Deserializer deserializer = SerializingFactory.createDeserializer(2, new ByteArrayInputStream(reply.getScaledArray("daten").getByteArray()));
		final int sizeOfData = deserializer.readInt();
		return deserializer.readBytes(sizeOfData);
	}

	/**
	 * Erzeugt ein kodiertes Byte-Array, das folgenden Aufbau besitzt:<br> - benutzte Serialisiererversion(Wert ist nicht serialisiert) (ersten 4 Bytes)<br> - Typ
	 * des Pakets (int)<br> - Länge des Zufallstexts (int) - Zufallstext (byte[]) - übergebenes Byte-Array <code>messageCleartext</code>
	 *
	 * @param messageType       Nachrichtentyp
	 * @param serializerVersion Version, mit der die Daten serialisiert werden sollen
	 * @param messageCleartext  Bisher erzeugte Daten, die verschickt werden sollen
	 *
	 * @return verschlüsseltes Byte-Array, das alle oben genannten Daten enthält
	 *
	 * @throws RequestException Alle Fehler die auftauchen werden als RequestException interpretiert. Dies wird gemacht, da eine weitere Übertragung keinen Sinn
	 *                          macht.
	 */
	private byte[] createTelegramByteArray(int messageType, int serializerVersion, byte[] messageCleartext, String encryptionText) throws RequestException {
		try {
			// Zufallstext von der Konfiguration anfordern
			byte[] randomText = getRandomText();

			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			final Serializer serializer = SerializingFactory.createSerializer(serializerVersion, out);

			serializer.writeInt(messageType);
			serializer.writeInt(randomText.length);
			serializer.writeBytes(randomText);
			// Klartext schreiben. Was ausgelesen werden kann, weiss der Empfänger (longs, ints, ....)
			serializer.writeBytes(messageCleartext);

			final byte[] randomStringAndCleartextMessage = out.toByteArray();

			// Die ersten 4 Bytes enhalten die Serialiszerversion
			final byte[] wholeMessage = new byte[4 + randomStringAndCleartextMessage.length];

			// Das höherwärtigste Byte steht in Zelle 0
			wholeMessage[0] = (byte)((serializerVersion & 0xff000000) >>> 24);
			wholeMessage[1] = (byte)((serializerVersion & 0x00ff0000) >>> 16);
			wholeMessage[2] = (byte)((serializerVersion & 0x0000ff00) >>> 8);
			wholeMessage[3] = (byte)(serializerVersion & 0x000000ff);

			// Array mit Zufallstext kopieren
			System.arraycopy(randomStringAndCleartextMessage, 0, wholeMessage, 4, randomStringAndCleartextMessage.length);

			return EncryptFactory.getEncryptInstance(_encryptDecryptProcedure).encrypt(wholeMessage, encryptionText);
		}
		catch(Exception e) {
			throw new RequestException(e);
		}
	}
}
