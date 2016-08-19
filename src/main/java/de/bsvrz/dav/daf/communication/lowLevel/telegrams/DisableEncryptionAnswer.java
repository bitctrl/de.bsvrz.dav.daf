/*
 * Copyright 2015 by Kappich Systemberatung Aachen
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

/**
 * Mit diesem Telegramm antwortet der Server auf die Anfrage die Verschlüsselung zu deaktivieren. Nach einer positiven Quittung deaktivieren beide Kommunikationspartner die 
 * Verschlüsselung.
 *
 * @author Kappich Systemberatung
 * @version $Revision$
 */
public class DisableEncryptionAnswer extends DataTelegram {

	private boolean _isDisabled;

	public DisableEncryptionAnswer() {
		type = DISABLE_ENCRYPTION_ANSWER_TYPE;
		priority = CommunicationConstant.SYSTEM_TELEGRAM_PRIORITY;
	}

	public DisableEncryptionAnswer(boolean isDisabled) {
		type = DISABLE_ENCRYPTION_ANSWER_TYPE;
		priority = CommunicationConstant.SYSTEM_TELEGRAM_PRIORITY;
		_isDisabled = isDisabled;
		length = 3;
	}

	public boolean isDisabled() {
		return _isDisabled;
	}

	public final String parseToString() {
		String str = "Systemtelegramm Verschlüsselung deaktivieren Antwort: \n";
		str += "Deaktivieren            : " + _isDisabled + "\n";
		return str;
	}

	public final void write(DataOutputStream out) throws IOException {
		out.writeShort(length);
		out.writeBoolean(_isDisabled);
	}

	public final void read(DataInputStream in) throws IOException {
		int _length = in.readShort();
		_isDisabled = in.readBoolean();
		length = 1 + 2;
		if(length != _length) {
			throw new IOException("Falsche Telegrammlänge");
		}
	}
}
