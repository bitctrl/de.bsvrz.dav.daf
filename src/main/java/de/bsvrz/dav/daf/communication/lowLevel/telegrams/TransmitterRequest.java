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
 * Erstes Telegramm, dass in einer verschlüsselten Verbindung versendet wird, um dem verbundenen Datenverteiler die eigene ID mitzuteilen. Der Server antwortet mit einer (in der Regel positiv quittierten, da die Authentifizierung ja bereits erfolgreich war) {@link TransmitterAuthentificationAnswer}.
 *
 * @author Kappich Systemberatung
 */
public class TransmitterRequest extends DataTelegram {

	private long _transmitterId;

	/**
	 * Erstellt eine neue nicht-initialisierte Instanz (zur Initialisierung über {@link #read(DataInputStream)}).
	 */
	public TransmitterRequest() {
		type = TRANSMITTER_REQUEST_TYPE;
		priority = CommunicationConstant.SYSTEM_TELEGRAM_PRIORITY;
	}

	/**
	 * Erstellt ein neues Telegramm mit vordefiniertem Inhalt
	 *
	 * @param transmitterId eigene Datenverteiler-ID
	 */
	public TransmitterRequest(long transmitterId) {
		type = TRANSMITTER_REQUEST_TYPE;
		priority = CommunicationConstant.SYSTEM_TELEGRAM_PRIORITY;
		this._transmitterId = transmitterId;
		length = 8;
	}

	/** 
	 * Gibt die eigene Datenverteiler-ID zurück
	 * @return die eigene Datenverteiler-ID
	 */
	public long getTransmitterId() {
		return _transmitterId;
	}

	public final String parseToString() {
		String str = "Systemtelegramm Datenverteiler Anfrage: \n";
		str += "Datenverteiler-ID   : " + _transmitterId + "\n";
		return str;
	}

	public final void write(DataOutputStream out) throws IOException {
		out.writeShort(length);
		out.writeLong(_transmitterId);
	}

	public final void read(DataInputStream in) throws IOException {
		int _length = in.readShort();
		_transmitterId = in.readLong();
		length = 8;
		if(length != _length) {
			throw new IOException("Falsche Telegrammlänge");
		}
	}
}
