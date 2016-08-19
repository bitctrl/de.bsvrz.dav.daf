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
import de.bsvrz.dav.daf.main.authentication.ClientCredentials;
import de.bsvrz.dav.daf.main.impl.InvalidArgumentException;
import de.bsvrz.dav.daf.userManagement.CommandLineAction;
import de.bsvrz.dav.daf.userManagement.ConsoleInterface;
import de.bsvrz.dav.daf.userManagement.UserManagement;
import de.bsvrz.dav.daf.userManagement.UserManagementFileInterface;

import java.util.Arrays;

/**
 * TBD Dokumentation
 *
 * @author Kappich Systemberatung
 */
public class NewUser extends CommandLineAction {
	private final UserManagementFileInterface _userManagementInterface;

	public NewUser(final UserManagementFileInterface userManagementInterface) {
		_userManagementInterface = userManagementInterface;
	}

	@Override
	protected void execute(final ConsoleInterface console) throws Exception {
		String userName = console.readLine("Benutzername: ");
		boolean admin = console.readBoolean("Administratorrechte: ", false);
		boolean random = console.readBoolean("Zufälliges Passwort für automatischen Login verwenden: ", false);
		ClientCredentials clientCredentials;
		if(random) {
			clientCredentials = SrpClientAuthentication.createRandomToken(SrpCryptoParameter.getDefaultInstance());
		}
		else {
			char[] password1 = console.readPassword("Neues Passwort: ");
			char[] password2 = console.readPassword("Neues Passwort (Wiederholen): ");
			if(Arrays.equals(password1, password2)) {
				clientCredentials = ClientCredentials.ofPassword(password1);
			}
			else {
				throw new InvalidArgumentException("Passwörter stimmen nicht überein");
			}
		}
		_userManagementInterface.createUser(
				userName,
				clientCredentials,
				admin,
				console
		);
		if(random) {
			UserManagement.saveToPasswd(console, clientCredentials, userName, true);
		}
	}


	@Override
	public String toString() {
		return "Benutzer erstellen";
	}
}
