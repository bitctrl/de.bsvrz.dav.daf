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
 * Viertes und letztes Telegramm der SRP-Authentifizierung.
 *
 * Der Server sendet den Wert M2 an den Client. Dadurch kann der Client überprüfen, dass der Server das Passwort akzeptiert hat.
 * Sendet der Server ein falsches M2, dann haben sich beide Kommunikationspartner auf einen unterschiedlichen Schlüssel geeinigt
 * (z.B. weil jemand die Verbindung manipuliert hat) und die Verbindung muss terminiert werden, da keine Nachrichten mehr
 * ausgetauscht werden können.
 *
 * @author Kappich Systemberatung
 */
public class SrpValidateAnswer extends DataTelegram{

	/**
	 * Der Serverseitig generierte Nachweis M2
	 */
	private BigInteger _m2;

	/**
	 * Erstellt eine neue nicht-initialisierte Instanz (zur Initialisierung über {@link #read(DataInputStream)}).
	 */
	public SrpValidateAnswer() {
		type = SRP_VALDIATE_ANSWER_TYPE;
		priority = CommunicationConstant.SYSTEM_TELEGRAM_PRIORITY;
	}

	/**
	 * Erstellt eine neue Instanz mit vordefiniertem Wert.
	 * @param m2 Server-nachweis M2
	 */
	public SrpValidateAnswer(final BigInteger m2) {
		type = SRP_VALDIATE_ANSWER_TYPE;
		priority = CommunicationConstant.SYSTEM_TELEGRAM_PRIORITY;
		_m2 = m2;
		length = 0;
		length += _m2.toByteArray().length + 2;
	}

	/**
	 * Gibt den Wert M2 zurück
	 * @return M2
	 */
	public BigInteger getM2() {
		return _m2;
	}

	@Override
	public void read(final DataInputStream in) throws IOException {
		int _length = in.readShort();
		_m2 = readBigInteger(in);
		length = 0;
		length += _m2.toByteArray().length + 2;
		if(length != _length) {
			throw new IOException("Falsche Telegrammlänge");
		}
	}

	@Override
	public void write(final DataOutputStream out) throws IOException {
		out.writeShort(length);
		writeBigInteger(out, _m2);
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

	@Override
	public String parseToString() {
		String str = "Systemtelegramm SRP Bestätigung Antwort: \n";
		str += "Server-Nachweis M2  : " + _m2 + "\n";
		return str;
	}
	
}
