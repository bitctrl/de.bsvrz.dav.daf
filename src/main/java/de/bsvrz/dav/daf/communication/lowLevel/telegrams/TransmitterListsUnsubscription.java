/*
 * Copyright 2007 by Kappich Systemberatung, Aachen
 * Copyright 2004 by Kappich+Kniß Systemberatung, Aachen
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
 * Mit diesem Systemtelegramm fordert Datenverteiler A seinen Nachbardatenverteiler B auf, ihm aus seiner lokalen Anmeldungsliste zu bestimmten erreichbaren
 * Datenverteilern die Objekt- und Attributgruppenliste nicht mehr zur Verfügung zu stellen. Datenverteiler A meldet ein zuvor bei Datenverteiler B angemeldetes
 * Abonnement wieder ab.
 *
 * @author Kappich Systemberatung
 * @version $Revision$
 */
public class TransmitterListsUnsubscription extends DataTelegram {

	/** Die Liste der Datenverteiler */
	private long transmitterList[];

	public TransmitterListsUnsubscription() {
		type = TRANSMITTER_LISTS_UNSUBSCRIPTION_TYPE;
		priority = CommunicationConstant.SYSTEM_TELEGRAM_PRIORITY;
	}

	public TransmitterListsUnsubscription(long _transmitterList[]) {
		type = TRANSMITTER_LISTS_UNSUBSCRIPTION_TYPE;
		priority = CommunicationConstant.SYSTEM_TELEGRAM_PRIORITY;
		transmitterList = _transmitterList;
		length = 2;
		if(transmitterList != null) {
			length += (transmitterList.length * 8);
		}
	}

	/**
	 * Gibt die Liste der Datenverteiler zurück
	 *
	 * @return Liste der Datenverteiler
	 */
	public final long[] getTransmitterList() {
		return transmitterList;
	}

	public final String parseToString() {
		String str = "Systemtelegramm Datenverteileranmeldelisten Abonement Kündigung:\n";
		if(transmitterList != null) {
			str += "Datenverteilerliste: [ ";
			for(int i = 0; i < transmitterList.length; ++i) {
				str += " " + transmitterList[i] + " ";
			}
			str += " ]\n";
		}
		return str;
	}

	public final void write(DataOutputStream out) throws IOException {
		out.writeShort(length);
		if(transmitterList == null) {
			out.writeShort(0);
		}
		else {
			out.writeShort(transmitterList.length);
			for(int i = 0; i < transmitterList.length; ++i) {
				out.writeLong(transmitterList[i]);
			}
		}
	}

	public final void read(DataInputStream in) throws IOException {
		int _length = in.readShort();
		length = 2;
		if(_length < length) throw new IOException("Falsche Telegrammlänge (zu kurz)");
		int size = in.readShort();
		length += (size * 8);
		if(length != _length) {
			throw new IOException("Falsche Telegram Länge");
		}
		if(size > 0) {
			transmitterList = new long[size];
			for(int i = 0; i < size; ++i) {
				transmitterList[i] = in.readLong();
			}
		}
	}
}
