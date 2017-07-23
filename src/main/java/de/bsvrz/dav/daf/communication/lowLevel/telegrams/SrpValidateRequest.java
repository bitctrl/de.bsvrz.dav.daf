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

import de.bsvrz.dav.daf.main.impl.CommunicationConstant;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;

/**
 * Drittes Telegramm der SRP-Authentifizierung.
 * 
 * Der Client sendet die Werte A und M1 an den Server. Mit diesen Werten kann der Server überprüfen, ob der Client das
 * richtige Passwort verwendet hat.
 *
 * @author Kappich Systemberatung
 */
public class SrpValidateRequest extends DataTelegram{

	/**
	 * Der öffentliche Client-Wert A
	 */
	private BigInteger _a;

	/**
	 * Der Clientseitig generierte Nachweis M1
	 */
	private BigInteger _m1;

	/**
	 * Erstellt eine neue nicht-initialisierte Instanz (zur Initialisierung über {@link #read(DataInputStream)}).
	 */
	public SrpValidateRequest() {
		type = SRP_VALDIATE_REQUEST_TYPE;
		priority = CommunicationConstant.SYSTEM_TELEGRAM_PRIORITY;
	}

	/**
	 * Erstellt eine neue Instanz mit vordefineirten Werten
	 * @param a Öffentlicher Client-Wert A
	 * @param m1 Client-Nachweis M1
	 */
	public SrpValidateRequest(final BigInteger a, final BigInteger m1) {
		type = SRP_VALDIATE_REQUEST_TYPE;
		priority = CommunicationConstant.SYSTEM_TELEGRAM_PRIORITY;
		_a = a;
		_m1 = m1;
		length = 0;
		length += _a.toByteArray().length + 2;
		length += _m1.toByteArray().length + 2;
	}

	/**
	 * Gibt dem Client-Wert A zurück
	 * @return A
	 */
	public BigInteger getA() {
		return _a;
	}

	/**
	 * Gibt dem Client-Nachweis M1 zurück
	 * @return M1
	 */
	public BigInteger getM1() {
		return _m1;
	}

	@Override
	public void read(DataInputStream in) throws IOException {
		int telegramLength = in.readShort();
		in = DataTelegrams.getTelegramStream(in, telegramLength);
		_a = DataTelegrams.checkAndReadBigInteger(in);
		_m1 = DataTelegrams.checkAndReadBigInteger(in);
		if(in.available() != 0) throw new IOException("Falsche Telegrammlänge (überflüssige Bytes)");
		length=telegramLength;
	}

	@Override
	public void write(final DataOutputStream out) throws IOException {
		out.writeShort(length);
		writeBigInteger(out, _a);
		writeBigInteger(out, _m1);
	}

	private static void writeBigInteger(final DataOutputStream out, final BigInteger bigInteger) throws IOException {
		byte[] b_bytes = bigInteger.toByteArray();
		out.writeShort(b_bytes.length);
		out.write(b_bytes);
	}

	@Override
	public String parseToString() {
		String str = "Systemtelegramm SRP Bestätigung Anfrage: \n";
		str += "Client-Wert A       : " + _a + "\n";
		str += "Client-Nachweis M1  : " + _m1 + "\n";
		return str;
	}
}
