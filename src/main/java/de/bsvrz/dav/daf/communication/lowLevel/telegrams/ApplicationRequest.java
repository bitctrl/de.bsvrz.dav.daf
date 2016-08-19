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
 * Erstes Telegramm, dass in einer verschlüsselten Verbindung versendet wird, um dem Datenverteiler Applikationsnamen, Typ, usw. mitzuteilen
 * und eine Applikations-ID anzufordern. Der Server antwortet mit einer (in der Regel positiv quittierten, da die Authentifizierung ja bereits erfolgreich war) {@link AuthentificationAnswer}.
 * Eine negative Quittung kann aber dennoch möglich sein, wenn beispielsweise der Applikationstyp unbekannt ist oder ein anderer Parameter nicht erlaubt ist.
 *
 * @author Kappich Systemberatung
 */
public class ApplicationRequest extends DataTelegram {

	/** Der Applikationsname */
	private String _applicationName;

	/** Die PID der Applikationstyp */
	private String _applicationTypePid;

	/** Die PID und ID des Konfigurationsverantwortlichen (durch einen Doppelpunkt getrennt) */
	private String _configurationPid;

	/**
	 * Erstellt eine neue nicht-initialisierte Instanz (zur Initialisierung über {@link #read(DataInputStream)}).
	 */
	public ApplicationRequest() {
		type = APPLICATION_REQUEST_TYPE;
		priority = CommunicationConstant.SYSTEM_TELEGRAM_PRIORITY;
	}

	/**
	 * Erstellt ein neues Telegramm mit vordefiniertem Inhalt
	 *
	 * @param applicationName    Applikationsname
	 * @param applicationTypePid PID des Applikationstypen
	 * @param configurationPid   PID der Konfiguration
	 */
	public ApplicationRequest(String applicationName, String applicationTypePid, final String configurationPid) {
		type = APPLICATION_REQUEST_TYPE;
		priority = CommunicationConstant.SYSTEM_TELEGRAM_PRIORITY;
		this._applicationName = applicationName;
		this._applicationTypePid = applicationTypePid;
		this._configurationPid = configurationPid;
		length = 0;
		length += this._applicationName.getBytes(StandardCharsets.UTF_8).length + 2;
		length += this._applicationTypePid.getBytes(StandardCharsets.UTF_8).length + 2;
		length += this._configurationPid.getBytes(StandardCharsets.UTF_8).length + 2;
	}

	/**
	 * Ermittelt den Applikationsnamen
	 *
	 * @return Applikationsname
	 */
	public final String getApplicationName() {
		return _applicationName;
	}

	/**
	 * Ermittelt die PID des Applikationstypen
	 *
	 * @return Applikationstyp PID
	 */
	public final String getApplicationTypePid() {
		return _applicationTypePid;
	}

	/**
	 *  Die PID und ID des Konfigurationsverantwortlichen (durch einen Doppelpunkt getrennt) 
	 *
	 * @return Pid und ID der Konfiguration (falls es sich um eine Konfiguration handelt, die sich anmeldet)
	 */
	public String getConfigurationPid() {
		return _configurationPid;
	}

	public final String parseToString() {
		String str = "Systemtelegramm Applikation Anfrage: \n";
		str += "Applikationsname    : " + _applicationName + "\n";
		str += "Applikationstyp Pid : " + _applicationTypePid + "\n";
		str += "Konfiguration Pid   : " + _configurationPid + "\n";
		return str;
	}

	public final void write(DataOutputStream out) throws IOException {
		out.writeShort(length);
		if(_applicationTypePid != null) {
			out.writeUTF(_applicationTypePid);
		}
		if(_applicationName != null) {
			out.writeUTF(_applicationName);
		}
		if(_configurationPid != null) {
			out.writeUTF(_configurationPid);
		}
	}

	public final void read(DataInputStream in) throws IOException {
		int _length = in.readShort();
		_applicationTypePid = in.readUTF();
		_applicationName = in.readUTF();
		_configurationPid = in.readUTF();
		length = 0;
		length += _applicationName.getBytes(StandardCharsets.UTF_8).length + 2;
		length += _applicationTypePid.getBytes(StandardCharsets.UTF_8).length + 2;
		length += _configurationPid.getBytes(StandardCharsets.UTF_8).length + 2;
		if(length != _length) {
			throw new IOException("Falsche Telegrammlänge");
		}
	}
}
