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

/**
 * TBD Dokumentation
 *
 * @author Kappich Systemberatung
 */
public interface ConsoleInterface {

	public String readLine(String prompt, Object... parameter);

	public char[] readPassword(String prompt, Object... parameter);

	public void writeLine(String prompt, Object... parameter);
	
	public default int readInt(String prompt, int defValue){
		while(true) {
			try {
				String maybeInt = readLine(prompt + "(" + defValue + ") ");
				if(maybeInt.isEmpty()) return defValue;
				return Integer.parseInt(maybeInt);
			}
			catch(NumberFormatException ignored){
			}
		}
	}	
	
	public default String readString(String prompt, String defValue){
		String maybeString = readLine(prompt + "(" + defValue + ") ");
		if(maybeString.isEmpty()) return defValue;
		return maybeString;
	}

	public default boolean readBoolean(String prompt, boolean defValue){
		while(true) {
			String maybeString = readLine(prompt + "(" + (defValue ? "J/n" : "j/N") + ") ");
			if(maybeString.isEmpty()) return defValue;
			if(maybeString.toLowerCase().startsWith("j")) return true;
			if(maybeString.toLowerCase().startsWith("n")) return false;
		}
	}
	
}
