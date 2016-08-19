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

import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Klasse, mit Daten, die in der Konfiguration an einem Benutzer gespeichert werden, um diesen später authentifizieren zu können.
 * 
 * Elementar für das SRP-Protokoll sind hier v und s, zusätzlich werden die bei der Erzeugung verwendeten Parameter kryptographischen Parameter gespeichert,
 * damit der Client (bei ggf. geänderten Default-Werten) noch passende SRP-Werte berechnen kann und dadurch nicht ausgesperrt wird.
 * 
 * @author Kappich Systemberatung
 */
public class SrpVerifierData {

	private static final Pattern PARSE_PATTERN = Pattern.compile("^SRP6~~~~ v:(\\p{XDigit}+) s:(\\p{XDigit}+) (.+)$");

	private final BigInteger _verifier;

	private final BigInteger _salt;
	
	private final SrpCryptoParameter _srpCryptoParameter;

	/** 
	 * Erstellt ein neues SrpVerifierData-Objekt mit den gegebenen Parametern
	 * @param verifier SRP-Verifier
	 * @param salt Salt
	 * @param srpCryptoParameter Kryptographische Parameter
	 */
	public SrpVerifierData(final BigInteger verifier, final BigInteger salt, final SrpCryptoParameter srpCryptoParameter) {
		_verifier = verifier;
		_salt = salt;
		_srpCryptoParameter = srpCryptoParameter;
	}

	/**
	 * Liest die Werte aus einem String ein, kompatibel mit {@link #toString()}
	 * @param s String-Darstellung
	 * @throws IllegalArgumentException Bei einem String, der nicht dem erwarteten Format entspricht
	 */
	public SrpVerifierData(String s) {
		final Matcher matcher = PARSE_PATTERN.matcher(s.trim());
		if(matcher.matches()){
			try {
				_verifier = SrpUtilities.bigIntegerFromHex(matcher.group(1));
				_salt = SrpUtilities.bigIntegerFromHex(matcher.group(2));
				_srpCryptoParameter = new SrpCryptoParameter(matcher.group(3));
			}
			catch(IllegalArgumentException e){
				throw new IllegalArgumentException("Kein gültiges SRP-Format: " + s, e);
			}
		}
		else {
			throw new IllegalArgumentException("Kein gültiges SRP-Format: " + s);
		}
	}

	/** 
	 * Gibt den Überprüfungscode v zurück
	 * @return den Überprüfungscode v
	 */
	public BigInteger getVerifier() {
		return _verifier;
	}

	/** 
	 * Gibt das Salt s zurück
	 * @return das Salt s
	 */
	public BigInteger getSalt() {
		return _salt;
	}

	/** 
	 * Gibt die kryptografischen Parameter zurück
	 * @return die kryptografischen Parameter, mit denen der Verifier und das Salt erzeugt wurden.
	 */
	public SrpCryptoParameter getSrpCryptoParameter() {
		return _srpCryptoParameter;
	}

	@Override
	public String toString() {
		return String.format("SRP6~~~~ v:%s s:%s %s", SrpUtilities.bigIntegerToHex(_verifier), SrpUtilities.bigIntegerToHex(_salt), _srpCryptoParameter.toString());
	}

}
