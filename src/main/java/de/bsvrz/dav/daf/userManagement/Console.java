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

import java.io.*;

/**
 * TBD Dokumentation
 *
 * @author Kappich Systemberatung
 */
public class Console implements ConsoleInterface {

	private final java.io.Console _console;
	
	private Console(final java.io.Console console) {
		_console = console;
	}
	
	public static ConsoleInterface getInstance() {
		java.io.Console systemConsole = System.console();
		if(systemConsole != null){
			return new Console(systemConsole);
		}
		return new StdConsole();
	}

	@Override
	public String readLine(final String prompt, final Object... parameter) {
		return _console.readLine(prompt, parameter);
	}

	@Override
	public char[] readPassword(final String prompt, final Object... parameter) {
		return _console.readPassword(prompt, parameter);
	}

	@Override
	public void writeLine(final String prompt, final Object... parameter) {
		_console.printf(prompt, parameter);
		_console.printf("%n");
	}

	private static class StdConsole implements ConsoleInterface {

		private final BufferedReader _reader;
		private final BufferedWriter _writer;

		public StdConsole() {
			_reader = new BufferedReader(new InputStreamReader(System.in));
			_writer = new BufferedWriter(new OutputStreamWriter(System.out));
		}

		@Override
		public String readLine(final String prompt, final Object... parameter) {
			try {
				_writer.write(String.format(prompt, parameter));
				_writer.flush();
				return _reader.readLine();
			}
			catch(IOException e) {
				throw new AssertionError(e);
			}
		}

		@Override
		public char[] readPassword(final String prompt, final Object... parameter) {
			try {
				_writer.write(String.format(prompt, parameter));
				_writer.flush();
				String line = _reader.readLine();
				if(line == null) return null;
				return line.toCharArray();
			}
			catch(IOException e) {
				throw new AssertionError(e);
			}
		}

		@Override
		public void writeLine(final String prompt, final Object... parameter) {
			try {
				_writer.write(String.format(prompt, parameter));
				_writer.newLine();
				_writer.flush();
			}
			catch(IOException e) {
				throw new AssertionError(e);
			}
		}
	}
}
