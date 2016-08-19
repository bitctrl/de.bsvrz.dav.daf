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

import com.nimbusds.srp6.SRP6Routines;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Verschiedene allgemeine Hilfsfunktionen für die SRP-Authentifizierung
 *
 * @author Kappich Systemberatung
 */
public class SrpUtilities {

	private SrpUtilities() {
	}

	/**
	 * Gibt die Anzahl Bytes zurück, die die von den angegebenen kryptographischen parametern referenzierte Hashfunktion als Ausgabe verwendet
	 * @param cryptoParams kryptographische Parameter
	 * @return Anzahl Bytes
	 */
	static int getHashLength(final SrpCryptoParameter cryptoParams) {
		int digestLength = 32;
		try {
			MessageDigest messageDigest = MessageDigest.getInstance(cryptoParams.getHashFunction());
			digestLength = messageDigest.getDigestLength();
		}
		catch(NoSuchAlgorithmException ignored) {
		}
		if(digestLength <= 0) digestLength = 32;
		return digestLength;
	}

	/**
	 * Erstellt ein zufälliges Salt
	 * @param cryptoParams Kryptographische Parameter (zur Bestimmung der Anzahl Bytes)
	 * @return zufälliges byte-Array der angegebenen Länge.
	 */
	public static byte[] generateRandomSalt(final SrpCryptoParameter cryptoParams) {
		return SRP6Routines.generateRandomSalt(cryptoParams.getSrpSaltBytes());
	}

	/**
	 * Erstellt ein (zufällig aussehendes) Salt, das aber eindeutig aus dem übergebenen Seed berechnet wird
	 * @param cryptoParams Kryptographische Parameter (hauptsächlich zur Bestimmung der Anzahl Bytes)
	 * @param seed Daten, die Grundlage für das zufällige Salt sind
	 * @return byte-Array der angegebenen Länge, welches aus dem seed gebildet wurde.
	 */
	public static byte[] generatePredictableSalt(final SrpCryptoParameter cryptoParams, final byte[] seed) {
		try {
			byte[] bytes = SrpTelegramEncryption.computeHash(cryptoParams.getSrpSaltBytes(), cryptoParams.getHashFunction(), seed);
			
			// Das Array kann größer sein, also bei Bedarf abschneiden
			return Arrays.copyOf(bytes, cryptoParams.getSrpSaltBytes());
		}
		catch(NoSuchAlgorithmException | DigestException e) {
			throw new IllegalArgumentException("Ungültige SrpCryptoParameter", e);
		}
	}

	/**
	 * Wandelt ein Byte-Array in eine hexadezimale Darstellung um
	 * @param bytes Byte-Array
	 * @return Hex-Darstellung, z.B. "4711af2b"
	 */
	public static String bytesToHex(final byte[] bytes) {
		StringBuilder stringBuilder = new StringBuilder(bytes.length * 2);
		for(byte b : bytes) {
			stringBuilder.append(Character.forDigit(0xF & (b >>> 4), 16));
			stringBuilder.append(Character.forDigit(0xF & b, 16));
		}
		return stringBuilder.toString();
	}

	/**
	 * Wandelt eine hexadezimale Zeichenfolge in ein Byte-Array im
	 * @param str Hex-Zeichen, z.B. "4711af2b"
	 * @return Byte-Array
	 */
	public static byte[] bytesFromHex(final String str) {
		byte[] result = new byte[(str.length() + 1) / 2];
		final char[] charArray = str.toCharArray();
		int idx = result.length - 1;
		boolean even = true;
		for(int i = charArray.length - 1; i >= 0; i--) {
			final char c = charArray[i];
			int digit = Character.digit(c, 16);
			if(digit == -1){
				throw new IllegalArgumentException("Ungültiges Hex-Zeichen '" + c + "' an Position " + i);
			}
			if(even) {
				result[idx] += digit;
			}
			else {
				result[idx] += (digit << 4);
				idx--;
			}
			even = !even;
		}
		return result;
	}

	/**
	 * Wandelt ein vorzeichenloses byte-Array in ein positiven BigInteger um
	 * @param bytes Byte-Array
	 * @return BigInteger
	 */
	public static BigInteger bigIntegerFromBytes(final byte[] bytes) {
		return new BigInteger(1, bytes);
	}

	/**
	 * Wandelt einen BigInteger in ein vorzeichenloses Byte-Array um.
	 * @param bigInteger BigInteger
	 * @return byte-Array mit erstem Byte != 0
	 */
	public static byte[] bigIntegerToBytes(final BigInteger bigInteger) {
		byte[] bytes = bigInteger.toByteArray();
		if(bytes[0] == 0) {
			return Arrays.copyOfRange(bytes, 1, bytes.length);
		}
		return bytes;
	}

	/**
	 * Wandelt einen BigInteger in vorzeichenlose Hex-Darstellung um.
	 * @param bigInteger BigInteger
	 * @return Hex-Zeichenfolge
	 */
	public static String bigIntegerToHex(final BigInteger bigInteger) {
		return bytesToHex(bigIntegerToBytes(bigInteger));
	}

	/**
	 * Wandelt eine vorzeichenlose Hex-Zeichenfolge in einen positiven BigInteger
	 * @param hexStr Hex-Zeichenfolge
	 * @return BigInteger
	 */
	public static BigInteger bigIntegerFromHex(final String hexStr) {
		return bigIntegerFromBytes(bytesFromHex(hexStr));
	}

	/**
	 * Wandelt ein byte-Array in ein char-Array um.
	 * @param bytes bytes (UTF-8-kodiert)
	 * @return char[]-Array
	 */
	public static char[] bytesToChars(final byte[] bytes) {
		ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
		CharBuffer charBuffer = StandardCharsets.UTF_8.decode(byteBuffer);

		int len = charBuffer.limit();
		char[] result = new char[len];
		charBuffer.get(result, 0, len);

		return result;
	}

	/**
	 * Wandelt ein char-Array in ein byte-Array um.
	 * @param chars char[]-Array
	 * @return bytes (UTF-8-kodiert) 
	 */
	public static byte[] charsToBytes(final char[] chars) {
		CharBuffer charBuffer = CharBuffer.wrap(chars);
		ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(charBuffer);

		int len = byteBuffer.limit();
		byte[] result = new byte[len];
		byteBuffer.get(result, 0, len);

		return result;
	}
}
