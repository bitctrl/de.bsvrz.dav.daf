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
import de.bsvrz.dav.daf.communication.srpAuthentication.SrpUtilities;
import de.bsvrz.dav.daf.communication.srpAuthentication.SrpVerifierData;
import de.bsvrz.dav.daf.main.authentication.ClientCredentials;
import de.bsvrz.dav.daf.userManagement.CommandLineAction;
import de.bsvrz.dav.daf.userManagement.ConsoleInterface;
import de.bsvrz.dav.daf.userManagement.UserManagement;

import java.util.Arrays;
import java.util.List;

/**
 * SRP-Werkzeuge, die nicht an eine Verbindung gekoppelt sind
 *
 * @author Kappich Systemberatung
 */
public class SrpUtilitiesAction extends CommandLineAction{
	@Override
	public String toString() {
		return "Fortgeschrittene Werkzeuge";
	}

	@Override
	public List<? extends CommandLineAction> getChildren() {
		return Arrays.asList(
				new CommandLineAction() {
					@Override
					public String toString() {
						return "Login-Token und Überprüfungscode aus Passwort berechnen";
					}

					@Override
					protected void execute(final ConsoleInterface console) throws Exception {
						SrpCryptoParameter srpCryptoParameter = SrpCryptoParameter.getDefaultInstance();
						String userName = console.readLine("Benutzername: ");
						char[] password = console.readPassword("Passwort: ");
						byte[] salt = SrpUtilities.generateRandomSalt(srpCryptoParameter);
						String hex = console.readString("Zufallstext: ", SrpUtilities.bytesToHex(salt));
						salt = SrpUtilities.bytesFromHex(hex);
						ClientCredentials clientCredentials = ClientCredentials.ofPassword(password);
						SrpVerifierData verifier = SrpClientAuthentication.createVerifier(srpCryptoParameter, userName, clientCredentials, salt);
						if(clientCredentials.hasPassword()) {
							clientCredentials = SrpClientAuthentication.createLoginToken(verifier, userName, password);
						}
						console.writeLine("Login-Token:");
						console.writeLine(clientCredentials.toString());
						console.writeLine("Überprüfungscode:");
						console.writeLine(verifier.toString());
						UserManagement.saveToPasswd(console, clientCredentials, userName, false);
					}                             
				},		
				new CommandLineAction() {
					@Override
					public String toString() {
						return "Überprüfungscode aus Login-Token berechnen";
					}

					@Override
					protected void execute(final ConsoleInterface console) throws Exception {
						SrpCryptoParameter srpCryptoParameter = SrpCryptoParameter.getDefaultInstance();
						String userName = console.readLine("Benutzername: ");
						String password = console.readLine("Login-Token: ");
						byte[] salt = SrpUtilities.generateRandomSalt(srpCryptoParameter);
						String hex = console.readString("Zufallstext: ", SrpUtilities.bytesToHex(salt));
						salt = SrpUtilities.bytesFromHex(hex);
						ClientCredentials clientCredentials = ClientCredentials.ofString(password);
						SrpVerifierData verifier = SrpClientAuthentication.createVerifier(srpCryptoParameter, userName, clientCredentials, salt);
						console.writeLine("Überprüfungscode:");
						console.writeLine(verifier.toString());
					}                             
				},
				new CommandLineAction() {
					@Override
					public String toString() {
						return "Zufälligen Login-Token erzeugen";
					}

					@Override
					protected void execute(final ConsoleInterface console) throws Exception {
						SrpCryptoParameter srpCryptoParameter = SrpCryptoParameter.getDefaultInstance();
						ClientCredentials clientCredentials = SrpClientAuthentication.createRandomToken(srpCryptoParameter);
						String userName = console.readLine("Benutzername: ");
						console.writeLine("Login-Token:");
						console.writeLine(clientCredentials.toString());
						UserManagement.saveToPasswd(console, clientCredentials, userName, false);
					}                             
				}
		);
	}
}
