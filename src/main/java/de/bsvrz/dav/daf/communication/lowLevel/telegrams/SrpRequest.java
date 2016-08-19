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
import java.nio.charset.StandardCharsets;

/**
 * Erstes Telegramm der SRP-Authentifizierung, wird vom Client zum Server geschickt, primär um den Benutzernamen mitzuteilen
 *
 * @author Kappich Systemberatung
 */
public class SrpRequest extends DataTelegram {

	/** Der Einmalpasswort-Index */
	private int _passwordIndex;
	
	/** Der Benutzername */
	private String _userName;
	
	/**
	 * Erstellt eine neue nicht-initialisierte Instanz (zur Initialisierung über {@link #read(DataInputStream)}).
	 */
	public SrpRequest() {
		type = SRP_REQUEST_TYPE;
		priority = CommunicationConstant.SYSTEM_TELEGRAM_PRIORITY;
	}

	/**
	 * Erstellt ein neues Telegramm mit vordefiniertem Inhalt
	 *
	 * @param userName             Benutzername
	 * @param passwordIndex        Index den Einmalpassworts oder -1 falls das normale Passwort benutzt werden soll
	 */
	public SrpRequest(String userName, final int passwordIndex) {
		type = SRP_REQUEST_TYPE;
		priority = CommunicationConstant.SYSTEM_TELEGRAM_PRIORITY;
		this._userName = userName;
		this._passwordIndex = passwordIndex;
		length = 0;
		length += this._userName.getBytes(StandardCharsets.UTF_8).length + 2;
		length += 4;
	}

	/** 
	 * Gibt den Benutzernamen zurück
	 * @return den Benutzernamen
	 */
	public String getUserName() {
		return _userName;
	}

	/** Gibt den Index des zu verwendenden Einmalpassworts zurück 
	 * @return Den Einmalpasswortindex oder -1 falls kein Einmalpasswort verwendet werden soll
	 */
	public int getPasswordIndex() {
		return _passwordIndex;
	}

	public final String parseToString() {
		String str = "Systemtelegramm SRP Anfrage: \n";
		str += "Benutzername        : " + _userName + "\n";
		str += "Passwortindex       : " + _passwordIndex + "\n";
		return str;
	}

	public final void write(DataOutputStream out) throws IOException {
		out.writeShort(length);
		out.writeUTF(_userName);
		out.writeInt(_passwordIndex);
	}

	public final void read(DataInputStream in) throws IOException {
		int _length = in.readShort();
		_userName = in.readUTF();
		_passwordIndex = in.readInt();
		
		length = 0;
		length += _userName.getBytes(StandardCharsets.UTF_8).length + 2;
		length += 4;
		if(length != _length) {
			throw new IOException("Falsche Telegrammlänge");
		}
	}
	
}
