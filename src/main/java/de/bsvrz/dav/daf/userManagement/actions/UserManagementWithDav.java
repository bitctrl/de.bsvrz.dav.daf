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

import de.bsvrz.dav.daf.main.*;
import de.bsvrz.dav.daf.main.authentication.ClientCredentials;
import de.bsvrz.dav.daf.userManagement.CommandLineAction;
import de.bsvrz.dav.daf.userManagement.ConsoleInterface;
import de.bsvrz.dav.daf.userManagement.UserManagement;

import java.util.List;

/**
 * Aufbau einer Datenverteilerverbindung im Migrationstool
 *
 * @author Kappich Systemberatung
 */
public class UserManagementWithDav extends CommandLineAction {

	private ClientDavConnection _connection;
	private String _userName;
	private char[] _password;

	@Override
	public String toString() {
		return "Benutzerverwaltung über den Datenverteiler";
	}

	@Override
	protected void execute(final ConsoleInterface console) throws CommunicationError, ConnectionException, InconsistentLoginException {
		String ip = console.readString("Datenverteiler-Adresse: ", "localhost");
		int port = console.readInt("Datenverteiler-Port: ", 8083);
		boolean loggedIn = false;
		try {
			ClientDavParameters clientDavParameters = new ClientDavParameters();
			clientDavParameters.setDavCommunicationAddress(ip);
			clientDavParameters.setDavCommunicationSubAddress(port);
			_connection = new ClientDavConnection(clientDavParameters);
			_connection.connect();
			_userName = console.readLine("Benutzername zur Anmeldung: ");
			_password = console.readPassword("Passwort: ");
			_connection.login(_userName, ClientCredentials.ofPassword(_password));
			console.writeLine("Verbindung aufgebaut, Verschlüsselungsstatus der Verbindung: %s", _connection.getEncryptionStatus());
			loggedIn = true;
		}
		catch(MissingParameterException e) {
			throw new AssertionError(e);
		}
		finally {
			if(!loggedIn){
				_connection.disconnect(true, "Authentifizierung fehlgeschlagen");
			}
		}
	}

	@Override
	protected void dispose(final ConsoleInterface console) {
		_connection.disconnect(false, "");
	}

	@Override
	public List<? extends CommandLineAction> getChildren() {
		return UserManagement.getActions(_connection, _userName, _password);
	}
}
