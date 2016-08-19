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

package de.bsvrz.dav.daf.main.authentication;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Implementierung des interaktiven Login
 *
 * @author Kappich Systemberatung
 */
public abstract class InteractiveAuthentication implements UserProperties{
	public static InteractiveAuthentication getInstance() {
		Console console = System.console();
		if(console == null){
			return new StdInAuthentication();
		}
		else {
			return new ConsoleAuthentication(console);
		}
	}

	private static String getPrompt(final String userName, final String suffix) {
		final String prompt;
		if(suffix == null) {
			prompt = "Passwort für \"" + userName + "\": ";
		}
		else {
			prompt = "Passwort für \"" + userName + "\" an \"" + suffix + "\": ";
		}
		return prompt;
	}

	private static class StdInAuthentication extends InteractiveAuthentication {
		@Override
		public ClientCredentials getClientCredentials(final String userName, final String suffix) {
			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
				System.err.println("Warnung: Das Passwort kann bei der Eingabe nicht versteckt werden. Trotzdem Fortfahren? (Bitte \"ja\" tippen und mit Enter bestätigen)");
				String answer = br.readLine();
				if(answer != null && answer.toLowerCase().equals("ja")) {
					System.out.print(getPrompt(userName, suffix));
					return ClientCredentials.ofPassword(br.readLine().toCharArray());
				}
			}
			catch(IOException e){
				System.err.println("Das Passwort konnte nicht gelesen werden: ");
				e.printStackTrace();
			}
			return null;
		}
		
	}

	private static class ConsoleAuthentication extends InteractiveAuthentication {

		private final Console _console;

		public ConsoleAuthentication(final Console console) {
			_console = console;
		}

		@Override
		public ClientCredentials getClientCredentials(final String userName, final String suffix) {
			return ClientCredentials.ofPassword(_console.readPassword(getPrompt(userName, suffix)));
		}
	}
}


