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

package de.bsvrz.dav.daf.userManagement;

import de.bsvrz.dav.daf.main.authentication.ClientCredentials;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * TBD Dokumentation
 */
public class EditableAuthenticationFile {

	/**
	 * Zuordnung Benutzername(+Suffix) -> Anmeldedaten
	 *
	 * Kommentare in der Daten werden als Key mit Null-Wert abgebildet
	 */
	private final Map<String, ClientCredentials> _userMap;

	private final Path _authenticationFilePath;

	/**
	 * Erstellt eine neue AuthenticationFile-Instanz
	 * @param authenticationFilePath Dateiname der Authentifizierungsdatei
	 */
	public EditableAuthenticationFile(Path authenticationFilePath) throws IOException {
		Properties properties = new Properties();
		_authenticationFilePath = authenticationFilePath;
		_userMap = new LinkedHashMap<>(properties.size());
		if(Files.exists(_authenticationFilePath)) {
			try(BufferedReader reader = Files.newBufferedReader(_authenticationFilePath, StandardCharsets.ISO_8859_1)) {
				while(true) {
					properties.clear();
					final String s = reader.readLine();
					if(s == null) break;
					properties.load(new StringReader(s));
					if(properties.isEmpty()) {
						// Kommentarzeile
						_userMap.put(s, null);
					}
					else {
						for(Map.Entry<Object, Object> entry : properties.entrySet()) {
							_userMap.put((String) entry.getKey(), ClientCredentials.ofString((String) entry.getValue()));
						}
					}
				}
			}
		}
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
	public ClientCredentials getClientCredentials(String userName, String suffix){
		if(suffix != null && !suffix.isEmpty()){
			// Zuerst den spezifischen Eintrag bestehend aus Benutzernamen und Suffix suchen
			ClientCredentials clientCredentials = _userMap.get(userName + "@" + suffix);
			if(clientCredentials != null) return clientCredentials;
		}
		// Danach als Fallback nur den Benutzernamen prüfen
		return _userMap.get(userName);
	}

	/**
	 * Gibt ein Passwort oder Login-Token zu einem bestimmten Benutzer zurück
	 *
	 * @param userName Benutzername
	 * @return Dem Benutzer zugeordnetes Passwort (oder Login-Token), falls es in der Datei enthalten war. Sonst null.
	 */
	public ClientCredentials getClientCredentials(String userName){
		return getClientCredentials(userName, null);
	}

	public void setClientCredentials(String userName, ClientCredentials clientCredentials) throws IOException {
		setClientCredentials(userName, null, clientCredentials);
	}
	public void setClientCredentials(String userName, String suffix, ClientCredentials clientCredentials) throws IOException {
		String key = userName;
		if(suffix != null) {
			key += "@" + suffix;
		}
		_userMap.put(key, clientCredentials);
		writeFile();
	}

	private void writeFile() throws IOException {
		try(BufferedWriter writer = Files.newBufferedWriter(_authenticationFilePath, StandardCharsets.ISO_8859_1)) {
			final Properties properties = new Properties();
			for(Map.Entry<String, ClientCredentials> entry : _userMap.entrySet()) {
				if(entry.getValue() != null) {
					properties.setProperty(entry.getKey(), entry.getValue().toString());
					final StringWriter stringWriter = new StringWriter();
					properties.store(stringWriter, null);
					try(BufferedReader reader = new BufferedReader(new StringReader(stringWriter.toString()))) {
						final String ignoredComment = reader.readLine();
						writer.write(reader.readLine());
					}
					writer.newLine();
					properties.clear();
				}
				else {
					// Kommentarzeile
					writer.write(entry.getKey());
					writer.newLine();
				}
			}
		}
	}

	public void deleteClientCredentials(final String userName) throws IOException {
		deleteClientCredentials(userName, null);
	}

	private void deleteClientCredentials(final String userName, final String suffix) throws IOException {
		String key = userName;
		if(suffix != null) {
			key += "@" + suffix;
		}
		_userMap.remove(key);
		writeFile();
	}

	public Stream<Map.Entry<String, ClientCredentials>> entries() {
		return _userMap.entrySet().stream().filter(e -> e.getValue() != null);
	}
}
