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

import com.nimbusds.srp6.*;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.SrpAnswer;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.SrpRequest;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.SrpValidateAnswer;
import de.bsvrz.dav.daf.communication.lowLevel.telegrams.SrpValidateRequest;
import de.bsvrz.dav.daf.main.CommunicationError;
import de.bsvrz.dav.daf.main.InconsistentLoginException;
import de.bsvrz.dav.daf.main.authentication.ClientCredentials;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Objects;

/**
 * Wrapper-Klasse, die die Nimbus-SRP-Implementierung clientseitig kapselt
 * 
 * @author Kappich Systemberatung
 */
public class SrpClientAuthentication {

	private SrpClientAuthentication() {
	}

	/**
	 * SRP-Authentifizierung auf Client-Seite
	 *
	 * @param userName          Benutzername
	 * @param clientCredentials Passwort oder Login-Schlüssel (SRP-"x")
	 * @param telegramInterface Klasse/Interface zum Empfangen von Telegrammen über diese Verbindung
	 * @return Sitzungsschlüssel falls erfolgreich
	 * @throws CommunicationError Server antwortet nicht
	 * @throws InconsistentLoginException Fehlerhafte Authentifikationsdaten (Passwort wahrscheinlich falsch)
	 * @throws SrpNotSupportedException Server/Konfiguration unterstützt kein SRP
	 */
	public static AuthenticationResult authenticate(String userName, int passwordIndex, final ClientCredentials clientCredentials, final TelegramInterface telegramInterface) throws CommunicationError, InconsistentLoginException, SrpNotSupportedException {
		try {
			if(!clientCredentials.hasPassword() && !clientCredentials.getTokenType().equals("SRP6")) {
				throw new InconsistentLoginException("Falscher Login-Token-Typ: " + clientCredentials.getTokenType());
			}
			
			// Nimbus-SRP-Sitzung erzeugen
			final SRP6ClientSession srp6ClientSession = new SRP6ClientSession();

			// SRP Schritt 1
			srp6ClientSession.step1(userName, "");
			SrpRequest telegram = new SrpRequest(userName, passwordIndex);
			final SrpAnswer srpAnswer = telegramInterface.sendAndReceiveRequest(telegram);
			
			if(!srpAnswer.isValid()){
				throw new SrpNotSupportedException(srpAnswer.getErrorMessage());
			}

			// SRP Schritt 2
			SrpCryptoParameter cryptoParams = srpAnswer.getCryptoParams();
			if(clientCredentials.hasPassword()) {
				srp6ClientSession.setXRoutine(new PasswordXRoutine(cryptoParams, clientCredentials.getPassword()));
			}
			else {
				srp6ClientSession.setXRoutine(new RawXRoutine(SrpUtilities.bigIntegerFromBytes(clientCredentials.getTokenData())));
			}
			final SRP6CryptoParams srp6CryptoParams = getNimbusCryptoParams(cryptoParams);
			final SRP6ClientCredentials srp6ClientCredentials = srp6ClientSession.step2(srp6CryptoParams, srpAnswer.getS(), srpAnswer.getB());
			SrpValidateRequest validateRequest = new SrpValidateRequest(srp6ClientCredentials.A, srp6ClientCredentials.M1);
			final SrpValidateAnswer srpValidateAnswer = telegramInterface.sendAndReceiveValidateRequest(validateRequest);

			// SRP Schritt 3
			srp6ClientSession.step3(srpValidateAnswer.getM2());
			return new AuthenticationResult(srp6ClientSession.getSessionKey(false), cryptoParams);
		}
		catch(SRP6Exception e){
			String reason = "Unbekannter Fehler";
			switch(e.getCauseType()){
				case BAD_PUBLIC_VALUE:
					reason = "Der Server verwendet unsichere Parameter";
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
	 * Gibt eine Implementierugn der Nimbus-SRP6CryptoParams zurück
	 * @param cryptoParams Eigene Implementierugn der KRyptographischen Parameter
	 * @return SRP6CryptoParams
	 */
	private static SRP6CryptoParams getNimbusCryptoParams(final SrpCryptoParameter cryptoParams) {
		return SRP6CryptoParams.getInstance(cryptoParams.getSrpPrimeBits(), cryptoParams.getHashFunction());
	}

	/**
	 * Erstellt einen SRP-Überprüfungscode mit einem zufälligen Salt. Diese Methode eignet sich um clientseitig ein SRP-Überprüfungscode zu erstellen und um diesen dann zusammen mit dem
	 * Salt an den Server zu senden, ohne dass dieser das Passwort jemals sehen kann.
	 * @param cryptoParams Cryptoparameter
	 * @param user Benutzer
	 * @param clientCredentials Passwort oder Login-Token
	 * @return Überprüfungscode
	 */
	public static SrpVerifierData createVerifier(final SrpCryptoParameter cryptoParams, final String user, final ClientCredentials clientCredentials) {
		final byte[] saltByteArray = SrpUtilities.generateRandomSalt(cryptoParams);
		return createVerifier(cryptoParams, user, clientCredentials, saltByteArray);
	}

	/**
	 * Erstellt einen SRP-Überprüfungscode mit einem festen Salt. Diese Methode macht dort Sinn, wo das Salt reproduzierbar identisch sein muss, z.B. wenn die
	 * Konfiguration einen Überprüfungscode für nicht vorhandene Benutzer "fälscht" (damit man nicht prüfen kann ob ein Benutzer exisitert oder nicht). Wäre das
	 * Salt zufällig könnte man durch mehrmalige Einlog-Versuche feststellen, dass sich das Salt ändert, was auf einen "gefälschten" Benutzer hindeuten würde.
	 *
	 * @param cryptoParams Cryptoparameter
	 * @param user         Benutzer
	 * @param clientCredentials     Passwort oder Login-Token
	 * @param saltBytes    Salt
	 * @return Überprüfungscode
	 */
	public static SrpVerifierData createVerifier(final SrpCryptoParameter cryptoParams, final String user, final ClientCredentials clientCredentials, final byte[] saltBytes) {
		BigInteger salt;
		salt = SrpUtilities.bigIntegerFromBytes(saltBytes);

		BigInteger v = calculateVerifier(cryptoParams, user, clientCredentials, salt);
		return new SrpVerifierData(v, salt, cryptoParams);
	}

	/**
	 * Erstellt einen SRP-Überprüfungscode mit dem angegebenem Salt.
	 * @param cryptoParams Cryptoparameter
	 * @param user         Benutzer
	 * @param clientCredentials     Passwort oder Login-Token
	 * @param salt    Salt
	 * @return Überprüfungscode
	 */
	private static BigInteger calculateVerifier(final SrpCryptoParameter cryptoParams, final String user, final ClientCredentials clientCredentials, final BigInteger salt) {
		final SRP6VerifierGenerator srp6VerifierGenerator = new SRP6VerifierGenerator(getNimbusCryptoParams(cryptoParams));
		if(clientCredentials.hasPassword()) {
			// XRoutine setzen um das X aus dem Passwort zu berechnen
			srp6VerifierGenerator.setXRoutine(new PasswordXRoutine(cryptoParams, clientCredentials.getPassword()));
		}
		else {
			// X aus dem ClientCredentials-Token auslesen
			if(!clientCredentials.getTokenType().equals("SRP6")) {
				throw new IllegalArgumentException("Falscher Token-Typ: " + clientCredentials.getTokenType());
			}
			srp6VerifierGenerator.setXRoutine(new RawXRoutine(SrpUtilities.bigIntegerFromBytes(clientCredentials.getTokenData())));
		}
		return srp6VerifierGenerator.generateVerifier(salt, user, "");
	}

	/**
	 * Erstellt ein Login-Token, welcher ein bin&auml;rer Code ist, bit dem sich der Client beim Server ausweisen kann ohne das Passwort im Klartext zu kennen.
	 * Dieses Token entspricht dem SRP-"x". Es kann in der passwd-Datei gespeichert werden.
	 * @param srpVerifierData Bestehender &Uuml;berpr&uuml;fungscode. Wird beim setzen des Passworts mit {@link #createVerifier(SrpCryptoParameter, String, ClientCredentials)} erzeugt und kann auch sp&auml;ter mit {@link de.bsvrz.dav.daf.main.config.management.UserAdministration#getSrpVerifier(String, String, String, int)} abgefragt werden.
	 * @param user            Benutzername
	 * @param password Zugeh&ouml;riges Passwort
	 * @return Anmeldedaten ({@link ClientCredentials#hasPassword()} liefert false, da es sich um einen Login-Token handelt). Kann mit toString in eine Hex-Darstellung umgewandelt/serialisiert werden.
	 * Die einzelnen Bytes sind mit {@link ClientCredentials#getTokenData()} abrufbar.
	 * 
	 * @throws InconsistentLoginException Falls die angegebenen Daten (Benutzername/Passwort) nicht zum Verifier passen
	 */
	public static ClientCredentials createLoginToken(SrpVerifierData srpVerifierData, final String user, final char[] password) throws InconsistentLoginException {

		// Erst mal überprüfen, ob die angegebenen Daten überhaupt gültig sind
		if(!validateVerifier(srpVerifierData, user, ClientCredentials.ofPassword(password))){
			throw new InconsistentLoginException("Die Authentifikationsdaten sind fehlerhaft");
		}
		
		/* Code muss das gleiche Ergebnis liefern, wie die Routine in SRP6ClientSession#step2 */
		PasswordXRoutine passwordXRoutine = new PasswordXRoutine(srpVerifierData.getSrpCryptoParameter(), password);
		SRP6CryptoParams cryptoParams = getNimbusCryptoParams(srpVerifierData.getSrpCryptoParameter());
		BigInteger xInteger = passwordXRoutine.computeX(
				cryptoParams.getMessageDigestInstance(),
				SrpUtilities.bigIntegerToBytes(srpVerifierData.getSalt()),
				user.getBytes(Charset.forName("UTF-8")),
				new byte[0]
		);

		return ClientCredentials.ofToken(SrpUtilities.bigIntegerToBytes(xInteger), "SRP6");
		
	}

	/**
	 * Erstellt ein zufälligen Login-Token, dem also kein (bekanntes) Klartext-Passwort zugrunde liegt.
	 * @param cryptoParams Kryptographische Parameter
	 * @return Ein zufälliger Login-Token für SRP6
	 */
	public static ClientCredentials createRandomToken(final SrpCryptoParameter cryptoParams) {
		int digestLength = SrpUtilities.getHashLength(cryptoParams);
		byte[] randomBytes = SRP6Routines.generateRandomSalt(digestLength);
		return ClientCredentials.ofToken(randomBytes, "SRP6");
	}

	/**
	 * Überprüft, ob ein Verifier zu einem Benutzernamen und Passwort passt. Dies sollte nur clientseitig bzw. für Testzwecke benutzt werden, da der Server das
	 * Passwort gar nicht kennen darf.
	 * @param verifier    Bestehender Verifier
	 * @param user        Benutzername
	 * @param clientCredentials    Passwort oder Login-Token
	 * @return true: Verifier passt, false: sonst
	 */
	public static boolean validateVerifier(final SrpVerifierData verifier, final String user, final ClientCredentials clientCredentials) {
		SrpCryptoParameter cryptoParams = verifier.getSrpCryptoParameter();
		BigInteger v = calculateVerifier(cryptoParams, user, clientCredentials, verifier.getSalt());
		return Objects.equals(v, verifier.getVerifier());
	}

	/**
	 * Interface mit dem ein Anwender der {@link SrpClientAuthentication}-Klasse Telegramme sendet und empfängt
	 */
	public interface TelegramInterface {
		/**
		 * Eine SRP-Anfrage senden und die Antwort empfangen
		 * @param telegram Anfrage
		 * @return Antwort
		 * @throws CommunicationError Kommunikationsproblem
		 * @throws InconsistentLoginException Diese Exception kann die implementierende Klasse werfen, wenn aus irgendwelchen Gründen festgestellt wurde, das der Login falsch
		 * ist (z.B. negative Quittung statt Antwort erhalten). Eigentlich macht das aber die Klasse {@link SrpClientAuthentication} selbst.
		 */
		SrpAnswer sendAndReceiveRequest(final SrpRequest telegram) throws CommunicationError, InconsistentLoginException, SrpNotSupportedException;

		/**
		 * Eine SRP-Überprüfungs-Anfrage senden und die Antwort empfangen
		 * @param telegram Anfrage
		 * @return Antwort
		 * @throws CommunicationError Kommunikationsproblem
		 * @throws InconsistentLoginException Diese Exception kann die implementierende Klasse werfen, wenn aus irgendwelchen Gründen festgestellt wurde, das der Login falsch
		 * ist (z.B. negative Quittung statt Antwort erhalten). Eigentlich macht das aber die Klasse {@link SrpClientAuthentication} selbst.
		 */
		SrpValidateAnswer sendAndReceiveValidateRequest(final SrpValidateRequest telegram) throws CommunicationError, InconsistentLoginException;
	}

	/**
	 * Eine Nimbus-SRP-{@link XRoutine}, die den Passwortschlüssel x aus dem Passwort mit den spezifizierten kryptographischen Parametern berechnet.
	 * 
	 * Es handelt sich um eine erweiterte Implementierung von {@link com.nimbusds.srp6.XRoutineWithUserIdentity}, die das Passwort zusätzlich mit einer
	 * Schlüsselableitungsfunktion wie PBKDF2 hasht, um Brute-Force-Angriffe auf den Verifier zu verlangsamen.
	 * 
	 * Das Passwort wird im Konstruktor übergeben um die Limitierung der SRP-Bibliothek zu umgehen, nur Strings als Passwörter verwenden zu können
	 */
	private static class PasswordXRoutine implements XRoutine {
		
		private final SrpCryptoParameter _srpCryptoParameter;
		private final char[] _password;

		/** 
		 * Erstellt eine neue PasswordXRoutine mit den angegebenen Parametern
		 * @param srpCryptoParameter Kryptographische Parameter
		 * @param password Passwort
		 */
		public PasswordXRoutine(final SrpCryptoParameter srpCryptoParameter, final char[] password) {
			_srpCryptoParameter = srpCryptoParameter;
			_password = password;
		}

		@Override
		public BigInteger computeX(final MessageDigest digest, final byte[] salt, final byte[] username, final byte[] notUsedPassword) {
			// Berechnet H(s | H(u | ":" | KDF(p, s)))
			SecretKey key;
			try {
				SecretKeyFactory factory = SecretKeyFactory.getInstance(_srpCryptoParameter.getKeyDerivationFunction());
				PBEKeySpec keySpec = new PBEKeySpec(_password, salt, _srpCryptoParameter.getKeyDerivationIterations(), _srpCryptoParameter.getKeyDerivationHashBits());
				key = factory.generateSecret(keySpec);
			}
			catch(InvalidKeySpecException | NoSuchAlgorithmException e) {
				throw new UnsupportedOperationException(e);
			}

			digest.update(username);
			digest.update((byte) ':');
			digest.update(key.getEncoded());

			byte[] output = digest.digest();

			digest.update(salt);
			output = digest.digest(output);

			return SrpUtilities.bigIntegerFromBytes(output);
		}
	}
	
	/**
	 * Eine Nimbus-SRP-{@link XRoutine}, die den Passwortschlüssel x so zurückgibt, wie er der Klasse übergeben wurde. Diese Klasse kann benutzt werden, wenn beispielsweise
	 * das fertig berechnete x als Login-Token o.ä. aus der passwd-Datei eingelesen wurde.
	 * 
	 * Das x wird im Konstruktor übergeben um die Limitierung der SRP-Bibliothek zu umgehen, nur Strings als Passwörter verwenden zu können
	 */
	private static class RawXRoutine implements XRoutine {

		private final BigInteger _x;

		/** 
		 * Erstellt eine neue RawXRoutine
		 * @param x Fester "x"-Wert (Login-Token)
		 */
		public RawXRoutine(BigInteger x) {
			_x = x;
		}

		@Override
		public BigInteger computeX(final MessageDigest digest, final byte[] salt, final byte[] username, final byte[] notUsedPassword) {
			return _x;
		}
	}

	/**
	 * Ergebnis einer ARP-Authentifizierung
	 */
	public static class AuthenticationResult {
		private final BigInteger _sessionKey;
		private final SrpCryptoParameter _cryptoParams;

		private AuthenticationResult(final BigInteger sessionKey, final SrpCryptoParameter cryptoParams) {
			_sessionKey = sessionKey;
			_cryptoParams = cryptoParams;
		}

		/** 
		 * Gibt den Sitzungsschlüssel zurück
		 * @return den Sitzungsschlüssel
		 */
		public BigInteger getSessionKey() {
			return _sessionKey;
		}

		/** 
		 * Gibt die kryptografischen Parameter zurück
		 * @return die kryptografischen Parameter
		 */
		public SrpCryptoParameter getCryptoParams() {
			return _cryptoParams;
		}

		@Override
		public String toString() {
			return "AuthenticationResult{" +
					"_sessionKey=" + _sessionKey +
					", _cryptoParams=" + _cryptoParams +
					'}';
		}
	}
}
