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

package de.bsvrz.dav.daf.main.authentication;

import de.bsvrz.sys.funclib.debug.Debug;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * Klasse die eine Authentifizierungs-Datei ("passwd") einliest. Eine passwd-Datei besteht aus Schlüssel-Wert-Paaren (wie in {@link Properties} beschrieben)
 * wobei der Schlüssel ein Benutzername, optional gefolgt von einem "@"-Zeichen und einem Suffix ist, und der Wert entweder ein Klartextpasswort ist, oder
 * ein binärer Login-Token (z.B. ein SRP-"x"-Wert, mit dem sich der Benutzer beim Datenverteiler authentifizieren kann, ohne dass das Passwort im Klartext in der Datei stehen muss)
 * 
 * Ein solcher Token besitzt das Format "[XXXX]~~~~ [Hex-Daten]" wobei [XXXX] ein Authentifizierungsverfahren wie bspw. "SRP6" angibt.
 * 
 * Als Suffix für den Benutzernamen sind möglich:
 * 
 * - `@[Datenverteiler-Pid]` um das Passwort zur Anmeldung bei einem bestimmten Datenverteiler festzulegen
 * 
 * - `@[Konfigurationsverantwortlicher-Pid]` um das Passwort festzulegen, mit dem ein Datenverteiler sich bei einer Konfiguration authentifiziert
 *
 * @author Kappich Systemberatung
 */
public final class AuthenticationFile implements UserProperties {

	/**
	 * Pfad der Authentifizierungsdatei
	 */
	private final Path _authenticationFilePath;

	/** Logger */
	private static final Debug _debug = Debug.getLogger();
	
	/** 
	 * Erstellt eine neue AuthenticationFile-Instanz
	 * @param authenticationFilePath Dateiname der Authentifizierungsdatei
	 */
	public AuthenticationFile(Path authenticationFilePath) {
		_authenticationFilePath = Objects.requireNonNull(authenticationFilePath);
	}

	/**
	 * Gibt ein Passwort oder Login-Token zu einem bestimmten Benutzer zurück
	 *
	 * @param userName Benutzername
	 * @param suffix   Optionaler String, der spezifiziert, wo sich der Benutzer einloggen will. Beispielsweise kann ein Benutzer bei mehreren Datenverteilern
	 *                 unterschiedliche Namen vorgeben. In der Datei kann daher mit einem "@" getrennt an den Benutzernamen der "suffix" angehängt, werden. Dies
	 *                 kann z.B. die Pid des Datenverteilers sein.
	 * @return Dem Benutzer (und ggf. Suffix) zugeordnetes Passwort (oder Login-Token), falls es in der Datei enthalten war. Sonst null.
	 */
	@Override
	public ClientCredentials getClientCredentials(String userName, String suffix){
		HashMap<String, ClientCredentials> userMap = readFile();

		if(suffix != null && !suffix.isEmpty()) {
			// Zuerst den spezifischen Eintrag bestehend aus Benutzernamen und Suffix suchen 
			ClientCredentials clientCredentials = userMap.get(userName + "@" + suffix);
			if(clientCredentials != null) return clientCredentials;
		}
		// Danach als Fallback nur den Benutzernamen prüfen
		return userMap.get(userName);
	}

	private HashMap<String, ClientCredentials> readFile() {
		HashMap<String, ClientCredentials> userMap = new HashMap<>();
		try {
			Properties properties = new Properties();
			try(BufferedReader reader = Files.newBufferedReader(_authenticationFilePath, StandardCharsets.ISO_8859_1)) {
				properties.load(reader);
			}
			userMap = new HashMap<>(properties.size());
			for(Map.Entry<Object, Object> entry : properties.entrySet()) {
				userMap.put((String) entry.getKey(), ClientCredentials.ofString((String) entry.getValue()));
			}
		}
		catch(IOException e){
			// Aufräumen
			userMap.clear();
			
			_debug.error("Kann Authentifizierungsdatei \"" + _authenticationFilePath + "\" nicht lesen", e);
		}
		return userMap;
	}

}
