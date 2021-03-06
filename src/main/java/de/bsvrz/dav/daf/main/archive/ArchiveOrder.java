/*
 * Copyright 2007 by Kappich Systemberatung, Aachen
 * Copyright 2005 by Kappich+Kniß Systemberatung Aachen (K2S)
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
package de.bsvrz.dav.daf.main.archive;

/**
 * Ein Objekt dieser Klasse bestimmt, ob die nachgelieferten Archivdaten, die zu einer Archivanfrage gehören, nach ihrem
 * Datenindex oder nach ihrer Datenzeit sortiert und in Strom von nicht nachgelieferten Archivdaten einsortiert werden
 * sollen.
 *
 * @author Kappich Systemberatung
 * @version $Revision$
 */
public class ArchiveOrder {
	/**
	 * nach Datenindex sortieren
	 */
	public static final ArchiveOrder BY_INDEX = new ArchiveOrder("nach Index sortiert", 1);
	/**
	 * nach Datenzeit sortieren
	 */
	public static final ArchiveOrder BY_DATA_TIME = new ArchiveOrder("nach Datenzeit sortiert", 2);

	/**
	 * Diese Methode wandelt den übergebenen Parameter in ein Objekt dieser Klasse um
	 *
	 * @param code Der Code bestimmt welches Objekt dieser Klasse erzeugt wird
	 * @return eindeutiges Objekt dieser Klasse
	 */
	public static ArchiveOrder getInstance(int code) {

		switch(code) {
		case 1:
			return BY_INDEX;
		case 2:
			return BY_DATA_TIME;
		default:
			throw new IllegalArgumentException("Undefinierte Sortierreihenfolge");
		}
	}

	/**
	 * Wandelt das Objekt in einen String um
	 *
	 * @return String, der ausgegeben werden kann
	 */
	public String toString() {
		return _name;
	}

	/**
	 * Code des Objekts, dieser Code kann zum erzeugen eines identischen Objekts benutzt werden.
	 *
	 * @return Code
	 */
	public int getCode() {
		return _code;
	}

	private final String _name;
	private final int _code;

	private ArchiveOrder(String name, int code) {
		_name = name;
		_code = code;
	}
}
