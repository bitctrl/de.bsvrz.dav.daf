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
import de.bsvrz.dav.daf.userManagement.ConsoleInterface;
import de.bsvrz.dav.daf.userManagement.UserManagement;
import de.bsvrz.dav.daf.userManagement.UserManagementFileOffline;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Kommandozeilenaktion zur Offline-Benutzerverwaltung (auswahl der XML-Datei)
 *
 * @author Kappich Systemberatung
 */
public class UserManagementWithFile extends CommandLineAction {

	private UserManagementFileOffline _userManagementFileOffline;

	@Override
	public String toString() {
		return "Benutzerverwaltung über benutzerverwaltung.xml";
	}

	@Override
	protected void execute(final ConsoleInterface console) throws ParserConfigurationException {
		console.writeLine("Hinweis: Das Programm kann auch direkt mit dem Aufrufparameter \"-offline=pfad/zur/benutzerverwaltung.xml\" gestartet werden.");
		String path = console.readString("Ordner, in dem sich die benutzerverwaltung.xml befindet: ", ".");
		File userFile = new File(path, "benutzerverwaltung.xml");
		if(!userFile.exists()){
			throw new IllegalArgumentException("Datei \"" + userFile.getAbsolutePath() + "\" existiert nicht.");
		}
		_userManagementFileOffline = new UserManagementFileOffline(userFile);
		Runtime.getRuntime().addShutdownHook(new Thread(){
			@Override
			public void run() {
				dispose(console);
			}
		});
	}

	@Override
	protected void dispose(final ConsoleInterface console) {
		try {
			_userManagementFileOffline.close();
		}
		catch(IOException e) {
			console.writeLine("Datei konnte nicht geschlossen werden.");
			e.printStackTrace();
		}
	}

	@Override
	public List<? extends CommandLineAction> getChildren() {
		return UserManagement.getActions(_userManagementFileOffline);
	}
}
