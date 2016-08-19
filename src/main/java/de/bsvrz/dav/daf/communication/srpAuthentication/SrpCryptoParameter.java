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

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parameter für die Authentifizierung mit SRP und die nachfolgende Verschlüsselung
 *
 * @author Kappich Systemberatung
 */
public class SrpCryptoParameter {

	private static final Pattern PARSE_PATTERN = Pattern.compile("^t:(\\p{Digit}+) L:(\\p{Digit}+) H:(\\p{Graph}+) KDF:(\\p{Graph}+) c:(\\p{Digit}+) dkLen:(\\p{Digit}+) n:(\\p{Digit}+) sLen:(\\p{Digit}+)$");

	/**
	 * Anzahl Bits t zur Sicherstellung der Nachrichtenintegrität mit AES-GCM. Jedes verschlüsselte Telegramm wird mit einer Prüfsumme dieser Länge versehen. Empfohlener Minimum-Wert: 96
	 */
	private final int _gcmAuthenticationTagBits;

	/**
	 * Anzahl Bits L der Schlüssellänge der AES-Verschlüsselung. Mögliche Werte: 128, 192, 256. (Größere Werte als 128 benötigen möglicherweise die "Unlimited
	 * Strength Java Cryptography Extension Policy Files").
	 */
	private final int _aesKeyLengthBits;

	/**
	 * Auswahl der Hashfunktion H in SRP zur Berechnung von u und dem Sitzungsschlüssel (unter anderem), beispielsweise "SHA-256".
	 */
	private final String _hashFunction;

	/**
	 * Auswahl der Schlüsselableitungsfunktion KDF zur Berechnung von x, beispielsweise "PBKDF2WithHmacSHA256".
	 */
	private final String _keyDerivationFunction;

	/**
	 * Anzahl der Iterationen c der Schlüsselableitungsfunktion zur Berechnung von x. Empfohlener Minimum-Wert: 10_000
	 */
	private final int _keyDerivationIterations;

	/**
	 * Anzahl Bits dkLen für das Resultat der Schlüsselableitungsfunktion. Empfohlener Minimum-Wert: 128 
	 */
	private final int _keyDerivationHashBits;

	/**
	 * Anzahl Bits n der SRP-Primzahl N. Empfohlener Minimum-Wert: 1024
	 */
	private final int _srpPrimeBits;

	/**
	 * Anzahl Bytes sLen für das SRP-Salt. Empfohlener Minimum-Wert: 16
	 */
	private final int _srpSaltBytes;

	/**
	 * Erstellt eine Instanz mit Standardparametern
	 */
	private SrpCryptoParameter() {
		this(
				96,
		        128,
				"SHA-256",
				"PBKDF2WithHmacSHA256",
				20_000,
		        256,
		        1024,
		        16
		);
	}

	/**
	 * Gibt die als Standard (ggf. über die Systemproperty) festgelegten kryptografischen Parameter zurück.
	 * @return Standard-Parameter für Authentifizierung und Verschlüsselung
	 */
	public static SrpCryptoParameter getDefaultInstance(){
		return DefaultInstanceHolder.getInstance();
	}

	/**
	 * Erstellt eine Instanz mit benutzerdefinierten Parametern
	 *
	 * @param gcmAuthenticationTagBits Anzahl Bits zur Sicherstellung der Nachrichtenintegrität mit AES-GCM. Empfohlener Minimum-Wert: 96, kleinere Werte
	 *                                 verringern den Overhead, vereinfachen es aber möglicherweise, Nachrichten zu fälschen
	 * @param aesKeyLengthBits         Anzahl Bits der Schlüssellänge der AES-Verschlüsselung. Mögliche Werte: 128, 192, 256. (Größere Werte als 128 benötigen
	 *                                 möglicherweise die "Unlimited Strength Java Cryptography Extension Policy Files").
	 * @param hashFunction             Hashfunktion H in SRP zur Berechnung von u und dem Sitzungsschlüssel (unter anderem), beispielsweise "SHA-256".
	 * @param keyDerivationFunction    Auswahl der Schlüsselableitungsfunktion zur Berechnung von x, beispielsweise "PBKDF2WithHmacSHA256".
	 * @param keyDerivationIterations  Anzahl der Iterationen der Schlüsselableitungsfunktion zur Berechnung von x. Empfohlener Minimum-Wert: 10_000
	 * @param keyDerivationHashBits    Anzahl Bits für das Resultat der Schlüsselableitungsfunktion. Empfohlener Minimum-Wert: 128
	 * @param srpPrimeBits             Anzahl Bits der SRP-Primzahl N. Empfohlener Minimum-Wert: 1024
	 * @param srpSaltBytes             Anzahl Bytes für das SRP-Salt. Empfohlener Minimum-Wert: 16
	 * @throws NullPointerException     wenn einer der Strings null ist.
	 * @throws IllegalArgumentException bei negativen oder anderweitig implausiblen Bit-/Byte-Werten. Dieser Konstruktor prüft nur auf offensichtlich
	 *                                  fehlerhafte Werte und nimmt keine inhaltliche Prüfung auf sinnvolle Cryptoparameter vor.
	 */
	public SrpCryptoParameter(final int gcmAuthenticationTagBits, final int aesKeyLengthBits, final String hashFunction, final String keyDerivationFunction, final int keyDerivationIterations, final int keyDerivationHashBits, final int srpPrimeBits, final int srpSaltBytes) {
		_gcmAuthenticationTagBits = gcmAuthenticationTagBits;
		_aesKeyLengthBits = aesKeyLengthBits;
		_hashFunction = hashFunction;
		_keyDerivationFunction = keyDerivationFunction;
		_keyDerivationIterations = keyDerivationIterations;
		_keyDerivationHashBits = keyDerivationHashBits;
		_srpPrimeBits = srpPrimeBits;
		_srpSaltBytes = srpSaltBytes;
		validateValues();
	}

	/**
	 * Liest die Werte aus einem String ein, kompatibel mit {@link #toString()}
	 * @param cryptoParamsString String-Darstellung
	 * @throws IllegalArgumentException Bei einem String, der nicht dem erwarteten Format entspricht
	 */
	public SrpCryptoParameter(final String cryptoParamsString) throws IllegalArgumentException{
		final Matcher matcher = PARSE_PATTERN.matcher(cryptoParamsString.trim());
		if(matcher.matches()){
			_gcmAuthenticationTagBits = Integer.parseInt(matcher.group(1));
			_aesKeyLengthBits = Integer.parseInt(matcher.group(2));
			_hashFunction = matcher.group(3);
			_keyDerivationFunction = matcher.group(4);
			_keyDerivationIterations = Integer.parseInt(matcher.group(5));
			_keyDerivationHashBits = Integer.parseInt(matcher.group(6));
			_srpPrimeBits = Integer.parseInt(matcher.group(7));
			_srpSaltBytes = Integer.parseInt(matcher.group(8));
			validateValues();
		}
		else {
			throw new IllegalArgumentException("Kein gültiges Format für kryptographische Parameter: \"" + cryptoParamsString + "\". Gültiges Format (Beispiel):\"" + new SrpCryptoParameter() + "\"");
		}
	}

	private void validateValues() {
		// Parameter prüfen
		Objects.requireNonNull(_hashFunction, "hashFunction == null");
		Objects.requireNonNull(_keyDerivationFunction, "keyDerivationFunction == null");
		if(_gcmAuthenticationTagBits % 8 != 0 || _gcmAuthenticationTagBits <= 0) throw new IllegalArgumentException("gcmAuthenticationTagBits: " + _gcmAuthenticationTagBits);
		if(_aesKeyLengthBits != 128 && _aesKeyLengthBits != 192 && _aesKeyLengthBits != 256) throw new IllegalArgumentException("aesKeyLengthBits: " + _aesKeyLengthBits);
		if(_keyDerivationIterations <= 0) throw new IllegalArgumentException("keyDerivationIterations: " + _keyDerivationIterations);
		if(_keyDerivationHashBits % 8 != 0 || _keyDerivationHashBits <= 0) throw new IllegalArgumentException("keyDerivationHashBits: " + _keyDerivationHashBits);
		if(_srpPrimeBits < 256) throw new IllegalArgumentException("srpPrimeBits < 256: " + _srpPrimeBits);
		if(_srpSaltBytes < 16) throw new IllegalArgumentException("srpSaltBytes < 16: " + _srpSaltBytes);
	}

	/**
	 * @return Anzahl Bits zur Sicherstellung der Nachrichtenintegrität mit AES-GCM.
	 */
	public int getGcmAuthenticationTagBits() {
		return _gcmAuthenticationTagBits;
	}


	/**
	 * @return Anzahl Bits der Schlüssellänge der AES-Verschlüsselung.
	 */
	public int getAesKeyLengthBits() {
		return _aesKeyLengthBits;
	}

	/**
	 * @return Hashfunktion H in SRP
	 */
	public String getHashFunction() {
		return _hashFunction;
	}

	/**
	 * @return Schlüsselableitungsfunktion zur Berechnung von x, beispielsweise "PBKDF2WithHmacSHA256"
	 */
	public String getKeyDerivationFunction() {
		return _keyDerivationFunction;
	}

	/**
	 * @return Anzahl der Iterationen der Schlüsselableitungsfunktion
	 */
	public int getKeyDerivationIterations() {
		return _keyDerivationIterations;
	}

	/**
	 * @return Anzahl Bits für das Resultat der Schlüsselableitungsfunktion.
	 */
	public int getKeyDerivationHashBits() {
		return _keyDerivationHashBits;
	}

	/**
	 * @return Anzahl Bits der SRP-Primzahl N.
	 */
	public int getSrpPrimeBits() {
		return _srpPrimeBits;
	}

	/**
	 * @return Anzahl Bytes für das SRP-Salt.
	 */
	public int getSrpSaltBytes() {
		return _srpSaltBytes;
	}

	@Override
	public String toString() {
		return String.format("t:%s L:%s H:%s KDF:%s c:%s dkLen:%s n:%s sLen:%s", 
		                     _gcmAuthenticationTagBits,
	                         _aesKeyLengthBits,
	                         _hashFunction,
	                         _keyDerivationFunction,
	                         _keyDerivationIterations,
	                         _keyDerivationHashBits,
	                         _srpPrimeBits,
	                         _srpSaltBytes);
	}

	private static class DefaultInstanceHolder {
		private static final SrpCryptoParameter _instance;

		static {
			String srpParams = System.getProperty("srp6.params");
			if(srpParams == null){
				_instance = new SrpCryptoParameter(); // Default-Werte
			}
			else {
				_instance = new SrpCryptoParameter(srpParams);
			}
		}
		
		public static SrpCryptoParameter getInstance() {
			return _instance;
		}
	}
}
