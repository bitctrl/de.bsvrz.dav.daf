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

import de.bsvrz.dav.daf.communication.lowLevel.telegrams.SrpAnswer;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.SrpRequest;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.SrpValidateAnswer;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.SrpValidateRequest;
import de.bsvrz.dav.daf.communication.protocol.UserLogin;
import de.bsvrz.dav.daf.communication.srpAuthentication.*;
import de.bsvrz.dav.daf.main.*;
import de.bsvrz.dav.daf.main.authentication.ClientCredentials;
import de.bsvrz.dav.daf.main.config.ConfigurationTaskException;
import de.bsvrz.dav.daf.main.config.MutableCollectionChangeListener;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dav.daf.main.config.management.UserAdministration;
import de.bsvrz.dav.daf.main.impl.config.DafDynamicObjectType;
import de.bsvrz.dav.daf.main.impl.config.request.telegramManager.SenderReceiverCommunication;
import de.bsvrz.sys.funclib.dataSerializer.Deserializer;
import de.bsvrz.sys.funclib.dataSerializer.NoSuchVersionException;
import de.bsvrz.sys.funclib.dataSerializer.Serializer;
import de.bsvrz.sys.funclib.dataSerializer.SerializingFactory;
import de.bsvrz.sys.funclib.debug.Debug;

import java.io.*;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Klasse für die Benutzerverwaltung mit SRP-Authentifizierung
 *
 * @author Kappich Systemberatung
 */
public class SrpUserAdministration implements UserAdministration {
	private static final Debug _debug = Debug.getLogger();
	
	/** Verbindung zum Datenverteiler. Wird benötigt um die Verbindung zum Datenverteiler abzumelden, falls es bei Anfragen zu schweren Fehlern gekommen ist. */
	private final ClientDavInterface _connection;
	
	/** Objekt, das es ermöglicht die Benutzer einer Konfigurations zu verwalten (Benutzer erstellen, Passwörter ändern, usw.). */
	private final SenderReceiverCommunication _senderUserAdministration;
	
	/** Implementierung der Verschlüssleung (null falls keien verschlüsselung aufgebaut) */
	private SrpTelegramEncryption _srpTelegramEncryption = null;
	
	/** Kryptographische Parameter nach Aushandlung mit Konfiguration */
	private SrpCryptoParameter _srpCryptoParameter = null;
	
	/** Verifier um lokale Benutzername und Passwort prüfen zu können wenn bereits eingeloggt */
	private SrpVerifierData _verifier = null;
	
	/** Der zuletzt authentifizierte Benutzer */
	private String _authenticatedUser = null;
	
	/** Authentifizierung mit Login-Token ermöglichen? */
	private boolean _isTokenLoginAllowed;

	/** 
	 * Erstellt eine neue SrpUserAdministration
	 * @param connection Datenverteilerverbindung
	 * @param senderUserAdministration Sender-Klasse im Requester
	 * @param isTokenLoginAllowed Ist die authentifizierung mit Login-Token erlaubt?
	 */
	public SrpUserAdministration(ClientDavInterface connection, final SenderReceiverCommunication senderUserAdministration, final boolean isTokenLoginAllowed) {
		_connection = connection;
		_senderUserAdministration = senderUserAdministration;
		_isTokenLoginAllowed = isTokenLoginAllowed;
	}

	/**
	 * Anmeldung bei der Konfiguration. Erfolgt bei der ersten Anfrage je neuem Auftraggeber
	 * @param userName Auftraggeber
	 * @param userPassword Auftraggeber-Passwort
	 * @throws ConfigurationTaskException
	 * @throws CommunicationError
	 * @throws InconsistentLoginException
	 */
	private synchronized void login(final String userName, final String userPassword) throws ConfigurationTaskException, CommunicationError, InconsistentLoginException {
		ClientCredentials clientCredentials = ClientCredentials.ofString(userPassword);
		if(!clientCredentials.hasPassword() && !isTokenLoginAllowed()){
			throw new InconsistentLoginException("Authentifizierung mit Login-Token bei der Benutzerverwaltung ist nicht möglich");
		}


		// SRP-Login
		if(_srpTelegramEncryption != null && _authenticatedUser.equals(userName)){
			// Bereits eingeloggt, nur prüfen
			if(SrpClientAuthentication.validateVerifier(_verifier, userName, clientCredentials)) {
				return;
			}
			// Falsches Passwort. Das falsche Passwort könnte aber trotzdem richtig sein, wenn der Benutzer sein Passwort geändert hat. Also neu authentifizieren.
		}

		SrpClientAuthentication.AuthenticationResult authenticationResult = SrpClientAuthentication.authenticate(userName, -1, clientCredentials, new SrpClientAuthentication.TelegramInterface() {
			@Override
			public SrpAnswer sendAndReceiveRequest(final SrpRequest telegram) throws CommunicationError, InconsistentLoginException, SrpNotSupportedException {
				try {
					// SRP Schritt 1
					byte[] srpAnswerBytes = sendMessage(userName, "SrpRequest", new byte[0]);
					SrpAnswer srpAnswer = new SrpAnswer();
					try(DataInputStream in = new DataInputStream(new ByteArrayInputStream(srpAnswerBytes))) {
						srpAnswer.read(in);
					}
					return srpAnswer;
				}
				catch(InconsistentLoginException e){
					throw e;
				}
				catch(Exception e) {
					if(e.getMessage().contains("java.lang.IllegalArgumentException: No enum constant")){
						// SRP wird von der Konfiguration nicht unterstützt
						throw new SrpNotSupportedException("Die verwendete Konfiguration unterstützt keine SRP-Authentifizierung");
					}
					throw new CommunicationError(e.getMessage(), e);
				}
			}

			@Override
			public SrpValidateAnswer sendAndReceiveValidateRequest(final SrpValidateRequest telegram) throws CommunicationError, InconsistentLoginException {
				try {
					final SrpValidateAnswer srpValidateAnswer = new SrpValidateAnswer();
					ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
					DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
					telegram.write(dataOutputStream);
					byte[] srpValidateAnswerBytes = sendMessage(userName, "SrpValidateRequest", byteArrayOutputStream.toByteArray());
					try(DataInputStream in = new DataInputStream(new ByteArrayInputStream(srpValidateAnswerBytes))) {
						srpValidateAnswer.read(in);
					}
					return srpValidateAnswer;
				}
				catch(InconsistentLoginException e){
					throw e;
				}
				catch(Exception e) {
					throw new CommunicationError(e.getMessage(), e);
				}
			}
		});

		_srpCryptoParameter = authenticationResult.getCryptoParams();
		_verifier =  SrpClientAuthentication.createVerifier(_srpCryptoParameter, userName, clientCredentials);
		_authenticatedUser = userName;
		_srpTelegramEncryption = new SrpTelegramEncryption(SrpUtilities.bigIntegerToBytes(authenticationResult.getSessionKey()), true, _srpCryptoParameter);
	}

	/**
	 * Ist die Authentifizierung mit Login-Token erlaubt? Standardmäßig nein.
	 * @return Ist die Authentifizierung mit Login-Token erlaubt?
	 */
	private boolean isTokenLoginAllowed() {
		return _isTokenLoginAllowed;
	}

	/**
	 * Sendet eine verschlüsselte Anfrage an die Konfiguration. Eine SRP-Authentifizierung zur Aushandlung dr Verschlüsselung mit {@link #login(String, String)}
	 * muss bereits erfolgt sein.
	 * @param orderer Auftraggeber
	 * @param ordererPassword Auftraggeber-Passwort
	 * @param query Anfragetyp
	 * @param serializer Delegate-Funktion oder Lambda-Ausdruck, die die Anfrage serialisiert. Die in dieser Funktion geschriebenen daten werden automatisch verschlüsselt
	 * @return Antwort von der Konfiguration, wird in dieser Methode automatisch entschlüsselt. Das Byte-Array kann je nach Anfrage unterschiedlich interpretiert werden.
	 * @throws ConfigurationTaskException
	 */
	private synchronized byte[] sendUserAdministrationQuery(final String orderer, final String ordererPassword, final UserAdministrationQuery query, final DataSerializer serializer) throws ConfigurationTaskException {
		try {
			login(orderer, ordererPassword);
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			final byte[] encryptedMessage;
			try(DataOutputStream out = new DataOutputStream(byteArrayOutputStream)) {
				out.writeUTF(query.name());
				serializer.serialize(out);
			}
			// Verschlüsselter Auftrag
			encryptedMessage = _srpTelegramEncryption.encrypt(byteArrayOutputStream.toByteArray());

			byte[] encryptedAnswer = sendMessage(orderer, "SrpEncrypted", encryptedMessage);
			
			return _srpTelegramEncryption.decrypt(encryptedAnswer);
		}
		catch(ConfigurationTaskException e){
			throw e;
		}
		catch(CommunicationError e) {
			e.printStackTrace();
			_debug.error("Fehler beim Stellen der Konfigurationsanfrage " + query, e);
			throw closeConnectionAndThrowException(e);
		}
		catch(InconsistentLoginException e){
			throw new ConfigurationTaskException(e.getMessage(), e);
		}	
		catch(Exception e){
			throw new ConfigurationTaskException(e);
		}
	}

	private byte[] sendMessage(final String orderer, final String messageAction, final byte[] messageBytes) throws RequestException, ConfigurationTaskException, InconsistentLoginException {
		try {
			final ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
			final Serializer serializer = SerializingFactory.createSerializer(2, byteArrayStream);

			// Auftraggeber in Klarschrift
			serializer.writeString(orderer);

			// Benutztes Verschlüsselungsverfahren
			serializer.writeString(messageAction);

			// Der verschlüsselte Text
			serializer.writeInt(messageBytes.length);
			serializer.writeBytes(messageBytes);

			// Daten verschicken und auf Antwort warten
			return sendUserAdministrationTask(byteArrayStream.toByteArray());
		}
		catch(NoSuchVersionException | IOException e) {
			throw new RequestException("Fehler beim Erzeugen der Anfrage", e);
		}
	}

	/**
	 * Prüft ein Data ob es den richtigen Nachrichtentyp enthält. Ist das Data vom richtigen Typ, wird das Byte-Array des DataŽs genommen und einem
	 * Deserialisierer übergeben.
	 *
	 * @param reply               Antwort der Konfiguration auf einen Konfigurationsanfrage
	 * @param expectedMessageType Typ des Telegramms, den die Konfiguration verschickt, wenn der Auftrag ohne Probleme bearbeitet werden konnte
	 * @return Objekt, über das Daten ausgelesen werden können
	 * @throws RequestException           Technischer Fehler auf Seiten der Konfiguration oder auf Seiten des Clients bei der Übertragung des Auftrags. Dieser
	 *                                    Fehler ist nicht zu beheben.
	 * @throws InconsistentLoginException Konfiguration verweigert Ausführung (wegen fehlender Rechte o.ä.)
	 */
	private static byte[] getMessage(Data reply, String expectedMessageType)
			throws RequestException, InconsistentLoginException {
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
			return message;
		}
		else if(messageType.equals("FehlerAntwort")) {
			try {
				String errorMessage = deserializer.readString();
				throw new RequestException(errorMessage);
			}
			catch(IOException e) {
				throw new RequestException("Fehlerhafte FehlerAntwort empfangen", e);
			}
		}
		else if("KonfigurationsänderungVerweigert".equals(messageType)) {
			// Die Konfiguration verweigert nur den Auftrag, weil diverse Randbedingungen nicht erfüllt sind.
			try {
				final String reason = deserializer.readString();
				throw new InconsistentLoginException(reason);
			}
			catch(IOException e) {
				// Die Antwort konnte nicht entschlüsselt werden
				throw new RequestException(
						"Die Konfiguration verweigert die Ausführung einer Konfigurationsänderung, aber der Grund konnte nicht entschlüsselt werden" , e
				);
			}
		}
		else if("KonfigurationsauftragVerweigert".equals(messageType)) {
			// Die Konfiguration verweigert nur den Auftrag, weil diverse Randbedingungen nicht erfüllt sind.
			try {
				final String reason = deserializer.readString();
				throw new InconsistentLoginException(reason);
			}
			catch(IOException e) {
				// Die Antwort konnte nicht entschlüsselt werden
				throw new RequestException(
						"Die Konfiguration verweigert die Ausführung eines Auftrages, aber der Grund konnte nicht entschlüsselt werden" , e
				);
			}
		}
		else {
			throw new RequestException("Falsche Antwort empfangen: " + messageType);
		}
	}

	/**
	 * Diese Methode verschickt ein Telegramm vom Typ "AuftragBenutzerverwaltung" und wartet anschließend auf die Antwort.
	 *
	 * @param message Nachricht, die verschickt werden soll
	 *
	 * @return Antwort der Benutzerverwaltung auf die Anfrage.
	 * 
	 * @throws RequestException Fehler bei der Bearbeitung des Telegramms (Der Benutzer hatte nicht die nötigen Rechte diesen Auftrag zu erteilen, usw.)
	 * @throws ConfigurationTaskException Fehler bei Bearbeitung des Auftrags auf Konfigurationsseite
	 */
	private byte[] sendUserAdministrationTask(final byte[] message) throws RequestException, InconsistentLoginException {
		int requestIndex;
		try {
			requestIndex = _senderUserAdministration.sendData("AuftragBenutzerverwaltung", message);
		}
		catch(Exception e) {
			e.printStackTrace();
			throw new RequestException(e);
		}
		Data reply = _senderUserAdministration.waitForReply(requestIndex);
		return getMessage(reply, "AuftragBenutzerverwaltungAntwort");
	}

	/**
	 * Nimmt eine beliebige Exception entgegen und meldet dann die Verbindung zum Datenverteiler ab. Nach der Abmeldung wird eine IllegalStateException geworfen.
	 *
	 * @param e Grund, warum die Verbindung abgebrochen werden muss
	 */
	private IllegalStateException closeConnectionAndThrowException(Exception e) {
		_connection.disconnect(true, e.getMessage());
		throw new IllegalStateException("Kommunikationsprobleme mit der Konfiguration, Verbindung zum Datenverteiler wird abgemeldet. Grund: " + e);
	}

	@Override
	public int getSingleServingPasswordCount(final String orderer, final String ordererPassword, final String username) throws ConfigurationTaskException {
		byte[] bytes = sendUserAdministrationQuery(orderer, ordererPassword, UserAdministrationQuery.GetOneTimePasswordCount, out -> out.writeUTF(username));
		try(DataInputStream stream = new DataInputStream(new ByteArrayInputStream(bytes))) {
			return stream.readInt();
		}
		catch(IOException e) {
			e.printStackTrace();
			_debug.error("Fehler beim Empfangen der Konfigurationsanfrage zum Abfragen von Einmalpasswörtern", e);
			throw closeConnectionAndThrowException(e);
		}
	}

	@Override
	public int[] getValidOneTimePasswordIDs(final String orderer, final String ordererPassword, final String username) throws ConfigurationTaskException {
		byte[] bytes = sendUserAdministrationQuery(orderer, ordererPassword, UserAdministrationQuery.GetOneTimePasswordIDs, out -> {
			out.writeUTF(username);
		});
		try(DataInputStream stream = new DataInputStream(new ByteArrayInputStream(bytes))) {
			int count = stream.readInt();
			if(count < 0) throw new IOException("Ungültige Anzahl: " + count);
			int[] result = new int[count];
			for(int i = 0; i < count; i++){
				result[i] = stream.readInt();
			}
			return result;
		}
		catch(IOException e) {
			e.printStackTrace();
			_debug.error("Fehler beim Empfangen der Konfigurationsanfrage zum Abfragen von Einmalpasswörtern", e);
			throw closeConnectionAndThrowException(e);
		}
	}

	@Override
	public void clearSingleServingPasswords(final String orderer, final String ordererPassword, final String username) throws ConfigurationTaskException {
		sendUserAdministrationQuery(orderer, ordererPassword, UserAdministrationQuery.ClearSingleServingPasswords, out -> out.writeUTF(username));
	}

	@Override
	public void createNewUser(
			String orderer,
			String ordererPassword,
			String newUsername,
			String newUserPid,
			String newPassword,
			boolean adminRights,
			String pidConfigurationArea
	) throws ConfigurationTaskException {
		createNewUser(orderer, ordererPassword, newUsername, newUserPid, newPassword, adminRights, pidConfigurationArea, null);
	}

	@Override
	public void deleteUser(final String orderer, final String ordererPassword, final String userToDelete) throws ConfigurationTaskException {
		sendUserAdministrationQuery(orderer, ordererPassword, UserAdministrationQuery.DeleteUser, out -> out.writeUTF(userToDelete));
	}

	@Override
	public boolean isUserAdmin(final String orderer, final String ordererPassword, final String user) throws ConfigurationTaskException {
		byte[] data = sendUserAdministrationQuery(orderer, ordererPassword, UserAdministrationQuery.IsUserAdmin, out -> out.writeUTF(user));
		return data.length > 0 && data[0] == 1;
	}

	@Override
	public boolean isUserValid(final String orderer, final String ordererPassword, final String user) throws ConfigurationTaskException {
		byte[] data = sendUserAdministrationQuery(orderer, ordererPassword, UserAdministrationQuery.IsUserValid, out -> out.writeUTF(user));
		return data.length > 0 && data[0] == 1;
	}

	@Override
	public void createNewUser(
			final String orderer,
			final String ordererPassword,
			final String newUsername,
			final String newUserPid,
			final String newPassword,
			final boolean adminRights,
			final String pidConfigurationArea,
			Collection<DataAndATGUsageInformation> data) throws ConfigurationTaskException {

		final Collection<DataAndATGUsageInformation> finalData = (data == null ? Collections.emptyList() : data);
		
		sendUserAdministrationQuery(orderer, ordererPassword, UserAdministrationQuery.CreateNewUser, out -> {
			final Serializer serializer = SerializingFactory.createSerializer(out);
			serializer.writeString(newUsername);
			serializer.writeString(newUserPid);
			ClientCredentials clientCredentials = validatePassword(newPassword);
			if(System.getProperty("srp6.disable.verifier") != null) {
				// Klartextpasswort übertragen
				serializer.writeString(newPassword);
			}
			else{
				SrpVerifierData verifier = SrpClientAuthentication.createVerifier(_srpCryptoParameter, newUsername, clientCredentials);
				serializer.writeString(verifier.toString());
			}
			serializer.writeBoolean(adminRights);
			serializer.writeString(pidConfigurationArea);
			//DataAndATGUsageInformation serialisieren, es wurde am Anfang der Funktion geprüft, ob data null ist.
			serializer.writeInt(finalData.size());
			for(DataAndATGUsageInformation dataAndATGUsageInformation : finalData) {
				serializer.writeObjectReference(dataAndATGUsageInformation.getAttributeGroupUsage());
				serializer.writeData(dataAndATGUsageInformation.getData());
			}
		});
	}

	@Override
	public void changeUserRights(String orderer, String ordererPassword, String user, boolean adminRights) throws ConfigurationTaskException {
		sendUserAdministrationQuery(orderer, ordererPassword, UserAdministrationQuery.ChangeUserRights, out -> {
			out.writeUTF(user);
			out.writeBoolean(adminRights);
		});
	}


	@Override
	public SrpVerifierAndUser getSrpVerifier(final String orderer, final String ordererPassword, final String user, final int passwordIndex) throws ConfigurationTaskException, SrpNotSupportedException {
		byte[] data = sendUserAdministrationQuery(orderer, ordererPassword, UserAdministrationQuery.GetSrpVerifier, out -> {
			out.writeUTF(user);
			out.writeInt(passwordIndex);
		});
		try(DataInputStream stream = new DataInputStream(new ByteArrayInputStream(data))) {
			long userId = stream.readLong();
			SrpVerifierData verifierData = new SrpVerifierData(stream.readUTF());
			return new SrpVerifierAndUser(UserLogin.ofLong(userId), verifierData, stream.readBoolean());
		}
		catch(IOException e) {
			e.printStackTrace();
			_debug.error("Fehler beim Empfangen der Konfigurationsanfrage zum Abfragen eines SRP-Verifiers", e);
			throw closeConnectionAndThrowException(e);
		}
	}

	@Override
	public void disableOneTimePassword(final String orderer, final String ordererPassword, final String user, final int passwordIndex) throws ConfigurationTaskException {
		sendUserAdministrationQuery(orderer, ordererPassword, UserAdministrationQuery.DisableOneTimePassword, out -> {
			out.writeUTF(user);
			out.writeInt(passwordIndex);
		});
	}

	private int setOrAppendOneTimePasswords(final String orderer, final String ordererPassword, final String user, final String[] passwords, final boolean append) throws ConfigurationTaskException {
		byte[] data = sendUserAdministrationQuery(orderer, ordererPassword, UserAdministrationQuery.SetOneTimePasswords, out -> {
			out.writeUTF(user);
			out.writeBoolean(append); // Nicht anhängen
			out.writeInt(passwords.length);
			for(String password : passwords) {
				ClientCredentials clientCredentials = validatePassword(password);
				writeVerifierOrPassword(user, out, clientCredentials);
			}
		});
		try(DataInputStream stream = new DataInputStream(new ByteArrayInputStream(data))) {
			return stream.readInt();
		}
		catch(IOException e) {
			e.printStackTrace();
			_debug.error("Fehler beim Empfangen der Konfigurationsanfrage zum Erstellen von Einmalpassworten", e);
			throw closeConnectionAndThrowException(e);
		}
	}

	/**
	 * Schreibt das Passwort bzw. den Überprüfungscode in das Telegramm
	 * @param user Benutzername
	 * @param out Telegramm-OutputStream
	 * @param clientCredentials Passwort
	 * @throws IOException IO-Fehler
	 */
	private void writeVerifierOrPassword(final String user, final DataOutputStream out, final ClientCredentials clientCredentials) throws IOException {
		if(System.getProperty("srp6.disable.verifier") != null) {
			// Klartextpasswort übertragen
			out.writeUTF(clientCredentials.toString());
		}
		else {
			// Überprüfungscode übertragen
			SrpVerifierData verifier = SrpClientAuthentication.createVerifier(_srpCryptoParameter, user, clientCredentials);
			out.writeUTF(verifier.toString());
		}
	}

	@Override
	public int createOneTimePasswords(final String orderer, final String ordererPassword, final String user, final String... passwords) throws ConfigurationTaskException {
		return setOrAppendOneTimePasswords(orderer, ordererPassword, user, passwords, true);
	}

	@Override
	public void changeUserPassword(String orderer, String ordererPassword, String user, String newPassword) throws ConfigurationTaskException {
		ClientCredentials clientCredentials = validatePassword(newPassword);
		sendUserAdministrationQuery(orderer, ordererPassword, UserAdministrationQuery.ChangeUserPassword, out -> {
			out.writeUTF(user);
			writeVerifierOrPassword(user, out, clientCredentials);
		});
	}

	@Override
	public void setSrpVerifier(final String orderer, final String ordererPassword, final String user, final SrpVerifierData verifier) throws ConfigurationTaskException {
		sendUserAdministrationQuery(orderer, ordererPassword, UserAdministrationQuery.ChangeUserPassword, out -> {
			out.writeUTF(user);
			out.writeUTF(verifier.toString());
		});
	}

	/**
	 * Überprüft ein Passwort. Diese Methode prüft aktuell nur, dass das Passwort nicht leer ist. Man könnte noch weitere Richtlinien implementieren
	 * @param newPassword Neues Passwort
	 * @return Ein ClientCredentials-Objekt, das das Passwort enthält (falls gültig)
	 * @throws IllegalArgumentException Ungültiges Passwort
	 */
	private ClientCredentials validatePassword(final String newPassword) {
		ClientCredentials clientCredentials = ClientCredentials.ofString(newPassword);
		if(clientCredentials == null){
			throw new IllegalArgumentException("Leeres Passwort");
		}
		return clientCredentials;
	}

	@Override
	public void createSingleServingPassword(String orderer, String ordererPassword, String user, String singleServingPassword)
			throws ConfigurationTaskException {
		createOneTimePasswords(orderer, ordererPassword, user, singleServingPassword);
	}

	@Override
	public List<SystemObject> subscribeUserChangeListener(MutableCollectionChangeListener listener) {
		final DafDynamicObjectType userType = (DafDynamicObjectType)_connection.getDataModel().getType("typ.benutzer");
		if(listener != null){
			userType.addChangeListener((short)0, listener);
		}
		return userType.getElements();
	}

	@Override
	public void unsubscribeUserChangeListener(final MutableCollectionChangeListener listener) {
		final DafDynamicObjectType userType = (DafDynamicObjectType)_connection.getDataModel().getType("typ.benutzer");
		if(listener != null){
			userType.removeChangeListener((short)0, listener) ;
		}
	}

	/**
	 * Interface für Serialisierungsfunktionen.
	 *
	 * @author Kappich Systemberatung
	 */
	@FunctionalInterface
	private interface DataSerializer {
		/**
		 * Schreibt Daten auf einen Stream
		 * @param out Stream
		 * @throws IOException
		 */
		void serialize(DataOutputStream out) throws IOException;
	}
}
