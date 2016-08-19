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

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.*;
import java.util.Objects;

/**
 * Klasse, die die Verschlüsselung von Telegrammen nach einer erfolgreichen SRP-Authentifikation ermöglicht. In Konstruktor der Klasse wird der SRP-Sitzungsschlüssel übergeben (oder zwei Schlüssel bei Dav-Dav-Verbindungen),
 * daraus berechnet diese Klasse je einen AES-Schlüssel für beide Richtungen sowie GCM-Noncen gemäß <a href="https://tools.ietf.org/html/rfc5288#section-3">RFC5288</a>.
 * 
 * Jeder Kommunikationspartner sollte eine Instanz dieser Klasse (pro Verbindung) erzeugen und kann dann mit {@link #encrypt(byte[])} Daten vor dem Versand verschlüsseln und mit
 * {@link #decrypt(byte[])} empfangene Daten entschlüsseln.
 * 
 * Es ist wichtig, dass die Reihenfolge der Telegramme beibehalten wird und kein Telegramm ausgelassen wird, da die GCM-Noncen durch einen einfachen Zähler realisiert werden,
 * der auf beiden Seiten der Verbindung denselben Wert annehmen muss.
 *
 * Diese Klasse ist nur eingeschränkt threadsafe, es dürfen nicht mehrere Verschlüsselungen oder Entschlüsselungen gleichzeitig durchgeführt werden, was allerdings sowieso verboten ist,
 * da es eine fest definierte Telegramm-Reihenfolge geben muss. Es ist kein Problem, parallel eine Verschlüsselung und eine Entschlüsselung durchzuführen.
 *
 * @author Kappich Systemberatung
 */
public class SrpTelegramEncryption {

	/** Länge des GCM authentication tag */
	private final int _authTagBits;

	/** Länge des AES-IV */
	private static final int NONCE_BYTES = 12;
	
	/** Schlüssel zum verschlüsseln (ausgehende Nachrichten) */
	private final SecretKey _encryptionKey;
	
	/** Nonce/GCM-Zähler zum verschlüsseln (ausgehende Nachrichten) */
	private final byte[] _encryptionNonce;
	
	/** Cipher-Objekt zum verschlüsseln */
	private final Cipher _encryptionCipher;
	
	/** Puffer */
	private final byte[] _encryptTmp = new byte[128];

	/** Schlüssel zum entschlüsseln (eingehende Nachrichten) */
	private final SecretKey _decryptionKey;
	
	/** Nonce/GCM-Zähler zum entschlüsseln (eingehende Nachrichten) */
	private final byte[] _decryptionNonce;
	
	/** Cipher-Objekt zum entschlüsseln */
	private final Cipher _decryptionCipher;
	
	/** Puffer */
	private final byte[] _decryptTmp = new byte[128];

	/** 
	 * Erstellt eine neue SrpTelegramEncryption-Instanz für eine Verbindung. Der AES-Key wird aus dem Sitzungsschlüssel bestimmt.
	 * @param sessionKey Sitzungsschlüssel (z.B. aus SRP-Authentifizierung)
	 * @param isClient Handelt es sich hierbei um den Client?
	 * @param srpCryptoParameter Cryptographische Verschlüsselungsparameter
	 */
	public SrpTelegramEncryption(final byte[] sessionKey, boolean isClient, SrpCryptoParameter srpCryptoParameter) throws SrpNotSupportedException {
		try {
			_authTagBits = srpCryptoParameter.getGcmAuthenticationTagBits();

			_encryptionCipher = Cipher.getInstance("AES_" + srpCryptoParameter.getAesKeyLengthBits() + "/GCM/NoPadding");
			_decryptionCipher = Cipher.getInstance("AES_" + srpCryptoParameter.getAesKeyLengthBits() + "/GCM/NoPadding");

			int keyLen = srpCryptoParameter.getAesKeyLengthBits() / 8;

			final byte[] hash = computeHash(2 * keyLen + 2 * 4, srpCryptoParameter.getHashFunction(), sessionKey);

			int pos = 0;
			SecretKey clientKey = new SecretKeySpec(hash, pos, keyLen, "AES");
			pos += keyLen;
			SecretKey serverKey = new SecretKeySpec(hash, pos, keyLen, "AES");
			pos += keyLen;

			// https://tools.ietf.org/html/rfc5288#section-3
			final byte[] clientNonce = new byte[NONCE_BYTES];
			final byte[] serverNonce = new byte[NONCE_BYTES];
			for(int i = 0; i < 4; i++) {
				clientNonce[i] = hash[pos + i];
				serverNonce[i] = hash[pos + 4 + i];
			}

			// Bei Client und Server jeweils die gegensätzlichen Schlüssel verwenden
			if(isClient) {
				_encryptionKey = clientKey;
				_encryptionNonce = clientNonce;
				_decryptionKey = serverKey;
				_decryptionNonce = serverNonce;
			}
			else {
				_encryptionKey = serverKey;
				_encryptionNonce = serverNonce;
				_decryptionKey = clientKey;
				_decryptionNonce = clientNonce;
			}
		}
		catch(NoSuchPaddingException | NoSuchAlgorithmException | DigestException e) {
			throw new SrpNotSupportedException("Verschlüsselungs-Verfahren wird nicht unterstützt", e);
		}
	}

	/**
	 * Verschlüsselt ein Telegramm zum Versand an die Gegenseite
	 * @param telegram Telegramm
	 * @return Verschlüsselte Daten
	 * @throws IOException Fehler beim Verschlüsseln
	 */
	public byte[] encrypt(byte[] telegram) throws IOException {
		GCMParameterSpec spec = nonce(_encryptionNonce);
		try {
			_encryptionCipher.init(Cipher.ENCRYPT_MODE, _encryptionKey, spec);
		}
		catch(InvalidKeyException | InvalidAlgorithmParameterException e) {
			throw new IOException(e);
		}

		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(telegram.length + _authTagBits / 8);
		ByteArrayInputStream inStream = new ByteArrayInputStream(telegram);
		try(CipherInputStream inputStream = new CipherInputStream(inStream, _encryptionCipher)) {
			while(true) {
				int read = inputStream.read(_encryptTmp);
				if(read == -1) break;
				byteArrayOutputStream.write(_encryptTmp, 0, read);
			}
		}

		return byteArrayOutputStream.toByteArray();
	}

	/**
	 * Entschlüsselt ein Telegramm von der Gegenseite
	 * @param telegram Verschlüsselte Daten
	 * @return Entschlüsseltes Telegramm
	 * @throws IOException Fehler beim Entschlüsseln
	 */
	public byte[] decrypt(byte[] telegram) throws IOException {
		GCMParameterSpec spec = nonce(_decryptionNonce);
		try {
			_decryptionCipher.init(Cipher.DECRYPT_MODE, _decryptionKey, spec);
		}
		catch(InvalidKeyException | InvalidAlgorithmParameterException e) {
			throw new IOException(e);
		}

		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(Math.max(0, telegram.length - _authTagBits / 8));
		ByteArrayInputStream inStream = new ByteArrayInputStream(telegram);
		try(CipherInputStream inputStream = new CipherInputStream(inStream, _decryptionCipher)) {
			while(true) {
				int read = inputStream.read(_decryptTmp);
				if(read == -1) break;
				byteArrayOutputStream.write(_decryptTmp, 0, read);
			}
		}

		return byteArrayOutputStream.toByteArray();
	}

	/**
	 * Gibt die nächste GCM-Nonce zurück. Die bisherige Nonce wird um 1 inkrementiert.
	 * @param bytes bisherige Nonce
	 * @return nächste Nonce
	 */
	private GCMParameterSpec nonce(final byte[] bytes) {
		increment(bytes);
		return new GCMParameterSpec(_authTagBits, bytes);
	}

	/**
	 * Inkrementiert ein Byte-Array wie einen Integer. Der erste Wert den Arrays ist am signifikantesten. 
	 * @param bytes Byte-Array
	 */
	public static void increment(final byte[] bytes) {
		int index = bytes.length-1;
		bytes[index]++;
		while(index > 0 && bytes[index] == 0){
			// Übertrag
			index--;
			bytes[index]++;
		}
	}

	/**
	 * Berechnet einen Hashwert der angegebenen Länge aus den übergebenen Daten
	 * @param outputMinimumSize Mindestlänge in Bytes des Ergebniswerts. Das zurückgegebene Array kann größer sein, aber nicht kleiner.
	 * @param hashAlgorithm Algorithmus zum Hashen
	 * @param data Ein oder mehrere Byte-Arrays die zu hashende Daten enthalten. Die Arrays werden aneinander gehängt. Leere Arrays sind nicht zulässig.
	 * @return Byte-Array mit Hashwerten
	 * @throws NoSuchAlgorithmException Der Algorithmus existiert nicht
	 * @throws DigestException Fehler beim Hashwert berechnen
	 */
	public static byte[] computeHash(final int outputMinimumSize, final String hashAlgorithm, final byte[] data) throws NoSuchAlgorithmException, DigestException {
		Objects.requireNonNull(data, "data == null");
		if(data.length == 0){
			throw new IllegalArgumentException("Leeres Array");
		}
		MessageDigest messageDigest = MessageDigest.getInstance(hashAlgorithm);
		int digestLength = messageDigest.getDigestLength();
		int numHashes = (outputMinimumSize + digestLength - 1) / digestLength;
		byte[] result = new byte[numHashes * digestLength];
		for(int i = 0; i < numHashes; i++){
			messageDigest.update((byte) (i >> 24));
			messageDigest.update((byte) (i >> 16));
			messageDigest.update((byte) (i >> 8));
			messageDigest.update((byte) (i));
			if(data.length == 0){
				throw new IllegalArgumentException("Leeres Array");
			}
			messageDigest.update(data);
			messageDigest.digest(result, i * digestLength, digestLength);
		}
		return result;
	}

	/**
	 * Gibt den Namen der verwendeten Verschlüsselung zurück
	 * @return Name der Verschlüsselung
	 */
	public String getCipherName() {
		return _encryptionCipher.getAlgorithm();
	}
}
