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

import java.util.Objects;

/**
 * Informationen über den Verschlüsselungsstatus einer Verbindung
 *
 * @author Kappich Systemberatung
 */
public class EncryptionStatus {
	private final boolean _isEncrypted;
	private final String _cipher;

	/**
	 * Erstellt eine neue Instanz
	 * @param isEncrypted Ist die Verbindung verschlüsselt?
	 * @param cipher Verwendete Verschlüsselung
	 */
	private EncryptionStatus(final boolean isEncrypted, final String cipher) {
		Objects.requireNonNull(cipher, "cipher == null");
		_isEncrypted = isEncrypted;
		_cipher = cipher;
	}

	/**
	 * Erstellt eine neue Instanz für eine nicht verschlüsselte Verbindung
	 * @return Instanz
	 */
	public static EncryptionStatus notEncrypted() {
		return new EncryptionStatus(false, "");
	}

	/**
	 * Erstellt eine neue Instanz für eine verschlüsselte Verbindung
	 * @param cipherName Verwendeter Verschlüsselungsalgorithmus
	 * @return Instanz
	 */
	public static EncryptionStatus encrypted(final String cipherName) {
		return new EncryptionStatus(true, cipherName);
	}

	/** 
	 * Gibt <tt>true</tt> zurück, wenn die Verbindung verschlüsselt ist
	 * @return <tt>true</tt>, wenn die Verbindung verschlüsselt ist, sonst <tt>false</tt>
	 */
	public boolean isEncrypted() {
		return _isEncrypted;
	}

	/** 
	 * Gibt den Verschlüsselungsalgorithmus zurück
	 * @return den Verschlüsselungsalgorithmus oder einen Leerstring falls nicht verschlüsselt
	 */
	public String getCipher() {
		return _cipher;
	}

	@Override
	public boolean equals(final Object o) {
		if(this == o) return true;
		if(!(o instanceof EncryptionStatus)) return false;

		final EncryptionStatus that = (EncryptionStatus) o;

		if(_isEncrypted != that._isEncrypted) return false;
		return _cipher.equals(that._cipher);

	}

	@Override
	public int hashCode() {
		int result = (_isEncrypted ? 1 : 0);
		result = 31 * result + _cipher.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return _isEncrypted ? _cipher : "Nicht verschlüsselt";
	}
}
