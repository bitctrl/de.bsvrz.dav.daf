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

import de.bsvrz.dav.daf.main.ClientDavConnection;
import de.bsvrz.dav.daf.main.ClientDavParameters;
import de.bsvrz.dav.daf.main.authentication.ClientCredentials;
import de.bsvrz.dav.daf.userManagement.actions.*;
import de.bsvrz.sys.funclib.commandLineArgs.ArgumentList;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * TBD Dokumentation
 *
 * @author Kappich Systemberatung
 */
public class UserManagement extends CommandLineAction {

	private static EditableAuthenticationFile _passwd = null;

	public static void main(String[] args) throws Exception {
		ArgumentList argumentList = new ArgumentList(args.clone());
		CommandLineAction mainAction;
		if(argumentList.hasArgument("-authentifizierung")) {
			ArgumentList.Argument argument = argumentList.fetchArgument("-authentifizierung");
			if(argument.asString().equals("interaktiv") || argument.asString().equals("STDIN")){
				_passwd = new EditableAuthenticationFile(Paths.get("passwd"));
			}
			else {
				_passwd = new EditableAuthenticationFile(argument.asWritableFile(true).toPath());
			}
		}
		else {
			_passwd = new EditableAuthenticationFile(Paths.get("passwd"));
		}
		if(argumentList.fetchArgument("-online=nein").booleanValue()){
			ClientDavParameters parameters = new ClientDavParameters(new ArgumentList(args.clone()));
			ClientDavConnection connection = new ClientDavConnection(parameters);
			connection.connect();
			String userName = parameters.getUserName();
			char[] password = parameters.getClientCredentials().getPassword();
			connection.login(userName, password);
			mainAction = new ChooseConfiguration(connection, userName, password);
		}
		else if(argumentList.hasArgument("-offline")){
			mainAction = new CommandLineAction() {

				private UserManagementFileOffline _userManagementInterface;

				@Override
				protected void execute(final ConsoleInterface console) throws Exception {
					_userManagementInterface = new UserManagementFileOffline(argumentList.fetchArgument("-offline=").asFile());
				}

				@Override
				protected void dispose(final ConsoleInterface console) throws Exception {
					_userManagementInterface.close();
				}

				@Override
				public String toString() {
					throw new UnsupportedOperationException("Nicht implementiert");
				}

				@Override
				public List<? extends CommandLineAction> getChildren() {
					return UserManagement.getActions(_userManagementInterface);
				}
			};
		}
		else {
			mainAction = new UserManagement();
		}
		mainAction.execute(Console.getInstance(), null);
	}

	public static List<? extends CommandLineAction> getActions(final ClientDavConnection connection, final String userName, final char[] password) {
		return Arrays.asList(
				new ChooseConfiguration(connection, userName, password)
		);
	}

	public static EditableAuthenticationFile getPasswd() {
		return _passwd;
	}

	public static void saveToPasswd(ConsoleInterface console, final ClientCredentials loginToken, final String user, final boolean alwaysSave) throws IOException {
		final EditableAuthenticationFile passwdApp = getPasswd();
		final ClientCredentials oldEntry = passwdApp.getClientCredentials(user);
		if(Objects.equals(oldEntry, loginToken)){
			console.writeLine("Dieser Login-Token ist bereits in der Authentifizierungsdatei gespeichert.");
			return;
		}
		String prompt = "Login-Token für automatische Anmeldung in Authentifizierungsdatei speichern? ";
		if(oldEntry != null) {
			if(oldEntry.hasPassword()){
				prompt = "Klartextpasswort in Authentifizierungsdatei durch Login-Token ersetzen? ";
			}
			else {
				prompt = "Login-Token für automatische Anmeldung in Authentifizierungsdatei ersetzen? ";
			}
		}
		final boolean b = alwaysSave || console.readBoolean(prompt, false);
		if(b) {
			passwdApp.setClientCredentials(user, loginToken);
			if(oldEntry == null) {
				console.writeLine("Authentifizierungsdatei um Login-Token für " + user + " ergänzt");
			}
			else {
				console.writeLine("Login-Token für " + user + " in Authentifizierungsdatei geändert");
			}
		}
		else if(oldEntry != null) {
			if(oldEntry.hasPassword()) {
				prompt = "Bisheriges Klartextpasswort aus Authentifizierungsdatei löschen? ";
			}
			else {
				prompt = "Bisherigen Login-Token aus Authentifizierungsdatei löschen? ";
			}
			final boolean delete = console.readBoolean(prompt, true);
			if(delete) {
				passwdApp.deleteClientCredentials(user);
				if(oldEntry.hasPassword()) {
					console.writeLine("Altes Klartextpasswort für Benutzer " + user + " wurde aus der Authentifizierungsdatei gelöscht.");
				}
				else {
					console.writeLine("Alter Login-Token für Benutzer " + user + " wurde aus der Authentifizierungsdatei gelöscht.");
				}
			}
		}
	}

	@Override
	public String toString() {
		return "Hauptmenü";
	}

	@Override
	public List<? extends CommandLineAction> getChildren() {
		return Arrays.asList(
				new UserManagementWithDav(),
				new UserManagementWithFile(),
				new SrpUtilitiesAction()
		);
	}

	public static List<? extends CommandLineAction> getActions(final UserManagementFileInterface userManagementInterface) {
		return Arrays.asList(
				new MigrationStatus(userManagementInterface),
				new UserList(userManagementInterface),
				new NewUser(userManagementInterface),
		        new SrpUtilitiesAction()
		);
	}

}
