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

import com.nimbusds.srp6.SRP6CryptoParams;
import com.nimbusds.srp6.SRP6Exception;
import com.nimbusds.srp6.SRP6ServerSession;
import de.bsvrz.dav.daf.main.InconsistentLoginException;

import java.math.BigInteger;

/**
 * Wrapper-Klasse, die die Nimbus-SRP-Implementierung serverseitig kapselt
 *
 * @author Kappich Systemberatung
 */
public class SrpServerAuthentication {
	private final SRP6ServerSession _srp6ServerSession;
	private final SrpCryptoParameter _srpCryptoParams;

	/** 
	 * Erstellt eine neue SrpServerAuthentication-Instanz
	 * @param srpCryptoParams Kryptographische Parameter, die der Server zur Verifizierung verwendet und an den Client übermittelt 
	 */
	public SrpServerAuthentication(final SrpCryptoParameter srpCryptoParams) {
		_srp6ServerSession = new SRP6ServerSession(SRP6CryptoParams.getInstance(srpCryptoParams.getSrpPrimeBits(), srpCryptoParams.getHashFunction()));
		_srpCryptoParams = srpCryptoParams;
	}

	/**
	 * SRP-Authentifizierung Schritt 1. Der Client übermittelt seinen Benutzernamen an den Server, welcher daraufhin zu diesem Benutzer das gespeicherte Salt
	 * und den Überprüfungscode aus der Konfiguration holt.
	 * @param userName Benutzername (vom Client)
	 * @param salt Salt (von der Konfiguration)
	 * @param verifier Überprüfungscode (von der Konfiguration)
	 * @param mockUser Der Benutzer existiert nicht. Falls true können daher vorgetäuschte Fake-Verifier- und Salt-Werte an den Benutzer gesendet werden, damit dieser
	 *                 nicht unterscheiden kann, ob der Benutzer existiert oder nicht (und damit kein zusätzliches Telegramm für eine negative Quittung gebraucht wird).
	 *                 Das Verhalten dieser Methode ist identisch, egal wie dieser Parameter gesetzt wird, aber falls dieser Parameter true ist, wird ein Flag gesetzt,
	 *                 sodass in Schritt 2 die Authentifizierung auf jeden Fall abgelehnt wird, auch wenn der Client irgendwie eine gültige Antwort generieren kann
	 *                 (was aber praktisch ausgeschlossen ist).
	 * @return Der öffentliche Server-Wert B
	 */
	public BigInteger step1(final String userName, final BigInteger salt, final BigInteger verifier, boolean mockUser) {
		if(mockUser){
			return _srp6ServerSession.mockStep1(userName, salt, verifier);
		}
		else {
			return _srp6ServerSession.step1(userName, salt, verifier);
		}
	}

	/**
	 * SRP-Authentifizierung Schritt 2. Der Client übermittelt seinen öffentlichen Client-Wert A und den Überprüfungscode M1 an den Server, welcher mit dem
	 * Überprüfungscode M2 antwortet.
	 * @param a Öffentlicher Client-Wert A
	 * @param m1 Überprüfungscode M1
	 * @return Überprüfungscode M2
	 * @throws InconsistentLoginException Wenn der Client falsche Login-Daten benutzt
	 */
	public BigInteger step2(final BigInteger a, final BigInteger m1) throws InconsistentLoginException {
		try {
			return _srp6ServerSession.step2(a, m1);
		}
		catch(SRP6Exception e) {
			String reason = "Unbekannter Fehler";
			switch(e.getCauseType()){
				case BAD_PUBLIC_VALUE:
					reason = "Der Client verwendet unsichere Parameter";
					break;
				case BAD_CREDENTIALS:
					reason = "Die Authentifikationsdaten sind fehlerhaft";
					break;
				case TIMEOUT:
					reason = "Timeout";
					break;
			}
			throw new InconsistentLoginException(reason, e);
		}
	}

	/** 
	 * Gibt den Sitzungsschlüssel zurück
	 * @return den Sitzungsschlüssel
	 */
	public BigInteger getSessionKey() {
		return _srp6ServerSession.getSessionKey();
	}

	/**
	 * Gibt den Namen des authentifizierten Benutzers zurück
	 * @return Benutzername oder null
	 */
	public String getAuthenticatedUser() {
		return _srp6ServerSession.getUserID();
	}

	/** 
	 * Gibt die kryptographischen Parameter zurück
	 * @return die kryptographischen Parameter
	 */
	public SrpCryptoParameter getSrpCryptoParams() {
		return _srpCryptoParams;
	}
}
