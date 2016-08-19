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

/**
 * Anfrage auf das Abschalten der Verschlüsselung, wird nach der erfolgreichen Authentifizierung (SrpValidateAnswer) vom Client an den Server gesendet
 * 
 * Der Server sendet eine DisableEncryptionAnswer mit einem booean-Wert, der angibt ob der Server einverstanden ist, die Verschlüsselung zu deaktivieren
 *
 * @author Kappich Systemberatung
 */
public class DisableEncryptionRequest extends DataTelegram {

	/**
	 * Erstellt eine neue Instanz
	 */
	public DisableEncryptionRequest() {
		type = DISABLE_ENCRYPTION_REQUEST_TYPE;
		priority = CommunicationConstant.SYSTEM_TELEGRAM_PRIORITY;
	}

	public final String parseToString() {
		return "Systemtelegramm Verschlüsselung Abschalten Anfrage: \n";
	}

	public final void write(DataOutputStream out) throws IOException {
		out.writeShort(length);
	}

	public final void read(DataInputStream in) throws IOException {
		int l = in.readShort();

		length = 0;
		if(length != l) {
			throw new IOException("Falsche Telegrammlänge");
		}
	}
	
}
