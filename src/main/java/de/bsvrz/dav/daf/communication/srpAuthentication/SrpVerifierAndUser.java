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

package de.bsvrz.dav.daf.communication.srpAuthentication;

import de.bsvrz.dav.daf.communication.protocol.UserLogin;

import java.util.Objects;

/**
 * Antwort von der Konfiguration für die Anfrage nach s und v eines Benutzers
 * 
 * @see de.bsvrz.dav.daf.main.config.management.UserAdministration#getSrpVerifier(String, String, String, int)
 * 
 * @author Kappich Systemberatung 
 */
public class SrpVerifierAndUser {
	private final UserLogin _userLogin;
	private final SrpVerifierData _verifierData;
	private final boolean _isPlainTextPassword;

	/**
	 * Erstellt eine neue Instanz
	 * @param userLogin Der Benutzer (ob er existiert oder nicht, ggf. die Benutzer-ID)
	 * @param verifierData Der SRP-Überprüfungscode plus Metadaten
	 * @param isPlainTextPassword True wenn das Passwort den Benutzers im Klartext gespeichert ist, sonst false
	 */
	public SrpVerifierAndUser(final UserLogin userLogin, final SrpVerifierData verifierData, final boolean isPlainTextPassword) {
		Objects.requireNonNull(verifierData, "verifierData == null");
		_userLogin = userLogin;
		_verifierData = verifierData;
		_isPlainTextPassword = isPlainTextPassword;
	}

	/**
	 * @return Der Benutzer (ob er existiert oder nicht, ggf. die Benutzer-ID)
	 */
	public UserLogin getUserLogin() {
		return _userLogin;
	}

	/**
	 * @return Der SRP-Überprüfungscode den Benutzers plus Metadaten
	 */
	public SrpVerifierData getVerifier() {
		return _verifierData;
	}

	/**
	 * Gibt zurück, ob das Passwort in der Konfiguration im Klartext gespeichert ist und nur ein künstlicher Verifier erzeugt wurde.
	 * Diese Information kann dazu verwendet werden, den Client zu warnen, dass er ein neues verschlüsseltes passwort setzen sollte.
	 * @return true: Klartextpasswort, sonst false
	 */
	public boolean isPlainTextPassword() {
		return _isPlainTextPassword;
	}

	@Override
	public String toString() {
		return "SrpVerifierAndAuthentication{" +
				"_userId=" + _userLogin +
				", _verifierData=" + _verifierData +
				", _isPlainTextPassword=" + _isPlainTextPassword +
				'}';
	}

	@Override
	public boolean equals(final Object o) {
		if(this == o) return true;
		if(o == null || getClass() != o.getClass()) return false;

		final SrpVerifierAndUser that = (SrpVerifierAndUser) o;

		if(_isPlainTextPassword != that._isPlainTextPassword) return false;
		if(!_userLogin.equals(that._userLogin)) return false;
		return _verifierData.equals(that._verifierData);

	}

	@Override
	public int hashCode() {
		int result = _userLogin.hashCode();
		result = 31 * result + _verifierData.hashCode();
		result = 31 * result + (_isPlainTextPassword ? 1 : 0);
		return result;
	}
}
