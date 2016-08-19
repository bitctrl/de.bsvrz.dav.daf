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

package de.bsvrz.dav.daf.userManagement;

import de.bsvrz.dav.daf.communication.protocol.UserLogin;
import de.bsvrz.dav.daf.communication.srpAuthentication.*;
import de.bsvrz.dav.daf.main.InconsistentLoginException;
import de.bsvrz.dav.daf.main.authentication.ClientCredentials;
import de.bsvrz.dav.daf.main.config.ConfigurationChangeException;
import de.bsvrz.dav.daf.main.config.ConfigurationTaskException;
import de.bsvrz.sys.funclib.filelock.FileLock;
import org.w3c.dom.*;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Klasse zur Bearbeitung der benutzerverwaltung.xml ohne eine Konfiguration starten zu müssen. Inhaltlich ähnlich mit {@link de.bsvrz.puk.config.main.authentication.ConfigAuthentication}
 */
public class UserManagementFileOffline implements UserManagementFileInterface {

	/** Als Schlüssel dient der Benutzername (String) als Value werden alle Informationen, die zu einem Benutzer gespeichert wurden, zurückgegeben. */
	private final Map<String, UserAccount> _userAccounts = new HashMap<>();

	/** XML-Datei, wird zum anlegen einer Sicherheitskopie gebraucht */
	private final File _xmlFile;

	/** Repräsentiert die vollständige XML-Datei. */
	private final Document _xmlDocument;

	private final FileLock _lockAuthenticationFile;
	
	private static final String _secretToken = new BigInteger(64, new SecureRandom()).toString(16);

	/**
	 * Lädt alle Informationen aus der angegebenen Datei.
	 * @param userFile XML-Datei, in der alle Benutzer gespeichert sind.
	 */
	public UserManagementFileOffline(File userFile) throws ParserConfigurationException {
		// Die Datei gegen doppelten Zugriff sichern
		_lockAuthenticationFile = new FileLock(userFile);
		try {
			_lockAuthenticationFile.lock();
		}
		catch(IOException e) {
			final String errorMessage =
					"IOException beim Versuch die lock-Datei zu schreiben. Datei, die gesichert werden sollte " + userFile.getAbsolutePath();
			throw new RuntimeException(errorMessage, e);
		}

		try {
			_xmlFile = userFile.getCanonicalFile();
		}
		catch(IOException e) {
			throw new IllegalArgumentException(e);
		}
		
		// Es gibt die Datei, also Daten auslesen
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);

		// die Validierung der XML-Datei anhand der DTD durchführen
		factory.setValidating(true);
		factory.setAttribute("http://xml.org/sax/features/validation", Boolean.TRUE);
		DocumentBuilder builder = factory.newDocumentBuilder();

		try {
			builder.setEntityResolver(new ConfigAuthenticationEntityResolver());
			_xmlDocument = builder.parse(_xmlFile);	// evtl. mittels BufferedInputStream cachen
		}
		catch(Exception ex) {
			final String errorMessage = "Die Benutzerdaten der Konfiguration konnten nicht eingelesen werden: " + _xmlFile.toString();
			throw new RuntimeException(errorMessage, ex);
		}
		// Daten aus der XML-Datei einlesen
		readUserAccounts();
	}

	/**
	 * Ließt alle Benutzer aus der XML-Datei ein und erzeugt entsprechende Java-Objekte. Diese werden dann in der in der Hashtable gespeichert. Die Methode ist
	 * private, weil diese Funktionalität nur an dieser Stelle zur Verfügung gestellt werden soll.
	 */
	private void readUserAccounts() {
		Element xmlRoot = _xmlDocument.getDocumentElement();

		NodeList entryList = xmlRoot.getElementsByTagName("benutzeridentifikation");
		for(int i = 0; i < entryList.getLength(); i++) {
			final Element element = (Element)entryList.item(i);

			final String userName = element.getAttribute("name");
			// Passwort, aus der XML-Datei. Das ist nicht in Klarschrift
			final String xmlPassword = element.getAttribute("passwort");

			// Hat der Benutzer Admin-Rechte
			final boolean admin = element.getAttribute("admin").toLowerCase().equals("ja");

			// Alle Einmal-Passwörter des Accounts (auch die schon benutzen)
			final List<SingleServingPassword> allSingleServingPasswords = new ArrayList<>();

			// Einmal-Passwort Liste
			final NodeList xmlSingleServingPasswordList = element.getElementsByTagName("autorisierungspasswort");

			for(int nr = 0; nr < xmlSingleServingPasswordList.getLength(); nr++) {
				// Einmal-Passwort als XML-Objekt
				final Element xmlSingleServingPassword = (Element)xmlSingleServingPasswordList.item(nr);

				// Einmal-Passwort, das aus der XML Datei eingelesen wurde, keine Klarschrift
				final String xmlSingleServingPasswort = xmlSingleServingPassword.getAttribute("passwort");
				// Index des Passworts (Integer)
				final int index = Integer.parseInt(xmlSingleServingPassword.getAttribute("passwortindex"));
				// Ist das Passwort noch gültig (ja oder nein)
				final boolean valid = xmlSingleServingPassword.getAttribute("gueltig").toLowerCase().equals("ja");

				allSingleServingPasswords.add(new SingleServingPassword(xmlSingleServingPasswort, index, valid, xmlSingleServingPassword));
			} // Alle Einmal-Passwörter

			// Alle Einmal-Passwörter wurden eingelesen

			// Alle Infos stehen zur Verfügung, das Objekt kann in die Map eingetragen werden
			final UserAccount userAccount = new UserAccount(userName, xmlPassword, admin, allSingleServingPasswords, element);

			_userAccounts.put(userAccount.getUsername(), userAccount);
		} // Alle Accounts durchgehen
	}


	@Override
	public Set<String> getUsers() {
		return _userAccounts.keySet();
	}

	@Override
	public boolean isUserAdmin(final String userName) throws ConfigurationTaskException {
		if(_userAccounts.containsKey(userName)) {
			return _userAccounts.get(userName).isAdmin();
		}
		throw new ConfigurationTaskException("Unbekannter Benutzer");
	}

	@Override
	public void setUserAdmin(final String userName, final boolean admin) throws ConfigurationTaskException {
		if(_userAccounts.containsKey(userName)) {
			// Der Benutzer existiert
			try {
				_userAccounts.get(userName).setAdminRights(admin);
			}
			catch(Exception e) {
				
				throw new ConfigurationTaskException(e);
			}
		}
		else {
			throw new ConfigurationTaskException("Unbekannter Benutzer");
		}
	}

	@Override
	public SrpCryptoParameter getCryptoParameter(final String userName, final int passwordIndex) throws ConfigurationTaskException {
		SrpVerifierAndUser verifier = getVerifier(userName, passwordIndex);
		if(verifier.isPlainTextPassword()) return null;
		return verifier.getVerifier().getSrpCryptoParameter();
	}

	@Override
	public boolean validateClientCredentials(final String userName, final ClientCredentials clientCredentials, final int passwordIndex) throws ConfigurationTaskException {
		SrpVerifierAndUser verifier = getVerifier(userName, passwordIndex);
		return SrpClientAuthentication.validateVerifier(verifier.getVerifier(), userName, clientCredentials);
	}

	@Override
	public ClientCredentials setUserPassword(final String userName, final char[] password) throws ConfigurationTaskException {
		SrpVerifierData verifier = SrpClientAuthentication.createVerifier(getCryptoParameters(), userName, ClientCredentials.ofPassword(password));
		if(_userAccounts.containsKey(userName)) {
			try {
				_userAccounts.get(userName).setPassword(verifier.toString());
			}
			catch(Exception e) {
				
				throw new ConfigurationTaskException(e);
			}
		}
		else {
			// Es ist ein Fehler aufgetreten, der Fehler wird nun genauer spezifiziert
			// Das Benutzerkonto fehlt
			throw new ConfigurationTaskException("Unbekannter Benutzer");
		}
		try {
			return SrpClientAuthentication.createLoginToken(verifier, userName, password);
		}
		catch(InconsistentLoginException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public ClientCredentials setRandomToken(final String userName) throws ConfigurationTaskException {
		ClientCredentials randomToken = SrpClientAuthentication.createRandomToken(getCryptoParameters());
		SrpVerifierData verifier = SrpClientAuthentication.createVerifier(getCryptoParameters(), userName, randomToken);
		if(_userAccounts.containsKey(userName)) {
			try {
				_userAccounts.get(userName).setPassword(verifier.toString());
			}
			catch(Exception e) {
				
				throw new ConfigurationTaskException(e);
			}
		}
		else {
			// Es ist ein Fehler aufgetreten, der Fehler wird nun genauer spezifiziert
			// Das Benutzerkonto fehlt
			throw new ConfigurationTaskException("Unbekannter Benutzer");
		}
		return randomToken;
	}

	@Override
	public ClientCredentials getLoginToken(final String userName, final char[] password, final int passwordIndex) throws ConfigurationTaskException {
		try {
			return SrpClientAuthentication.createLoginToken(getVerifier(userName, passwordIndex).getVerifier(), userName, password);
		}
		catch(InconsistentLoginException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public void createUser(final String userName, final ClientCredentials password, final boolean admin, final ConsoleInterface consoleInterface) throws ConfigurationTaskException {
		SrpVerifierData verifier = SrpClientAuthentication.createVerifier(getCryptoParameters(), userName, password);
		if(!_userAccounts.containsKey(userName)) {
			try {
				createUserXML(userName, verifier.toString(), admin);
			}
			catch(Exception e) {
				
				throw new ConfigurationTaskException(e);
			}
		}
		else {
			throw new ConfigurationTaskException("Der Benutzername ist bereits vergeben");
		}
	}

	@Override
	public void deleteUser(final String userName) throws ConfigurationTaskException {
		try {
			deleteUserXML(userName);
		}
		catch(Exception e) {
			
			throw new ConfigurationTaskException(e);
		}
	}

	@Override
	public Map<Integer, String> createOneTimePasswords(final String userName, final Collection<String> passwords) throws ConfigurationTaskException {
		// Einmal-Passwort erzeugen
		if(_userAccounts.containsKey(userName)) {
			List<SrpVerifierData> verifiers = passwords.stream()
					.map(pw -> SrpClientAuthentication.createVerifier(getCryptoParameters(), userName, ClientCredentials.ofString(pw)))
					.collect(Collectors.toList());
			int insertIndex = _userAccounts.get(userName).createNewSingleServingPasswords(verifiers);
			final Map<Integer, String> result = new TreeMap<>();
			int i = 0;
			for(String password : passwords) {
				int passwordIndex = insertIndex + i;
				result.put(passwordIndex, password);
				i++;
			}
			return result;	
		}
		else {
			// Der Benutzer, für den das Passwort angelegt werden soll, existiert nicht
			throw new ConfigurationTaskException("Unbekannter Benutzer");
		}
	}

	@Override
	public void clearOneTimePasswords(final String userName) throws ConfigurationTaskException {
		if(_userAccounts.containsKey(userName)) {
			try {
				_userAccounts.get(userName).clearSingleServingPasswords();
			}
			catch(Exception e) {
				throw new ConfigurationChangeException("Konnte Einmalpasswörter nicht löschen", e);
			}
		}
		else {
			throw new ConfigurationTaskException("Unbekannter Benutzer");
		}
	}

	@Override
	public int[] getOneTimePasswordIDs(final String userName) throws ConfigurationTaskException {
		if(_userAccounts.containsKey(userName)) {
			return _userAccounts.get(userName).getUsableSingleServingPasswords().stream().mapToInt(SingleServingPassword::getIndex).toArray();
		}
		else {
			// Es gibt zu dem Benutzernamen keine Informationen, also gibt es diesen Benutzer nicht
			
			throw new IllegalArgumentException("Benutzername/Passwort ist falsch");
		}
	}

	@Override
	public void disableOneTimePassword(final String userName, final int passwordID) throws ConfigurationTaskException {
		if(_userAccounts.containsKey(userName)) {
			_userAccounts.get(userName).disableSingleServingPassword(passwordID);
		}
		else {
			// Der Benutzer, für den das Passwort angelegt werden soll, existiert nicht
			throw new ConfigurationTaskException("Unbekannter Benutzer");
		}
	}

	@Override
	public String getDavPid() {
		return "<Lokale Datenverteiler-Pid>";
	}

	public void close() throws IOException {
		try {
			try {
				saveXMLFile();
			}
			catch(TransformerException | IOException e) {
				final String errorText = "Fehler beim Speichern der Benutzerdateien, es wird weiter versucht weitere Daten zu sichern";
				throw new IOException(errorText, e);
				
			}
		}
		finally {
			_lockAuthenticationFile.unlock();
		}
	}


	public String toString() {
		return _xmlFile.toString();
	}

	/**
	 * Erzeugt einen neuen Benutzer im speicher und speichert diesen in einer XML-Datei.
	 *
	 * @param newUserName     Benutzername
	 * @param newUserPassword Passwort
	 * @param admin           Adminrechte ja/nein
	 *
	 * @throws IOException
	 * @throws TransformerException
	 */
	private void createUserXML(String newUserName, String newUserPassword, boolean admin) throws IOException, TransformerException {
		// Für XML-Datei
		final String newUserRightsString;
		if(admin) {
			newUserRightsString = "ja";
		}
		else {
			newUserRightsString = "nein";
		}
		final Element xmlObject = createXMLUserAccount(newUserName, newUserPassword, newUserRightsString);

		final UserAccount newUser = new UserAccount(newUserName, newUserPassword, admin, new ArrayList<>(), xmlObject);

		// Das neue Objekt in die Liste der bestehenden einfügen
		_xmlDocument.getDocumentElement().appendChild(xmlObject);

		// Speichern
		saveXMLFile();
		_userAccounts.put(newUser.getUsername(), newUser);
	}


	@Override
	public SrpVerifierAndUser getVerifier(final String userName, final int passwordIndex) {
		UserAccount userAccount = _userAccounts.get(userName);
		if(userAccount == null) {
			return new SrpVerifierAndUser(UserLogin.systemUser(), fakeVerifier(userName, secretHash(userName, passwordIndex), ClientCredentials.ofString(_secretToken)), false);
		}
		try {
			return new SrpVerifierAndUser(UserLogin.systemUser(), new SrpVerifierData(userAccount.getPassword(passwordIndex)), false);
		}
		catch(IllegalArgumentException ignored){
			// Kein SRP-Format, Passwort liegt im Klartext vor.
			// Passenden SRP-Verifier erzeugen, damit der Benutzer sich authentifizieren kann.
			// Dem Datenverteiler ist es egal, ob dieser Verifier in der benutzerverwaltung.xml gespeichert war, oder hier erzeugt wurde.
			// Tatsächlich kann er es gar nicht unterscheiden.
			
			// ClientCredentials.ofString bewirkt, dass in der benutzerverwaltung.xml auch der Login-Token x drin stehen kann.
			// das ist zwar nirgendwo so spezifiziert, aber da spricht nicht wirklich was gegen
			ClientCredentials clientCredentials = ClientCredentials.ofString(userAccount.getPassword(passwordIndex));
			if(clientCredentials != null) {
				return new SrpVerifierAndUser(UserLogin.systemUser(), fakeVerifier(userName, secretHash(userName, passwordIndex), clientCredentials), true);
			}
			else {
				// Passwort ist leer ("")
				// Fake-Verifier erzeugen
				return new SrpVerifierAndUser(UserLogin.systemUser(), fakeVerifier(userName, secretHash(userName, passwordIndex), ClientCredentials.ofString(_secretToken)), false);
			}
		}
	}

	@Override
	public void setVerifier(final String userName, final SrpVerifierData verifier) throws ConfigurationTaskException {
		if(_userAccounts.containsKey(userName)) {
			try {
				_userAccounts.get(userName).setPassword(verifier.toString());
			}
			catch(Exception e) {

				throw new ConfigurationTaskException(e);
			}
		}
		else {
			// Es ist ein Fehler aufgetreten, der Fehler wird nun genauer spezifiziert
			// Das Benutzerkonto fehlt
			throw new ConfigurationTaskException("Unbekannter Benutzer");
		}	
	}

	private SrpVerifierData fakeVerifier(final String userName, final byte[] salt, final ClientCredentials clientCredentials) {
		return SrpClientAuthentication.createVerifier(getCryptoParameters(), userName, clientCredentials, salt);
	}

	private byte[] secretHash(final String userName, final int passwordIndex) {
		return SrpUtilities.generatePredictableSalt(getCryptoParameters(), (userName + _secretToken + passwordIndex).getBytes(StandardCharsets.UTF_8));
	}

	private SrpCryptoParameter getCryptoParameters() {
		return SrpCryptoParameter.getDefaultInstance();
	}

	/**
	 * Löscht einen Benutzer aus der XML-Datei
	 * @param userToDelete Benutzer, der gelöscht werden soll
	 * @throws TransformerException Fehler beim XML-Zugriff
	 * @throws IOException XMl-Datei nciht gefunden
	 */
	private void deleteUserXML(final String userToDelete) throws TransformerException, IOException {
		try{
			final NodeList childNodes = _xmlDocument.getDocumentElement().getChildNodes();
			for(int i = 0; i < childNodes.getLength(); i++) {
				final Node node = childNodes.item(i);
				if(node.hasAttributes()) {
					final NamedNodeMap attributes = node.getAttributes();
					final Node name = attributes.getNamedItem("name");
					if(name != null && name.getNodeValue().equals(userToDelete)) {
						_xmlDocument.getDocumentElement().removeChild(node);
						saveXMLFile();
						return;
					}
				}
			}

		}
		finally
		{
			_userAccounts.keySet().remove(userToDelete) ;
		}
	}

	/**
	 * Speichert alle Benutzerdaten in einer XML-Datei.
	 *
	 * @throws TransformerException
	 * @throws IOException
	 */
	private void saveXMLFile() throws TransformerException, IOException {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.ENCODING, "ISO-8859-1"); // ISO-Kodierung für westeuropäische Sprachen
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(OutputKeys.STANDALONE, "no");	  // DTD ist in einer separaten Datei

		// DOCTYPE bestimmen
		final DocumentType documentType = _xmlDocument.getDoctype();
		String publicID = null;
		String systemID = null;
		if(documentType != null) {
			publicID = _xmlDocument.getDoctype().getPublicId();
			systemID = _xmlDocument.getDoctype().getSystemId();
		}
		if(publicID != null) {
			transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, publicID);
		}
		else {
			// DOCTYPE PUBLIC_ID ist nicht vorhanden -> erstellen
			transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "-//K2S//DTD Authentifizierung//DE");
		}
		if(systemID != null) {
			transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, systemID);
		}
		else {
			// DOCTYPE SYSTEM_ID ist nicht vorhanden -> erstellen
			transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "authentication.dtd");
		}

		DOMSource source = new DOMSource(_xmlDocument);

		try(BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(_xmlFile))) {
			StreamResult result = new StreamResult(outputStream);  // gibt die XML-Struktur in einem Stream (Datei) aus
			transformer.transform(source, result);
		}
	}

	/**
	 * Erzeugt ein XML Objekt, das einem Einmal-Passwort entspricht.
	 *
	 * @param newPassword   Passwort des neuen Einmal-Passworts
	 * @param passwortindex Index des Einmal-Passworts
	 * @return XML-Objekt, das einem Einmal-Passwort entspricht
	 */
	private Element createXMLSingleServingPasswort(String newPassword, int passwortindex) {
		Element xmlSingleServingPassword = _xmlDocument.createElement("autorisierungspasswort");
		xmlSingleServingPassword.setAttribute("passwort", newPassword);
		xmlSingleServingPassword.setAttribute("passwortindex", String.valueOf(passwortindex));
		xmlSingleServingPassword.setAttribute("gueltig", "ja");
		return xmlSingleServingPassword;
	}

	/**
	 * Erzeugt ein XML Objekt, das einem Benutzerkonto entspricht. Einmal-Passwörter müssen mit der entsprechenden Methode erzeugt werden.
	 *
	 * @param name     Name des Benutzers
	 * @param password Passwort des Benutzers (in Klarschrift)
	 * @param admin    ja = Der Benutzer besitzt Admin-Rechte; nein = Der Benutzer besitzt keine Admin-Rechte
	 *
	 * @return XML Objekt, das einem Benutzerkonto entspricht
	 */
	private Element createXMLUserAccount(String name, String password, String admin) {
		Element xmlSingleServingPassword = _xmlDocument.createElement("benutzeridentifikation");
		xmlSingleServingPassword.setAttribute("name", name);
		xmlSingleServingPassword.setAttribute("passwort", password);
		xmlSingleServingPassword.setAttribute("admin", admin);
		return xmlSingleServingPassword;
	}

	/**
	 * Diese Klasse Speichert alle Informationen, die zu Benutzerkonto gehören. Dies beinhaltet:
	 * <p>
	 * Benutzername
	 * <p>
	 * Benutzerpasswort
	 * <p>
	 * Adminrechte
	 * <p>
	 * Liste von Einmal-Passwörtern (siehe TPuK1-130)
	 * <p>
	 * Sollen Änderungen an einem dieser Informationen vorgenommen werden, führt dies erst dazu, dass die Daten persistent in einer XML-Datei gespeichert werden.
	 * Ist dies erfolgreich, wird die Änderung auch an den Objekten durchgeführt. Kann die Änderungen nicht gespeichert werden, wird ein entsprechender Fehler
	 * ausgegeben und die Änderung nicht durchgeführt
	 */
	private final class UserAccount {

		/** Benutzername des Accounts */
		private final String _username;

		/** Passwort des Accounts in Klarschrift */
		private String _password;

		/** true = Der Benutzer ist ein Admin und darf Einstellungen bei anderen Benutzern vornehmen */
		private boolean _admin;

		private final LinkedList<SingleServingPassword> _usableSingleServingPasswords = new LinkedList<>();

		/**
		 * Speichert den größten Index, der bisher für ein Einmal-Passwort benutzt wurde. Das nächste Einmal-Passwort hätte als Index
		 * "_greatestSingleServingPasswordIndex++".
		 * <p>
		 * Wird mit -1 initialisiert. Das erste Passwort erhält also Index 0.
		 * <p>
		 * Der Wert wird im Konstruktor, falls Einmal-Passwörter vorhanden sind, auf den größten vergebenen Index gesetzt.
		 */
		private int _greatestSingleServingPasswordIndex = -1;

		/** XML-Objekt, dieses muss zuerst verändert und gespeichert werden, bevor die Objekte im Speicher geändert werden */
		private final Element _xmlObject;

		/**
		 * @param username                  Benutzername
		 * @param xmlPassword               Passwort, wie es in der XML-Datei gespeichert wurde
		 * @param admin                     Ob der Benutzer Admin_Rechte hat
		 * @param allSingleServingPasswords Alle Einmal-Passwörter
		 * @param xmlObject                 XML-Objekt, aus dem die obigen Daten ausgelesen wurden
		 */
		public UserAccount(String username, String xmlPassword, boolean admin, List<SingleServingPassword> allSingleServingPasswords, Element xmlObject) {
			_username = username;
			_password = xmlPassword;
			_xmlObject = xmlObject;
			_admin = admin;

			for(SingleServingPassword singleServingPassword : allSingleServingPasswords) {

				if(singleServingPassword.getIndex() > _greatestSingleServingPasswordIndex) {
					_greatestSingleServingPasswordIndex = singleServingPassword.getIndex();
				}

				if(singleServingPassword.isPasswordUsable()) {
					// Das Passwort kann noch benutzt werden
					_usableSingleServingPasswords.add(singleServingPassword);
				}
			}
		}

		/**
		 * Benutzername
		 *
		 * @return s.o.
		 */
		public String getUsername() {
			return _username;
		}

		/**
		 * Unverschlüsseltes Passwort des Benutzers
		 *
		 * @return s.o.
		 */
		public String getPassword() {
			return _password;
		}

		/**
		 * Ändert das Passwort und speichert das neue Passwort in einer XML-Datei
		 *
		 * @param password Neues Passwort
		 */
		public void setPassword(String password) throws IOException, TransformerException {
			_xmlObject.setAttribute("passwort", password);
			saveXMLFile();

			// Erst nach dem das neue Passwort gespeichert wurde, wird die Änderung im Speicher übernommen
			_password = password;
		}

		/** @return true = Der Benutzer darf die Eigenschaften anderer Benutzer ändern; false = Der Benutzer darf nur sein Passwort ändern */
		public boolean isAdmin() {
			return _admin;
		}

		/**
		 * Legt fest, ob ein Benutzer Admin-Rechte besitzt. Die Änderung wird sofort in der XML-Datei gespeichert.
		 *
		 * @param adminRights true = Der Benutzer besitzt Admin Rechte; false = Der Benutzer besitzt keine Admin-Rechte
		 */
		public void setAdminRights(boolean adminRights) throws IOException, TransformerException {
			if(adminRights) {
				_xmlObject.setAttribute("admin", "ja");
			}
			else {
				_xmlObject.setAttribute("admin", "nein");
			}

			saveXMLFile();

			_admin = adminRights;
		}

		public int createNewSingleServingPasswords(final List<SrpVerifierData> passwords) throws ConfigurationTaskException {
			int firstInsertIndex = _greatestSingleServingPasswordIndex + 1;
			for(SrpVerifierData password : passwords) {
				int passwortindex = _greatestSingleServingPasswordIndex + 1;
				final Element xmlSingleServingPassword = createXMLSingleServingPasswort(password.toString(), passwortindex);
				_xmlObject.appendChild(xmlSingleServingPassword);
				_greatestSingleServingPasswordIndex++;
				_usableSingleServingPasswords.add(new SingleServingPassword(password.toString(), passwortindex, true, xmlSingleServingPassword));
			}
			try {
				saveXMLFile();
			}
			catch(Exception e) {
				// Die Passwörter wurden nicht angelegt
				
				throw new ConfigurationTaskException(e);
			}
			return firstInsertIndex;
		}

		/**
		 * Löscht alle Einmalpasswörter eines Benutzers und markiert diese als ungültig
		 * @throws TransformerException
		 * @throws IOException
		 */
		public void clearSingleServingPasswords() throws TransformerException, IOException {
			// Alle Kindknoten (Einmalpasswörter löschen)
			while(_xmlObject.hasChildNodes()){
				_xmlObject.removeChild(_xmlObject.getFirstChild());
			}
			saveXMLFile();
			_usableSingleServingPasswords.clear();
			_greatestSingleServingPasswordIndex = -1;
		}

		/**
		 * Gibt das Passwort mit dem angegebenen Index zurück
		 * @param passwordIndex Index (falls -1 wird das normale Passwort zurückgegeben, sonst ein Einmalpasswort mit angegebenem Index)
		 * @return Passwort oder leeren String falls kein Passwort vorhanden ist. Der Aufrufer muss sicherstellen, dass man sich nicht mit einem leeren Passwort einloggen kann.
		 */
		public String getPassword(final int passwordIndex) {
			if(passwordIndex == -1) {
				return getPassword();
			}
			else {
				for(SingleServingPassword usableSingleServingPassword : _usableSingleServingPasswords) {
					if(usableSingleServingPassword.getIndex() == passwordIndex){
						return usableSingleServingPassword.getPassword();
					}
				}
			}
			
			return "";
		}

		public void disableSingleServingPassword(final int passwordIndex) throws ConfigurationTaskException {
			try {
				if(passwordIndex == -1) {
					throw new IllegalArgumentException("Das Standard-passwort kann nicht deaktiviert werden");
				}
				else {
					for(Iterator<SingleServingPassword> iterator = _usableSingleServingPasswords.iterator(); iterator.hasNext(); ) {
						final SingleServingPassword usableSingleServingPassword = iterator.next();
						if(usableSingleServingPassword.getIndex() == passwordIndex) {
							usableSingleServingPassword.setPasswortInvalid();
							iterator.remove();
							return;
						}
					}
				}
				throw new IllegalArgumentException("Angegebener Passwort-Index ist nicht am Benutzer " + _username + " vorhanden: " + passwordIndex);
			}
			catch(Exception e){
				// Die Passwörter wurden nicht angelegt
				throw new ConfigurationTaskException(e);	
			}
		}

		/**
		 * Liste, die alle benutzbaren Einmalpasswörter enthält. An Index 0 steht immer das als nächstes zu benutzende Passwort. Am Ende der Liste wird jedes neue
		 * Passwort eingefügt. Wird ein Passwort benutzt, wird das Passwort vom Anfang der Liste entfernt (FIFO).
		 */
		public LinkedList<SingleServingPassword> getUsableSingleServingPasswords() {
			return _usableSingleServingPasswords;
		}
	}

	/** Speichert alle Informationen zu einem "Einmal-Passwort" (Passwort, Index, "schon gebraucht") */
	private final class SingleServingPassword {

		/** Passwort in Klarschrift */
		private final String _password;

		/** Index des Passworts */
		private final int _index;

		/** Wurde das Passwort schon einmal benutzt */
		private boolean _passwordUsable;

		/** XML Objekt, das die Daten speichert */
		private final Element _xmlObject;

		/**
		 * @param password       Password des Einmal-Passworts, ausgelesen aus der XML-Datei
		 * @param index          Index des Passworts
		 * @param passwordUsable Kann das Passwort noch benutzt werden. true = es kann noch benutzt werden; false = es wurde bereits benutzt und kann nicht noch
		 *                       einmal benutzt werden
		 * @param xmlObject      XML-Objekt, das dem Einmal-Passwort entspricht
		 */
		public SingleServingPassword(String password, int index, boolean passwordUsable, Element xmlObject) {
			_password = password;
			_index = index;
			_passwordUsable = passwordUsable;
			_xmlObject = xmlObject;
		}

		/**
		 * Passwort des Einmal-Passworts
		 *
		 * @return s.o
		 */
		public String getPassword() {
			return _password;
		}

		/**
		 * Index des Einmal-Passworts
		 *
		 * @return s.o
		 */
		public int getIndex() {
			return _index;
		}

		/**
		 * Kann das Passwort noch benutzt werden.
		 *
		 * @return true = ja; false = nein, es wurde bereits benutzt und darf nicht noch einmal benutzt werden
		 */
		public boolean isPasswordUsable() {
			return _passwordUsable;
		}

		/** Setzt ein Einmal-Passwort auf ungültig und speichert diese Information in der XML-Datei (erst speichern, dann Objekte im Speicher ändern) */
		public void setPasswortInvalid() throws IOException, TransformerException {
			_xmlObject.setAttribute("gueltig", "nein");
			saveXMLFile();
			_passwordUsable = false;
		}

		
		public String toString() {
			return "SingleServingPassword{" +
					"_password='" + _password + '\'' +
					", _index=" + _index +
					", _passwordUsable=" + _passwordUsable +
					", _xmlObject=" + _xmlObject +
					'}';
		}
	}

	/**
	 * Implementierung eines EntityResolvers, der Referenzen auf den Public-Identifier "-//K2S//DTD Verwaltung//DE" ersetzt durch die verwaltungsdaten.dtd
	 * Resource-Datei in diesem Package.
	 */
	private static class ConfigAuthenticationEntityResolver implements EntityResolver {

		/**
		 * Löst Referenzen auf external entities wie z.B. DTD-Dateien auf.
		 * <p>
		 * Angegebene Dateien werden, falls sie im Suchverzeichnis gefunden werden, von dort geladen. Ansonsten wird der normale Mechanismus zum Laden von externen
		 * Entities benutzt.
		 *
		 * @param publicId Der public identifier der externen Entity oder null falls dieser nicht verfügbar ist.
		 * @param systemId Der system identifier aus dem XML-Dokument.
		 *
		 * @return Für Referenzen im Suchverzeichnis wird ein InputSource-Objekt, das mit der entsprechenden Datei im Suchverzeichnis verbunden ist, zurückgegeben.
		 *
		 * @throws SAXException Bei Fehlern beim Zugriff auf externe Entities.
		 * @throws IOException
		 */
		@Override
		public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
			if(publicId != null && publicId.equals("-//K2S//DTD Authentifizierung//DE")) {
				URL url = this.getClass().getResource("authentication.dtd");
				assert url != null : this.getClass();
				return new InputSource(url.toExternalForm());
			}
			return null;
		}
	}
}
