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

package de.bsvrz.dav.daf.main;

/**
 * Gibt die lokale Konfiguration der Verschlüsselung an. Eine Verschlüsselung kommt bei der Authentifizierung immer zustande und wird nur dann
 * aufgelößt, wenn beie Kommunikationspartner den Wunsch äußern, nicht verschlüsseln zu wollen (Wert automatisch bei lokaler Verbindung oder nein).
 *
 * @author Kappich Systemberatung
 */
public enum  EncryptionConfiguration {

	/**
	 * Es soll immer verschlüsselt werden
	 */
	AlwaysEncrypted("immer"),

	/**
	 * Es soll immer verschlüsselt werden, außer es handelt sich um eine lokale (Loopback-) Verbindung
	 */
	Auto("automatisch"),

	/**
	 * Der Kommunikationspartner möchte nie verschlüsseln
	 */
	PreferNoEncryption("nein");

	private final String _toStringValue;

	EncryptionConfiguration(final String toStringValue) {
		_toStringValue = toStringValue;
	}

	@Override
	public String toString() {
		return _toStringValue;
	}

	public boolean shouldDisable(final boolean isLoopbackConnection) {
		switch(this){
			case AlwaysEncrypted:
				return false;
			case Auto:
				return isLoopbackConnection;
			case PreferNoEncryption:
				return true;
		}
		// Sollte nicht auftreten
		assert false;
		return false;
	}
}
