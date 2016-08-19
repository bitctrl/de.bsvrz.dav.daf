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

import de.bsvrz.dav.daf.communication.srpAuthentication.SrpCryptoParameter;
import de.bsvrz.dav.daf.communication.srpAuthentication.SrpVerifierAndUser;
import de.bsvrz.dav.daf.communication.srpAuthentication.SrpVerifierData;
import de.bsvrz.dav.daf.main.authentication.ClientCredentials;
import de.bsvrz.dav.daf.main.config.ConfigurationTaskException;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * TBD Dokumentation
 *
 * @author Kappich Systemberatung
 */
public interface UserManagementFileInterface {
	
	Set<String> getUsers();
	
	boolean isUserAdmin(String userName) throws ConfigurationTaskException;
	
	void setUserAdmin(String userName, boolean admin) throws ConfigurationTaskException;
	
	SrpCryptoParameter getCryptoParameter(String userName, final int passwordIndex) throws ConfigurationTaskException;
	
	boolean validateClientCredentials(String userName, final ClientCredentials clientCredentials, final int passwordIndex) throws ConfigurationTaskException;
	
	ClientCredentials setUserPassword(String userName, char[] password) throws ConfigurationTaskException;

	ClientCredentials setRandomToken(String userName) throws ConfigurationTaskException;

	ClientCredentials getLoginToken(String userName, char[] password, final int passwordIndex) throws ConfigurationTaskException;
	
	void createUser(String userName, ClientCredentials password, boolean admin, final ConsoleInterface consoleInterface) throws ConfigurationTaskException;
	
	void deleteUser(String userName) throws ConfigurationTaskException;
	
	Map<Integer, String> createOneTimePasswords(String userName, Collection<String> passwords) throws ConfigurationTaskException;
	
	void clearOneTimePasswords(final String userName) throws ConfigurationTaskException;
	
	int[] getOneTimePasswordIDs(final String userName) throws ConfigurationTaskException;

	void disableOneTimePassword(final String userName, int passwordID) throws ConfigurationTaskException;

	String getDavPid();

	SrpVerifierAndUser getVerifier(String userName, int passwordIndex) throws ConfigurationTaskException;

	void setVerifier(String userName, SrpVerifierData srpVerifierData) throws ConfigurationTaskException;
}
