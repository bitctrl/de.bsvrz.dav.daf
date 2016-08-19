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

import java.util.*;

/**
 * Befehl im Benutzerverwaltungs-/Migrationstool
 *
 * @author Kappich Systemberatung
 */
public abstract class CommandLineAction {

	private CommandLineAction _parent;
	
	private boolean _skip = false;

	/**
	 * Führt den Befehl aus
	 * @param console Konsole
	 * @param parent Übergeordneter Befehl oder null falls es sich um die "Wurzel" handelt.
	 */
	public final void execute(ConsoleInterface console, CommandLineAction parent){
		_parent = parent;
		try {
			console.writeLine("");
//				console.writeLine(stack());
			execute(console);
			List<? extends CommandLineAction> children = getChildren();
			if(children.isEmpty() || _skip) {
				dispose(console);
				return;
			}
//			else if(children.size() == 1) {
//				children.get(0).execute(console, this);
//			}
			else {
				_skip = false;
				while(true) {
					console.writeLine("");
					printStatus(console);
					console.writeLine("");
					console.writeLine("Navigation");
					final List<CommandLineAction> actions = new ArrayList<>();
					for(CommandLineAction action : children) {
						actions.add(action);
						console.writeLine(actions.size() + ": " + action.toString());
					}
					if(_parent != null) {
						console.writeLine("0: Zurück");
					}
					console.writeLine("q: Beenden");
					String line = console.readLine("Auswahl: ");
					if(line == null) {
						line = "q";
					}
					String s = line.trim();
					if(s.isEmpty()) continue;
					if(s.equals("q")) {
						CommandLineAction p = this;
						while(p != null) {
							p.dispose(console);
							p = p._parent;
						}
						System.exit(0);
					}
					try {
						int i = Integer.parseInt(s);
						if(_parent != null && i == 0) {
							dispose(console);
							return;
						}
						else {
							if(i < 1 || i > actions.size()) {
								console.writeLine("Unbekannte Auswahl: " + i);
								continue;
							}
							actions.get(i - 1).execute(console, this);
							if(_skip) {
								dispose(console);
								return;
							}
						}
						children = getChildren();
					}
					catch(NumberFormatException ignored){
					}
				}
			}
		}
		catch(Exception e) {
			String localizedMessage = e.getLocalizedMessage();
			if(localizedMessage != null) {
				console.writeLine("Fehler: %s", localizedMessage);
			}
			else {
				console.writeLine("Fehler: %s", e.getClass().getSimpleName());
			}
			try {
				dispose(console);
			}
			catch(Exception ignored) {
			}
		}
	}

	/**
	 * Gibt einen Text vor der Auswahl der Aktion aus (zum überschreiben)
	 * @param console Konsole
	 * @throws Exception
	 */
	public void printStatus(final ConsoleInterface console) throws Exception {
		
	}

	private String stack() {
		final ArrayDeque<String> actions = new ArrayDeque<>();
		CommandLineAction tmp = this;
		while(tmp != null) {
			if(actions.size() > 3) {
				actions.addFirst("...");
				break;
			}
			actions.addFirst(tmp.toString());
			tmp = tmp._parent;
		}
		StringBuilder stringBuilder = new StringBuilder();
		for(Iterator<String> iterator = actions.iterator(); iterator.hasNext(); ) {
			final String action = iterator.next();
			stringBuilder.append(action);
			if(iterator.hasNext()){
				stringBuilder.append(" > ");
			}
		}
		return stringBuilder.toString();
	}


	/**
	 * Führt den eigentlichen Befehl aus
	 * @param console
	 * @throws Exception
	 */
	protected void execute(final ConsoleInterface console) throws Exception {}

	/**
	 * Wird aufgerufen wenn der Befehl verlassen wird
	 * @param console
	 * @throws Exception
	 */
	protected void dispose(final ConsoleInterface console) throws Exception {}

	/**
	 * Gibt den Befehlsnamen zurück
	 * @return Name
	 */
	public abstract String toString();

	/**
	 * Gibt die Kindbefehle zurück
	 * @return Kindbefehle
	 */
	public List<? extends CommandLineAction> getChildren() {return Collections.emptyList();}
	
	public final void skipParent(){
		if(_parent != null) {
			_parent._skip = true;
		}
	}
	
}
