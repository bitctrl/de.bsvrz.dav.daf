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

package de.bsvrz.dav.daf.communication.lowLevel.telegrams;

import de.bsvrz.dav.daf.communication.srpAuthentication.SrpCryptoParameter;
import de.bsvrz.dav.daf.main.impl.CommunicationConstant;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Zweites Telegramm der SRP-Authentifizierung, wird vom Server zum Client geschickt.
 * 
 * Sendet die SRP-Werte B und s sowie die kryptographischen Parameter an den Client
 * 
 * Kann SRP nicht benutzt werden, enthält das Telegramm einen B-Wert von 0 (was gemäß SRP kein gültiger Wert ist)
 * und einen Fehlermeldungstext als String-Wert
 *
 * @author Kappich Systemberatung
 */
public class SrpAnswer extends DataTelegram{

	/**
	 * Der öffentliche Server-Wert B
	 */
	private BigInteger _b = null;

	/**
	 * Der Salt-Wert am Benutzernamen des Clients
	 */
	private BigInteger _s = null;

	/**
	 * Crypto-Parameter, die der Server dem Client vorgibt
	 */
	private SrpCryptoParameter _cryptoParams = null;

	/**
	 * String-Darstellung der _cryptoParams oder String für negative Quittung
	 */
	private String _cryptoParamsString = null;


	/**
	 * Erstellt eine neue nicht-initialisierte Instanz (zur Initialisierung über {@link #read(DataInputStream)}).
	 */
	public SrpAnswer() {
		type = SRP_ANSWER_TYPE;
		priority = CommunicationConstant.SYSTEM_TELEGRAM_PRIORITY;
	}

	/**
	 * Erstellt eine neue Instanz mit vordefinierten Werten
	 * @param b Der öffentliche Server-Wert B
	 * @param s Der Salt-Wert am Benutzernamen des Clients
	 * @param cryptoParameter Crypto-Parameter, die der Server dem Client vorgibt
	 */
	public SrpAnswer(final BigInteger b, final BigInteger s, final SrpCryptoParameter cryptoParameter) {
		type = SRP_ANSWER_TYPE;
		priority = CommunicationConstant.SYSTEM_TELEGRAM_PRIORITY;
		_b = b;
		_s = s;
		_cryptoParams = cryptoParameter;
		_cryptoParamsString = cryptoParameter.toString();
		length = 0;
		length += _b.toByteArray().length + 2;
		length += _s.toByteArray().length + 2;
		length += _cryptoParamsString.getBytes(StandardCharsets.UTF_8).length + 2;
	}

	/**
	 * Erstellt eine negative SRP-Antwort
	 * @param errorMessage Fehlermeldung
	 */
	public SrpAnswer(final String errorMessage) {
		type = SRP_ANSWER_TYPE;
		priority = CommunicationConstant.SYSTEM_TELEGRAM_PRIORITY;
		_b = BigInteger.ZERO;
		_s = BigInteger.ZERO;
		_cryptoParams = null;
		_cryptoParamsString = errorMessage;
		length = 0;
		length += _b.toByteArray().length + 2;
		length += _s.toByteArray().length + 2;
		length += _cryptoParamsString.getBytes(StandardCharsets.UTF_8).length + 2;
	}

	/**
	 * Gibt den öffentlichen Serverwert B zurück
	 * @return B
	 */
	public BigInteger getB() {
		return _b;
	}

	/**
	 * Gibt das dem Client zugeordnete Salt s zurück
	 * @return s
	 */
	public BigInteger getS() {
		return _s;
	}

	/**
	 * Gibt die Crypto-Parameter zurück, die der Client (u.a.) zur Erzeugung des Verifiers benutzen soll
	 * @return SrpCryptoParameter oder null falls SRP nicht benutzt werden kann
	 */
	public SrpCryptoParameter getCryptoParams() {
		return _cryptoParams;
	}

	/**
	 * Gibt zurück, ob SRP benutzt werden kann. 
	 * @return true falls SRp benutzt werden kann, somst false
	 */
	public boolean isValid(){
		// Der Server überträgt s == 0 und b == 0 falls SRP von der Konfiguration nicht unterstützt wird
		return !Objects.equals(_b, BigInteger.ZERO);
	}

	@Override
	public void read(final DataInputStream in) throws IOException {
		int _length = in.readShort();
		_b = readBigInteger(in);
		_s = readBigInteger(in);
		_cryptoParamsString = in.readUTF();
		length = 0;
		length += _b.toByteArray().length + 2;
		length += _s.toByteArray().length + 2;
		length += _cryptoParamsString.getBytes(StandardCharsets.UTF_8).length + 2;
		if(isValid()) {
			try {
				_cryptoParams = new SrpCryptoParameter(_cryptoParamsString);
			}
			catch(IllegalArgumentException e) {
				throw new IOException("Ungültige kryptographische Parameter", e);
			}
		}
		if(length != _length) {
			throw new IOException("Falsche Telegrammlänge");
		}
	}

	@Override
	public void write(final DataOutputStream out) throws IOException {
		out.writeShort(length);
		writeBigInteger(out, _b);	
		writeBigInteger(out, _s);	
		out.writeUTF(_cryptoParamsString);
	}

	private static void writeBigInteger(final DataOutputStream out, final BigInteger bigInteger) throws IOException {
		byte[] b_bytes = bigInteger.toByteArray();
		out.writeShort(b_bytes.length);
		out.write(b_bytes);
	}

	private static BigInteger readBigInteger(final DataInputStream in) throws IOException {
		short length = in.readShort();
		byte[] tmp = new byte[length];
		in.readFully(tmp);
		return new BigInteger(tmp);
	}

	public String getErrorMessage() {
		if(isValid()){
			return "";
		}
		return _cryptoParamsString;
	}

	@Override
	public String parseToString() {
		String str = "Systemtelegramm SRP Antwort: \n";
		if(isValid()) {
			str += "Server-Wert B       : " + _b + "\n";
			str += "Passwort-Salz s     : " + _s + "\n";
			str += "Crypto-Parameter    : " + _cryptoParamsString + "\n";
		}
		else {
			str += "Fehlermeldung       : " + _cryptoParamsString + "\n";
		}
		return str;	
	}
}
