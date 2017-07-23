/*
 * Copyright 2017 by Kappich Systemberatung Aachen
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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * TBD rs: Dokumentieren.
 *
 * @author Kappich Systemberatung, Aachen
 * @version $Revision: 0000 $
 */
public class DataTelegrams {

	private static final Charset Utf8Charset = Charset.forName("UTF-8");

	public static DataInputStream getTelegramStream(final DataInputStream in, final int telegramLength) throws IOException {
		byte[] buf = new byte[telegramLength];
		in.readFully(buf);
		return new DataInputStream(new ByteArrayInputStream(buf));
	}

	public static String checkAndReadUTF(final DataInputStream in) throws IOException {
//		return in.readUTF();
		int readLength = in.readUnsignedShort();
		if(in.available()<readLength) throw new IOException("Falsche Telegrammlänge (UTF-String passt nicht ins Telegramm)");
		final byte[] utfBytes = new byte[readLength];
		in.readFully(utfBytes);
		return Utf8Charset.decode(ByteBuffer.wrap(utfBytes)).toString();
	}

	public static BigInteger checkAndReadBigInteger(final DataInputStream in) throws IOException {
		short readLength = in.readShort();
		if(in.available()<readLength) throw new IOException("Falsche Telegrammlänge (BigInteger passt nicht ins Telegramm)");
		byte[] tmp = new byte[readLength];
		in.readFully(tmp);
		return new BigInteger(tmp);
	}
}
