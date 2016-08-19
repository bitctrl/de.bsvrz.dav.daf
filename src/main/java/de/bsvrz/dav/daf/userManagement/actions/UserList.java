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

import de.bsvrz.dav.daf.userManagement.CommandLineAction;
import de.bsvrz.dav.daf.userManagement.UserManagementFileInterface;

import java.util.ArrayList;
import java.util.List;

/**
 * Benutzerliste Anzeigen
 *
 * @author Kappich Systemberatung
 */
public class UserList extends CommandLineAction {
	private final UserManagementFileInterface _userManagementInterface;

	public UserList(final UserManagementFileInterface userManagementInterface) {
		_userManagementInterface = userManagementInterface;
	}

	@Override
	public List<? extends CommandLineAction> getChildren() {
		final List<CommandLineAction> result = new ArrayList<>();
		for(String s : _userManagementInterface.getUsers()) {
			result.add(new EditUser(s, _userManagementInterface));
		}
		return result;
	}

	@Override
	public String toString() {
		return "Benutzerliste";
	}
}
