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

package de.bsvrz.dav.daf.userManagement.actions;

import de.bsvrz.dav.daf.communication.srpAuthentication.SrpClientAuthentication;
import de.bsvrz.dav.daf.communication.srpAuthentication.SrpCryptoParameter;
import de.bsvrz.dav.daf.communication.srpAuthentication.SrpVerifierData;
import de.bsvrz.dav.daf.main.authentication.ClientCredentials;
import de.bsvrz.dav.daf.main.config.ConfigurationTaskException;
import de.bsvrz.dav.daf.main.impl.InvalidArgumentException;
import de.bsvrz.dav.daf.userManagement.CommandLineAction;
import de.bsvrz.dav.daf.userManagement.ConsoleInterface;
import de.bsvrz.dav.daf.userManagement.UserManagement;
import de.bsvrz.dav.daf.userManagement.UserManagementFileInterface;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

import static de.bsvrz.dav.daf.userManagement.actions.EditUser.createRandomPassword;

/**
 * TBD Dokumentation
 *
 * @author Kappich Systemberatung
 */
public class MigrationStatus extends CommandLineAction {
	private static final Pattern config_pattern = Pattern.compile("[ck]onfig", Pattern.CASE_INSENSITIVE);
	private static final Pattern param_pattern = Pattern.compile("param", Pattern.CASE_INSENSITIVE);
	private static final Pattern dav_pattern = Pattern.compile("da(v|tenverteiler)", Pattern.CASE_INSENSITIVE);
	private static final String[] UNSAFE_PASSWORDS = {"geheim", "administrator", "configuration", "parameter"};
	private final UserManagementFileInterface _userManagementInterface;
	private List<Problem> _problems = Collections.emptyList();

	public MigrationStatus(final UserManagementFileInterface userManagementInterface) {
		_userManagementInterface = userManagementInterface;
	}

	@Override
	protected void execute(final ConsoleInterface console) throws Exception {
		
		console.writeLine("Der Migrationsstatus wird überprüft. Das kann einige Sekunden dauern.");
		
		console.writeLine("");
		
		_problems = new ArrayList<>();
		Set<String> users = _userManagementInterface.getUsers();
		for(String user : users) {
			if(_userManagementInterface.getCryptoParameter(user, -1) == null){
				_problems.add(new SetPassword(user, "Das Passwort für den Benutzer \"" + user + "\" ist in der Konfiguration unverschlüsselt gespeichert."));
			}
			int[] passwordIDs = _userManagementInterface.getOneTimePasswordIDs(user);
			for(int passwordID : passwordIDs) {
				if(_userManagementInterface.getCryptoParameter(user, passwordID) == null){
					_problems.add(new DeleteOneTimePasswords(user, "Das Einmal-Passwort mit dem Index " + passwordID + " für den Benutzer \"" + user + "\" ist in der Konfiguration unverschlüsselt gespeichert."));
				}
			}
			for(String unsafePassword : UNSAFE_PASSWORDS) {
				try {
					if(_userManagementInterface.validateClientCredentials(user, ClientCredentials.ofString(unsafePassword), -1)) {
						_problems.add(new SetPassword(user, "Das Passwort für den Benutzer \"" + user + "\" ist ein unsicheres Standardpasswort."));
					}
				}
				catch(ConfigurationTaskException ignored) {
				}
			}
		}
		UserManagement.getPasswd().entries().forEach(
				entry -> {
					final String userName = entry.getKey();
					final ClientCredentials clientCredentials = entry.getValue();
					if(clientCredentials.hasPassword()){
						_problems.add(new SetPassword(userName, "Das Passwort für den Benutzer \"" + userName + "\" ist in der Authentifizierungsdatei unverschlüsselt gespeichert."));
					}
					if(!_userManagementInterface.getUsers().contains(userName)){
						if(config_pattern.matcher(userName).find()) {
						}
						else if(param_pattern.matcher(userName).find()) {
						}
						else if(dav_pattern.matcher(userName).find()) {
						}
						else {
							_problems.add(new RemoveFromPasswd(userName, "In der Authentifizierungsdatei befindet sich ein Passwort für den Benutzer \"" + userName + "\", aber dieser Benutzer existiert nicht in der benutzerverwaltung.xml."));
						}

						for(String unsafePassword : UNSAFE_PASSWORDS) {
							if(clientCredentials.equals(ClientCredentials.ofString(unsafePassword))) {
								_problems.add(new SetPassword(userName, "Das Passwort für den Benutzer \"" + userName + "\" ist ein unsicheres Standardpasswort."));
							}
						}
					}
					else {
						try {
							if(!_userManagementInterface.validateClientCredentials(userName, clientCredentials, -1)) {
								_problems.add(new SetPassword(userName, "Das Passwort für den Benutzer \"" + userName + "\" in der Authentifizierungsdatei ist vermutlich fehlerhaft."));
							}
						}
						catch(ConfigurationTaskException ignored) {
						}
						if(!dav_pattern.matcher(userName).find()) {
							try {
								if(_userManagementInterface.isUserAdmin(userName)) {
									_problems.add(new RemoveFromPasswd(userName, "Das Passwort für den Benutzer \"" + userName + "\" in der Authentifizierungsdatei gespeichert, aber der Benutzer ist im Sinne der Benutzerverwaltung ein Administrator. Die Authentifizierungsdatei ist zum automatischen Login von Dienstprogrammen gedacht, diese benötigen normalerweise keine Administratorrechte. Für administrative Arbeiten sollte das Passwort interaktiv eingegeben werden."));
								}
							}
							catch(ConfigurationTaskException ignored) {
							}
						}
						else {
							try {
								if(!_userManagementInterface.isUserAdmin(userName)) {
									_problems.add(new ToggleAdmin(userName, "Falls der Benutzer \"" + userName + "\" für den Start des Datenverteilers verwendet werden soll (Aufrufparameter \"-benutzer=\"), müssen diesem Benutzer Administratorrechte in der benutzerverwaltung.xml erteilt werden."));
								}
							}
							catch(ConfigurationTaskException ignored) {
							}
						}
					}
				}

		);
		Collections.sort(_problems);
		if(_problems.isEmpty()){
			console.writeLine("Alle Benutzer wurden erfolgreich auf die verschlüsselte Anmeldung umgestellt, es wurden keine Probleme gefunden.");
		}
		else {
			console.writeLine("%d Probleme gefunden.", _problems.size());
		}
	}

	@Override
	public void printStatus(final ConsoleInterface console) throws Exception {
	}

	@Override
	public List<? extends CommandLineAction> getChildren() {
		return _problems;
	}

	@Override
	public String toString() {
		return "Migrations-Assistent";
	}

	private abstract class Problem extends CommandLineAction implements Comparable<Problem> {
		private final String _description;

		private Problem(final String description) {
			_description = description;
		}

		@Override
		public String toString() {
			return _description;
		}

		@Override
		public int compareTo(final Problem o) {
			return toString().compareTo(o.toString());
		}

		@Override
		protected final void execute(final ConsoleInterface console) throws Exception {
			executeInt(console);
			MigrationStatus.this.execute(console);
		}

		protected abstract void executeInt(final ConsoleInterface console) throws Exception;
	}
	
	private final class SetPassword extends Problem{

		private final String _userName;

		public SetPassword(final String userName, final String description) {
			super(description);
			_userName = userName;
		}

		@Override
		protected void executeInt(final ConsoleInterface console) throws Exception {
			if(_userManagementInterface.getUsers().contains(_userName)) {
				if(_userManagementInterface.isUserAdmin(_userName)) {
					if(!console.readBoolean("Neues Passwort setzen? ", true)) return;
					char[] password1 = console.readPassword("Neues Passwort: ");
					char[] password2 = console.readPassword("Neues Passwort (Wiederholen): ");
					if(Arrays.equals(password1, password2)) {
						savePassword(console, _userManagementInterface.setUserPassword(_userName, password1), _userName, false);
						return;
					}
					else {
						throw new InvalidArgumentException("Passwörter stimmen nicht überein");
					}
				}
				boolean auto = console.readBoolean("Wird der Benutzer nur für die automatische Anmeldung von Applikationen verwendet? (Es wird ein zufälliger Login-Token erzeugt) ", false);
				if(auto) {
					savePassword(console, _userManagementInterface.setRandomToken(_userName), _userName, true);
					return;
				}
				boolean incomingDav = console.readBoolean("Wird der Benutzer nur für die (eingehende) Authentifizierung von anderen Datenverteilern verwendet? ", false);
				if(incomingDav) {
					if(!console.readBoolean("Neues Passwort setzen? Hierfür muss auf dem System, von dem sich aus \"" + _userName + "\" anmeldet die Authentifizierungsdatei (passwd) angepasst werden.", true)) {
						console.writeLine("Alternativ kann auf dem anderen System das Passwort gesetzt werden.");
						return;
					}
					char[] password1 = console.readPassword("Neues Passwort: ");
					char[] password2 = console.readPassword("Neues Passwort (Wiederholen): ");
					if(Arrays.equals(password1, password2)) {
						ClientCredentials token = _userManagementInterface.setUserPassword(_userName, password1);
						console.writeLine("Login-Token für die Authentifizierungsdatei auf dem fremden System: ");
						console.writeLine("%s@%s:%s", _userName, _userManagementInterface.getDavPid(), token);
						return;
					}
					else {
						throw new InvalidArgumentException("Passwörter stimmen nicht überein");
					}
				}	
				boolean outgoingDav = console.readBoolean("Wird der Benutzer nur für die (ausgehende) Authentifizierung bei anderen Datenverteilern verwendet? ", false);
				if(outgoingDav) {
					if(!console.readBoolean("Neues Passwort setzen? Hierfür muss auf dem Zielsystem der hier generierte Überprüfungscode in der benutzerverwaltung.xml gesetzt werden.", true)) {
						console.writeLine("Alternativ kann auf dem anderen System das Passwort gesetzt werden.");
						return;
					}
					char[] password1 = console.readPassword("Neues Passwort: ");
					char[] password2 = console.readPassword("Neues Passwort (Wiederholen): ");
					if(Arrays.equals(password1, password2)) {
						savePassword(console, _userManagementInterface.setUserPassword(_userName, password1), _userName, true);
						console.writeLine("Überprüfungscode zum speichern auf den anderen Datenverteilern:");
						console.writeLine("%s", _userManagementInterface.getVerifier(_userName, -1));
						return;
					}
					else {
						throw new InvalidArgumentException("Passwörter stimmen nicht überein");
					}
				}
				if(!console.readBoolean("Neues Passwort setzen? ", true)) return;
				char[] password1 = console.readPassword("Neues Passwort: ");
				char[] password2 = console.readPassword("Neues Passwort (Wiederholen): ");
				if(Arrays.equals(password1, password2)) {
					savePassword(console, _userManagementInterface.setUserPassword(_userName, password1), _userName, false);
					return;
				}
				else {
					throw new InvalidArgumentException("Passwörter stimmen nicht überein");
				}
			}
			else {
				if(config_pattern.matcher(_userName).find()){
					savePassword(console, SrpClientAuthentication.createRandomToken(SrpCryptoParameter.getDefaultInstance()), _userName, true);
					return;
				}
				if(param_pattern.matcher(_userName).find()){
					savePassword(console, SrpClientAuthentication.createRandomToken(SrpCryptoParameter.getDefaultInstance()), _userName, true);
					return;
				}
				if(!console.readBoolean("Neues Passwort setzen? ", true)) return;
				char[] password1 = console.readPassword("Neues Passwort: ");
				char[] password2 = console.readPassword("Neues Passwort (Wiederholen): ");
				if(Arrays.equals(password1, password2)) {
					ClientCredentials clientCredentials = ClientCredentials.ofPassword(password1);
					SrpVerifierData verifier = SrpClientAuthentication.createVerifier(SrpCryptoParameter.getDefaultInstance(), _userName, clientCredentials);
					clientCredentials = SrpClientAuthentication.createLoginToken(verifier, _userName, password1);
					console.writeLine("Login-Token:");
					console.writeLine(clientCredentials.toString());
					console.writeLine("Überprüfungscode:");
					console.writeLine(verifier.toString());
					UserManagement.saveToPasswd(console, clientCredentials, _userName, true);
				}
				else {
					throw new InvalidArgumentException("Passwörter stimmen nicht überein");
				}
			}
		}
	}
	
	private final class DeleteOneTimePasswords extends Problem{

		private final String _userName;

		public DeleteOneTimePasswords(final String userName, final String description) {
			super(description);
			_userName = userName;
		}

		@Override
		protected void executeInt(final ConsoleInterface console) throws Exception {
			if(!console.readBoolean("Unverschlüsselte Einmalpasswörter löschen? ", true)) return;
			int[] passwordIDs = _userManagementInterface.getOneTimePasswordIDs(_userName);
			for(int passwordID : passwordIDs) {
				SrpCryptoParameter parameter = _userManagementInterface.getCryptoParameter(_userName, passwordID);
				if(parameter == null) {
					_userManagementInterface.disableOneTimePassword(_userName, passwordID);
				}
			}
			int numPasswords = console.readInt("Anzahl neue Einmalpasswörter: ", 10);
			// Set benutzen um doppelte Passwörter zu verhindern
			final Set<String> passwords = new LinkedHashSet<>(numPasswords);
			while(passwords.size() < numPasswords){
				passwords.add(createRandomPassword());
			}
			printPasswords(console, passwords);
		}

		private void printPasswords(final ConsoleInterface console, final Collection<String> passwords) throws ConfigurationTaskException {
			console.writeLine("Neue Einmalpasswörter:");
			Map<Integer, String> oneTimePasswords = _userManagementInterface.createOneTimePasswords(_userName, passwords);
			for(Map.Entry<Integer, String> entry : oneTimePasswords.entrySet()) {
				console.writeLine("%s-%d=%s", _userName, entry.getKey(), entry.getValue());
			}
		}
	}
	
	private final class ToggleAdmin extends Problem{

		private final String _userName;

		public ToggleAdmin(final String userName, final String description) {
			super(description);
			_userName = userName;
		}

		@Override
		protected void executeInt(final ConsoleInterface console) throws Exception {
			boolean userAdmin = _userManagementInterface.isUserAdmin(_userName);
			_userManagementInterface.setUserAdmin(_userName, !userAdmin);
			if(userAdmin) {
				console.writeLine("Dem Benutzer wurden die Administrator-Rechte entzogen");
			}
			else {
				console.writeLine("Der Benutzer besitzt jetzt Administrator-Rechte");
			}
		}
	}	
	
	private final class RemoveFromPasswd extends Problem{

		private final String _userName;

		public RemoveFromPasswd(final String userName, final String description) {
			super(description);
			_userName = userName;
		}

		@Override
		protected void executeInt(final ConsoleInterface console) throws Exception {
			if(!console.readBoolean("Benutzer \"" + _userName + "\" aus Authentifizierungsdatei entfernen? ", true)) return;
			UserManagement.getPasswd().deleteClientCredentials(_userName);
			console.writeLine("Eintrag für Benutzer " + _userName + " wurde aus der Authentifizierungsdatei gelöscht.");
		}
	}
	
	private void savePassword(final ConsoleInterface console, final ClientCredentials loginToken, final String user, final boolean alwaysSave) throws IOException {
		UserManagement.saveToPasswd(console, loginToken, user, alwaysSave);
	}
}
