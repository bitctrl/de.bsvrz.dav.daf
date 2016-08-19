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
 * Informationen über den Authentifizierungsstatus einer Verbindung
 *
 * @author Kappich Systemberatung
 */
public class AuthenticationStatus {
	private final boolean _isAuthenticated;
	private final String _method;

	/**
	 * Erstellt eine neue Instanz
	 * @param isAuthenticated Ist die Verbindung authentifiziert?
	 * @param method Verwendete Authentifizierung
	 */
	private AuthenticationStatus(final boolean isAuthenticated, final String method) {
		Objects.requireNonNull(method, "cipher == null");
		_isAuthenticated = isAuthenticated;
		_method = method;
	}

	/**
	 * Erstellt eine neue Instanz für eine nicht authentifizierte Verbindung
	 * @return Instanz
	 */
	public static AuthenticationStatus notAuthenticated() {
		return new AuthenticationStatus(false, "");
	}

	/**
	 * Erstellt eine neue Instanz für eine authentifizierte Verbindung
	 * @param cipherName Verwendeter Authentifizierungsalgorithmus
	 * @return Instanz
	 */
	public static AuthenticationStatus authenticated(final String cipherName) {
		return new AuthenticationStatus(true, cipherName);
	}

	/** 
	 * Gibt <tt>true</tt> zurück, wenn die Verbindung authentifiziert ist
	 * @return <tt>true</tt>, wenn die Verbindung authentifiziert ist, sonst <tt>false</tt>
	 */
	public boolean isAuthenticated() {
		return _isAuthenticated;
	}

	/** 
	 * Gibt den Authentifizierungsalgorithmus zurück
	 * @return den Authentifizierungsalgorithmus oder einen Leerstring falls nicht authentifiziert
	 */
	public String getMethod() {
		return _method;
	}

	@Override
	public boolean equals(final Object o) {
		if(this == o) return true;
		if(!(o instanceof AuthenticationStatus)) return false;

		final AuthenticationStatus that = (AuthenticationStatus) o;

		if(_isAuthenticated != that._isAuthenticated) return false;
		return _method.equals(that._method);

	}

	@Override
	public int hashCode() {
		int result = (_isAuthenticated ? 1 : 0);
		result = 31 * result + _method.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return _isAuthenticated ? _method : "Nicht authentifiziert";
	}
}
