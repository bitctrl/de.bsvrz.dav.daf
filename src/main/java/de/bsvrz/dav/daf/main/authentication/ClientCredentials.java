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

package de.bsvrz.dav.daf.main.authentication;

import de.bsvrz.dav.daf.communication.srpAuthentication.SrpUtilities;

import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Clientseitige Zugangsdaten zum Login beim Datenverteiler oder ähnlich.
 * <p>
 * Die Zugangsdaten bestehen entweder aus Benutzername und Passwort oder aus einem Login-Token, welcher ein binärer Code ist, bit dem sich der Client beim
 * Server ausweisen kann ohne das Passwort im Klartext zu kennen. So ein Token kann beispielsweise das bei SRP6 verwendete x sein, mit dem der Client sich beim
 * Server ausweisen kann, ohne dass das Klartextpasswort gespeichert oder eingegeben werden muss.
 *
 * @author Kappich Systemberatung
 */
public abstract class ClientCredentials {

	private static final Pattern SRP_PARSE_PATTERN = Pattern.compile("^(SRP6)~~~~ (\\p{XDigit}+)$");

	ClientCredentials() {
	}

	/**
	 * Gibt das Passwort zurück
	 *
	 * @return das Passwort
	 * @throws UnsupportedOperationException falls es sich um einen Token-Login handelt ({@link #hasPassword()} gibt false zurück)
	 */
	public abstract char[] getPassword();

	/**
	 * Gibt den binären Schlüssel zurück
	 *
	 * @return den Schlüssel
	 * @throws UnsupportedOperationException falls es sich um einen Passwort-Login handelt ({@link #hasPassword()} gibt true zurück)
	 */
	public abstract byte[] getTokenData();

	/**
	 * Gibt den Typ des binären Schlüssels zurück, beispielsweise "SRP6"
	 *
	 * @return den Schlüssel
	 * @throws UnsupportedOperationException falls es sich um einen Passwort-Login handelt ({@link #hasPassword()} gibt true zurück)
	 */
	public abstract String getTokenType();

	/**
	 * Gibt <tt>true</tt> zurück, wenn es sich um einen Passwort-Login handelt
	 *
	 * @return <tt>true</tt>, wenn es sich um einen Passwort-Login handelt, sonst (bei einem Token-Login) <tt>false</tt>
	 */
	public abstract boolean hasPassword();

	/**
	 * Erstellt eine Instanz, die ein Passwort speichert
	 *
	 * @param password Passwort
	 * @return Instanz oder null falls das angegebene Passwort null ist oder eine Länge von 0 hat.
	 */
	public static ClientCredentials ofPassword(char[] password) {
		if(password == null || password.length == 0) return null;
		return new PasswordClientCredentials(password);
	}

	/**
	 * Erstellt eine Instanz, die ein Login-Token darstellt. Ein Token ist ein (binärer) Code mit dem sich der Client statt einem Passwort authentifizieren
	 * kann
	 *
	 * @param tokenData Binäre Daten    (!= null)
	 * @param tokenType Art des Tokens (z.B. SRP6 für ein SRP-"x", != null)
	 * @return Login-Token
	 */
	public static ClientCredentials ofToken(byte[] tokenData, String tokenType) {
		Objects.requireNonNull(tokenData, "tokenData == null");
		Objects.requireNonNull(tokenType, "tokenType == null");
		return new TokenClientCredentials(tokenData, tokenType);
	}

	/**
	 * Erstellt eine Instanz aus einem serialisierten String (kompatibel mit {@link #toString()}
	 *
	 * @param s Passwort als String oder als String serialisierter Token (wie in Authentifizierungsdatei), z. B. "geheim" oder "SRP6~~~~ abcde"
	 *             
	 * @return Instanz oder null falls das Passwort null ist oder eine Länge von 0 hat.
	 */
	public static ClientCredentials ofString(String s) {
		if(s == null || s.isEmpty()) return null;
		Matcher matcher = SRP_PARSE_PATTERN.matcher(s);
		if(matcher.matches()) {
			return ofToken(SrpUtilities.bytesFromHex(matcher.group(2)), matcher.group(1));
		}
		else {
			return ofPassword(s.toCharArray());
		}
	}

	private static class PasswordClientCredentials extends ClientCredentials {

		private final char[] _password;

		public PasswordClientCredentials(final char[] password) {
			_password = password;
		}

		@Override
		public char[] getPassword() {
			return _password;
		}

		@Override
		public byte[] getTokenData() {
			throw new UnsupportedOperationException("Es wird ein Passwort verwendet, daher ist kein Zugriff auf das Login-Token möglich.");
		}

		@Override
		public String getTokenType() {
			throw new UnsupportedOperationException("Es wird ein Passwort verwendet, daher ist kein Zugriff auf das Login-Token möglich.");
		}

		@Override
		public boolean hasPassword() {
			return true;
		}

		@Override
		public String toString() {
			return new String(_password);
		}

		@Override
		public boolean equals(final Object o) {
			if(this == o) return true;
			if(o == null || getClass() != o.getClass()) return false;

			final PasswordClientCredentials that = (PasswordClientCredentials) o;

			return Arrays.equals(_password, that._password);

		}

		@Override
		public int hashCode() {
			return Arrays.hashCode(_password);
		}
	}

	private static class TokenClientCredentials extends ClientCredentials {
		private final byte[] _tokenData;
		private final String _tokenType;

		public TokenClientCredentials(final byte[] tokenData, final String tokenType) {
			_tokenData = tokenData;
			_tokenType = tokenType;
		}

		@Override
		public char[] getPassword() {
			throw new UnsupportedOperationException("Es wird ein Login-Token verwendet, daher ist kein Zugriff auf das Passwort möglich.");
		}

		@Override
		public byte[] getTokenData() {
			return _tokenData;
		}

		@Override
		public String getTokenType() {
			return _tokenType;
		}

		@Override
		public boolean hasPassword() {
			return false;
		}

		@Override
		public String toString() {
			return _tokenType + "~~~~ " + SrpUtilities.bytesToHex(_tokenData);
		}

		@Override
		public boolean equals(final Object o) {
			if(this == o) return true;
			if(o == null || getClass() != o.getClass()) return false;

			final TokenClientCredentials that = (TokenClientCredentials) o;

			if(!Arrays.equals(_tokenData, that._tokenData)) return false;
			return _tokenType.equals(that._tokenType);

		}

		@Override
		public int hashCode() {
			int result = Arrays.hashCode(_tokenData);
			result = 31 * result + _tokenType.hashCode();
			return result;
		}
	}

	@Override
	public abstract String toString();
}
