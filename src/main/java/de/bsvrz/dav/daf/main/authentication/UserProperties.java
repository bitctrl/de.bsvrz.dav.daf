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

/**
 * Interface für eine Datenbank zur Zuordnung von Benutzername und Passwort. Beispielsweise realisiert durch eine Authentifizierungsdatei (passwd).
 *
 * @author Kappich Systemberatung
 */
public interface UserProperties {
	/**
	 * Gibt ein Passwort oder Login-Token zu einem bestimmten Benutzer zurück
	 *
	 * @param userName Benutzername
	 * @param suffix   Optionaler String, der spezifiziert, wo sich der Benutzer einloggen will. Beispielsweise kann ein Benutzer bei mehreren Datenverteilern
	 *                 unterschiedliche Namen vorgeben. In der Datei kann daher mit einem "@" getrennt an den Benutzernamen der "suffix" angehängt, werden. Dies
	 *                 kann z.B. die Pid des Datenverteilers sein.
	 * @return Dem Benutzer (und ggf. Suffix) zugeordnetes Passwort (oder Login-Token), falls es in der Datei enthalten war. Sonst null.
	 */
	ClientCredentials getClientCredentials(String userName, String suffix);

	/**
	 * Gibt ein Passwort oder Login-Token zu einem bestimmten Benutzer zurück
	 *
	 * @param userName Benutzername
	 * @return Dem Benutzer zugeordnetes Passwort (oder Login-Token), falls es in der Datei enthalten war. Sonst null.
	 */
	default ClientCredentials getClientCredentials(String userName){
		return getClientCredentials(userName, null);
	}
}
