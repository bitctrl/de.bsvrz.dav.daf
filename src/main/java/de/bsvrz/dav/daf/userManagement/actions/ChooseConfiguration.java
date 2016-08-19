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

import de.bsvrz.dav.daf.communication.srpAuthentication.SrpNotSupportedException;
import de.bsvrz.dav.daf.main.ClientDavConnection;
import de.bsvrz.dav.daf.main.config.DataModel;
import de.bsvrz.dav.daf.main.config.management.UserAdministration;
import de.bsvrz.dav.daf.userManagement.CommandLineAction;
import de.bsvrz.dav.daf.userManagement.ConsoleInterface;
import de.bsvrz.dav.daf.userManagement.UserManagement;
import de.bsvrz.dav.daf.userManagement.UserManagementFileOnline;

import java.util.List;

/**
 * TBD Dokumentation
 *
 * @author Kappich Systemberatung
 */
public class ChooseConfiguration extends CommandLineAction {
	private final ClientDavConnection _connection;
	private String _userName;
	private char[] _password;
	private UserAdministration _userAdministration;
	private boolean _userAdmin;
	private DataModel _dataModel;
	private UserManagementFileOnline _userManagementInterface;

	public ChooseConfiguration(final ClientDavConnection connection, final String userName, final char[] password) {
		_connection = connection;
		_userName = userName;
		_password = password;
	}

	@Override
	public String toString() {
		return "Konfiguration auswählen";
	}

	@Override
	protected void execute(final ConsoleInterface console) throws Exception {
		String defKv = _connection.getDataModel().getConfigurationAuthorityPid();
		String kvPid = console.readString("Die Benutzer folgender AOE verwalten: ", defKv);
		_dataModel = _connection.getDataModel(kvPid);
		_userAdministration = _dataModel.getUserAdministration();
		if(!kvPid.equals(defKv)) {
			_userName = console.readString("Benutzername für Konfiguration", _userName);
			_password = console.readPassword("Passwort für Konfiguration");
		}
		_userAdmin = _userAdministration.isUserAdmin(_userName, new String(_password), _userName);
		_userManagementInterface = new UserManagementFileOnline(_connection, _dataModel, _userAdministration, _userName, _password, _userAdmin);
		if(_userAdmin){
			console.writeLine("Erfolgreich als " + _userName + " mit Administrator-Rechten eingeloggt.");
		}
		else {
			console.writeLine("Erfolgreich als " + _userName + " eingeloggt.");
			console.writeLine("Warnung: Der aktuelle Benutzer ist kein Administrator, einige Funktionen stehen nicht zur Verfügung.");
		}
		try {
			_userManagementInterface.getLoginToken(_userName, _password, -1);
		} catch(SrpNotSupportedException e){
			console.writeLine("");
			console.writeLine("Der Datenverteiler oder die Konfiguration unterstützt die neue Authentifizierung nicht.");
			console.writeLine("Falls die Software bereits aktualisiert wurde, bitte sicherstellen, dass es einen gültigen Benutzer");
			console.writeLine("für den Datenverteiler gibt. (Ggf. neuen Benutzer mit Administratorrechten anlegen und als Aufrufargument vom");
			console.writeLine("Datenverteiler -benutzer=<Benutzername> und -authentifizierung=<Passwortdatei> setzen). Dann das System neu starten.");
		}
	}

	@Override
	public List<? extends CommandLineAction> getChildren() {
		return UserManagement.getActions(_userManagementInterface);
	}
}
