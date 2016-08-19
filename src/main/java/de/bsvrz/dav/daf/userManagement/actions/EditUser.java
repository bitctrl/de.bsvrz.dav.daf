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
import java.security.SecureRandom;
import java.util.*;

/**
 * TBD Dokumentation
 *
 * @author Kappich Systemberatung
 */
public class EditUser extends CommandLineAction {
	private final String _userName;
	private final UserManagementFileInterface _userManagementInterface;

	public EditUser(final String userName, final UserManagementFileInterface userManagementInterface) {
		_userName = userName;
		_userManagementInterface = userManagementInterface;
	}

	@Override
	public String toString() {
		return _userName;
	}

	@Override
	public void printStatus(final ConsoleInterface console) throws Exception {
		try {
			console.writeLine("Benutzername: %s", _userName);
			console.writeLine("Administrator: %s", _userManagementInterface.isUserAdmin(_userName) ? "Ja" : "Nein");
			console.writeLine("Passwortsicherheit: %s", printParameter(_userManagementInterface.getCryptoParameter(_userName, -1)));
		}
		catch(Exception e){}
	}

	@Override
	public List<? extends CommandLineAction> getChildren() {
		return Arrays.asList(
				new CommandLineAction() {
					@Override
					protected void execute(final ConsoleInterface console) throws Exception {
						char[] password1 = console.readPassword("Neues Passwort: ");
						char[] password2 = console.readPassword("Neues Passwort (Wiederholen): ");
						if(Arrays.equals(password1, password2)) {
							savePassword(console, _userManagementInterface.setUserPassword(_userName, password1), false);
						}
						else {
							throw new InvalidArgumentException("Passwörter stimmen nicht überein");
						}
					}

					@Override
					public String toString() {
						if(System.getProperty("srp6.disable.verifier") != null) {
							return "Neues Klartextpasswort setzen";
						}
						return "Neues Passwort setzen";
					}
				},
				new CommandLineAction() {
					@Override
					protected void execute(final ConsoleInterface console) throws Exception {
						if(System.getProperty("srp6.disable.verifier") != null) {
							System.getProperties().remove("srp6.disable.verifier");
						}
						else {
							System.setProperty("srp6.disable.verifier", "");
						}
					}

					@Override
					public String toString() {
						if(System.getProperty("srp6.disable.verifier") != null) {
							return "Ab jetzt verschlüsselte Passwörter setzen";
						}
						return "Ab jetzt Klartextpasswörter setzen";
					}
				},
				new CommandLineAction() {
					@Override
					protected void execute(final ConsoleInterface console) throws Exception {
						savePassword(console, _userManagementInterface.setRandomToken(_userName), true);
					}

					@Override
					public String toString() {
						return "Zufallspasswort für automatischen Login setzen";
					}
				},		
				new CommandLineAction() {
					@Override
					protected void execute(final ConsoleInterface console) throws Exception {
						String s = console.readLine("Überprüfungscode: ");
						SrpVerifierData srpVerifierData = new SrpVerifierData(s);
						_userManagementInterface.setVerifier(_userName, srpVerifierData);
					}

					@Override
					public String toString() {
						return "Überprüfungscode setzen";
					}
				},
				new CommandLineAction() {
					@Override
					protected void execute(final ConsoleInterface console) throws Exception {
						char[] password = console.readPassword("Passwort überprüfen: ");
						savePassword(console, _userManagementInterface.getLoginToken(_userName, password, -1), true);
					}

					@Override
					public String toString() {
						return "Login-Token für Authentifizierungsdatei erzeugen";
					}
				},
				new CommandLineAction() {

					@Override
					protected void execute(final ConsoleInterface console) throws Exception {
						boolean userAdmin = _userManagementInterface.isUserAdmin(_userName);
						_userManagementInterface.setUserAdmin(_userName, !userAdmin);
						if(userAdmin) {
							console.writeLine("Dem Benutzer wurden die Administrator-Rechte entzogen");
						}
						else {
							console.writeLine("Der Benutzer besitzt jetzt Administrator-Rechte");
						}
					}

					@Override
					public String toString() {
						return "Administrator-Rechte setzen";
					}
				},
				new CommandLineAction() {

					@Override
					protected void execute(final ConsoleInterface console) throws Exception {
						char[] password = console.readPassword("Passwort überprüfen: ");
						boolean b = _userManagementInterface.validateClientCredentials(_userName, ClientCredentials.ofPassword(password), -1);
						if(b)
							console.writeLine("Passwort korrekt");
						else
							console.writeLine("Passwort falsch");
					}

					@Override
					public String toString() {
						return "Passwort auf Korrektheit überprüfen";
					}
				},
				new CommandLineAction() {

					@Override
					protected void execute(final ConsoleInterface console) throws Exception {
						String password = console.readLine("Login-Token überprüfen: ");
						boolean b = _userManagementInterface.validateClientCredentials(_userName, ClientCredentials.ofString(password), -1);
						if(b)
							console.writeLine("Token korrekt");
						else
							console.writeLine("Token falsch");
					}

					@Override
					public String toString() {
						return "Login-Token auf Korrektheit überprüfen";
					}
				},
				new ManageOneTimePasswords(),
				new CommandLineAction() {
					@Override
					public String toString() {
						return "Benutzer löschen";
					}

					@Override
					protected void execute(final ConsoleInterface console) throws Exception {
						_userManagementInterface.deleteUser(_userName);
						skipParent();
						console.writeLine("Benutzer \"" + _userName + "\" erfolgreich gelöscht.");
					}
				}
		);
	}

	private Object printParameter(final SrpCryptoParameter cryptoParameter) {
		if (cryptoParameter == null) return "Unverschlüsselt";
		return "Verschlüsselt mit SRP6 " + cryptoParameter;
	}

	private void savePassword(ConsoleInterface console, final ClientCredentials loginToken, final boolean alwaysSave) throws IOException {
		savePassword(console, -1, loginToken, alwaysSave);
	}


	private void savePassword(ConsoleInterface console, int index, final ClientCredentials loginToken, final boolean alwaysSave) throws IOException {
		final String user = _userName + (index == -1 ? "" : "-" + index);
		UserManagement.saveToPasswd(console, loginToken, user, alwaysSave);
	}

	private class ManageOneTimePasswords extends CommandLineAction {

		@Override
		public void printStatus(final ConsoleInterface console) throws Exception {
			console.writeLine("Einmalpasswörter:");
			int[] passwordIDs = _userManagementInterface.getOneTimePasswordIDs(_userName);
			if(passwordIDs.length == 0) {
				console.writeLine("Keine Einmalpasswörter vorhanden");
			}
			else {
				console.writeLine("Anzahl gültige Einmalpasswörter: " + passwordIDs.length);

			}
		}

		@Override
		public List<? extends CommandLineAction> getChildren() {
			return Arrays.asList(
					new CommandLineAction() {

						@Override
						protected void execute(final ConsoleInterface console) throws Exception {
							_userManagementInterface.clearOneTimePasswords(_userName);
						}

						@Override
						public String toString() {
							return "Alle Einmalpasswörter löschen";
						}
					},
					new CommandLineAction() {


						@Override
						protected void execute(final ConsoleInterface console) throws Exception {
							int[] passwordIDs = _userManagementInterface.getOneTimePasswordIDs(_userName);
							for(int passwordID : passwordIDs) {
								SrpCryptoParameter parameter = _userManagementInterface.getCryptoParameter(_userName, passwordID);
								if(parameter == null) {
									_userManagementInterface.disableOneTimePassword(_userName, passwordID);
								}
							}
						}

						@Override
						public String toString() {
							return "Unverschlüsselte Einmalpasswörter löschen";
						}
					},
					new CommandLineAction() {

						@Override
						protected void execute(final ConsoleInterface console) throws Exception {
							console.writeLine("Leere Zeile um aufzuhören");
							final List<String> passwords = new ArrayList<>();
							while(true) {
								String pw = console.readLine("Neues Einmalpasswort: ");
								if(pw.isEmpty()) break;
								passwords.add(pw);
							}
							printPasswords(console, passwords);
						}

						@Override
						public String toString() {
							return "Einmalpasswörter hinzufügen";
						}
					},
					new CommandLineAction() {
						@Override
						protected void execute(final ConsoleInterface console) throws Exception {
							int numPasswords = console.readInt("Anzahl: ", 10);
							// Set benutzen um doppelte Passwörter zu verhindern
							final Set<String> passwords = new LinkedHashSet<>(numPasswords);
							while(passwords.size() < numPasswords){
								passwords.add(createRandomPassword());
							}
							printPasswords(console, passwords);
						}

						@Override
						public String toString() {
							return "Zufällige Einmalpasswörter hinzufügen";
						}
					},
					new CommandLineAction() {
						@Override
						protected void execute(final ConsoleInterface console) throws Exception {
							console.writeLine("Einmalpasswörter:");
							int[] passwordIDs = _userManagementInterface.getOneTimePasswordIDs(_userName);
							for(int passwordID : passwordIDs) {
								console.writeLine("%d: %s", passwordID, printParameter(_userManagementInterface.getCryptoParameter(_userName, passwordID)));
							}
						}

						@Override
						public String toString() {
							return "Gültige Einmalpasswörter auflisten";
						}
					}
			);
		}

		private void printPasswords(final ConsoleInterface console, final Collection<String> passwords) throws ConfigurationTaskException {
			console.writeLine("Neue Einmalpasswörter:");
			Map<Integer, String> oneTimePasswords = _userManagementInterface.createOneTimePasswords(_userName, passwords);
			for(Map.Entry<Integer, String> entry : oneTimePasswords.entrySet()) {
				console.writeLine("%s-%d=%s", _userName, entry.getKey(), entry.getValue());
			}
		}

		@Override
		public String toString() {
			return "Einmalpasswörter bearbeiten";
		}
	}

	public static String createRandomPassword() {
		int length = 19;
		char[] result = new char[length];
		SecureRandom secureRandom = new SecureRandom();
		for(int i = 0; i < length; i++){
			if(i % 5 == 4){
				result[i] = '-';
			}
			else {
				int index = secureRandom.nextInt(CharacterHolder.CHARS.length);
				result[i] = CharacterHolder.CHARS[index];
			}
		}
		return String.valueOf(result);
	}

	private static class CharacterHolder {
		public static final char[] CHARS;
		
		static {
			// 1 , l und 0 sind leicht verwechselbar und daher hier nicht enthalten
			CHARS = "abcdefghijkmnopqrstuvwxyz23456789".toCharArray();
		}
	}
}
