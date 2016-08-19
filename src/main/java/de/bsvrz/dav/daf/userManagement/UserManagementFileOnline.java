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

import de.bsvrz.dav.daf.communication.srpAuthentication.SrpClientAuthentication;
import de.bsvrz.dav.daf.communication.srpAuthentication.SrpCryptoParameter;
import de.bsvrz.dav.daf.communication.srpAuthentication.SrpVerifierAndUser;
import de.bsvrz.dav.daf.communication.srpAuthentication.SrpVerifierData;
import de.bsvrz.dav.daf.main.ClientDavConnection;
import de.bsvrz.dav.daf.main.InconsistentLoginException;
import de.bsvrz.dav.daf.main.authentication.ClientCredentials;
import de.bsvrz.dav.daf.main.config.ConfigurationTaskException;
import de.bsvrz.dav.daf.main.config.DataModel;
import de.bsvrz.dav.daf.main.config.DynamicObjectType;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dav.daf.main.config.management.UserAdministration;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Benutzerverwaltung Online
 *
 * @author Kappich Systemberatung
 */
public class UserManagementFileOnline implements UserManagementFileInterface {
	private final ClientDavConnection _connection;
	private final DataModel _dataModel;
	private final UserAdministration _userAdministration;
	private final String _orderer;
	private final String _ordererPassword;
	private final boolean _userAdmin;

	public UserManagementFileOnline(final ClientDavConnection connection, final DataModel dataModel, final UserAdministration userAdministration, final String orderer, final char[] ordererPassword, final boolean userAdmin) throws ConfigurationTaskException {
		_connection = connection;
		_dataModel = dataModel;
		_userAdministration = userAdministration;
		_orderer = orderer;
		_ordererPassword = new String(ordererPassword);
		_userAdmin = userAdmin;
	}

	@Override
	public Set<String> getUsers() {
		return _userAdministration.subscribeUserChangeListener(null)
				.stream()
				.filter(it -> {
					try {
						return _userAdministration.isUserValid(_orderer, _ordererPassword, it.getName());
					}
					catch(ConfigurationTaskException ignored) {
						return false;
					}
				})
				.map(SystemObject::getName)
				.collect(Collectors.toCollection(TreeSet::new));
	}

	@Override
	public boolean isUserAdmin(final String userName) throws ConfigurationTaskException {
		return _userAdministration.isUserAdmin(_orderer, _ordererPassword, userName);
	}

	@Override
	public void setUserAdmin(final String userName, final boolean admin) throws ConfigurationTaskException {
		_userAdministration.changeUserRights(_orderer, _ordererPassword, userName, admin);
	}

	@Override
	public SrpCryptoParameter getCryptoParameter(final String userName, final int passwordIndex) throws ConfigurationTaskException {
		SrpVerifierAndUser verifier = _userAdministration.getSrpVerifier(_orderer, _ordererPassword, userName, passwordIndex);
		if(verifier.isPlainTextPassword()) return null;
		return verifier.getVerifier().getSrpCryptoParameter();
	}

	@Override
	public boolean validateClientCredentials(final String userName, final ClientCredentials clientCredentials, final int passwordIndex) throws ConfigurationTaskException {
		SrpVerifierAndUser srpVerifier = _userAdministration.getSrpVerifier(_orderer, _ordererPassword, userName, passwordIndex);
		return SrpClientAuthentication.validateVerifier(srpVerifier.getVerifier(), userName, clientCredentials);
	}

	@Override
	public ClientCredentials setUserPassword(final String userName, final char[] password) throws ConfigurationTaskException {
		_userAdministration.changeUserPassword(_orderer, _ordererPassword, userName, new String(password));
		return getLoginToken(userName, password, -1);
	}

	@Override
	public ClientCredentials setRandomToken(final String userName) throws ConfigurationTaskException {
		ClientCredentials randomToken = SrpClientAuthentication.createRandomToken(SrpCryptoParameter.getDefaultInstance());
		_userAdministration.changeUserPassword(_orderer, _ordererPassword, userName, randomToken.toString());
		return randomToken;
	}

	@Override
	public ClientCredentials getLoginToken(final String userName, final char[] password, final int passwordIndex) throws ConfigurationTaskException {
		SrpVerifierAndUser srpVerifier = _userAdministration.getSrpVerifier(_orderer, _ordererPassword, userName, passwordIndex);
		try {
			return SrpClientAuthentication.createLoginToken(srpVerifier.getVerifier(), userName, password);
		}
		catch(InconsistentLoginException e) {
			throw new ConfigurationTaskException(e);
		}
	}

	@Override
	public SrpVerifierAndUser getVerifier(final String userName, final int passwordIndex) throws ConfigurationTaskException {
		return _userAdministration.getSrpVerifier(_orderer, _ordererPassword, userName, passwordIndex);
	}

	@Override
	public void setVerifier(final String userName, final SrpVerifierData srpVerifierData) throws ConfigurationTaskException {
		_userAdministration.setSrpVerifier(_orderer, _ordererPassword, userName, srpVerifierData);
	}

	@Override
	public void createUser(final String userName, final ClientCredentials password, final boolean admin, final ConsoleInterface console) throws ConfigurationTaskException {
		String configArea = console.readString("Konfgurationsbereich: ", getDefaultConfigurationArea());
		String pid = console.readString("Pid: ", "benutzer." + userName.toLowerCase(Locale.GERMAN));
		_userAdministration.createNewUser(_orderer, _ordererPassword, userName, pid, password.toString(), admin, configArea);
	}

	private String getDefaultConfigurationArea() {
		return _connection.getDefaultConfigurationArea((DynamicObjectType) _dataModel.getType("typ.benutzer")).getPid();
	}

	@Override
	public void deleteUser(final String userName) throws ConfigurationTaskException {
		_userAdministration.deleteUser(_orderer, _ordererPassword, userName);
	}

	@Override
	public Map<Integer, String> createOneTimePasswords(final String userName, final Collection<String> passwords) throws ConfigurationTaskException {
		int firstIndex = _userAdministration.createOneTimePasswords(_orderer, _ordererPassword, userName, passwords.toArray(new String[0]));
		final Map<Integer, String> result = new TreeMap<>();
		int i = 0;
		for(String password : passwords) {
			int passwordIndex = firstIndex + i;
			result.put(passwordIndex, password);
			i++;
		}
		return result;
	}

	@Override
	public void clearOneTimePasswords(final String userName) throws ConfigurationTaskException {
		_userAdministration.clearSingleServingPasswords(_orderer, _ordererPassword, userName);
	}

	@Override
	public int[] getOneTimePasswordIDs(final String userName) throws ConfigurationTaskException {
		return _userAdministration.getValidOneTimePasswordIDs(_orderer, _ordererPassword, userName);
	}

	@Override
	public void disableOneTimePassword(final String userName, final int passwordID) throws ConfigurationTaskException {
		_userAdministration.disableOneTimePassword(_orderer, _ordererPassword, userName, passwordID);
	}

	@Override
	public String getDavPid() {
		return _connection.getLocalDav().getPid();
	}
}
