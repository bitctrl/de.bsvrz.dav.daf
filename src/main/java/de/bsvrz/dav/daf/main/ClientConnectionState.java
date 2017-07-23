/*
 * Copyright 2013 by Kappich Systemberatung Aachen
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

package de.bsvrz.dav.daf.main;

/**
 * Verbindungsstatus von Anmeldungen
 * 
 * @author Kappich Systemberatung
 * @version $Revision: 0000 $
 */
public enum ClientConnectionState {
	/**
	 * Die angemeldete Applikation ist lokal verbunden (normale Anmeldung an diesem Datenverteiler)
	 */
	FromLocalOk,
	/**
	 * Eine eingehende Anmeldung von einem anderen Datenverteiler. Dies entspricht vom Verhalten her
	 * in vielen Punkten einer lokalen Anmeldung, d.h. der Datenverteiler meldet an den anfragenden Datenverteiler zurück,
	 * ob eine Quelle/Senke verfügbar ist oder nicht.
	 */
	FromRemoteOk,
	/**
	 * Ausgehende Anmeldung an einen anderen potentiellen Zentraldatenverteiler, noch keine Rückmeldung
	 */
	ToRemoteWaiting,
	/**
	 * Ausgehende Anmeldung an einen anderen potentiellen Zentraldatenverteiler, positive Rückmeldung (anderer Datenverteiler
	 * ist entweder der Zentraldatenverteiler oder dieser ist über ihn erreichbar).
	 */
	ToRemoteOk,
	/**
	 * Ausgehende Anmeldung an einen anderen potentiellen Zentraldatenverteiler, negative Rückmeldung da
	 * anderer Datenverteiler keine Quelle/Senke für das Datum besitzt
	 */
	ToRemoteNotResponsible,
	/**
	 * Ausgehende Anmeldung an einen anderen potentiellen Zentraldatenverteiler, negative Rückmeldung da
	 * keine Berechtigung vorliegt, die Daten zu empfangen oder zu senden
	 */
	ToRemoteNotAllowed,
	/**
	 * Ausgehende Anmeldung an einen anderen potentiellen Zentraldatenverteiler, negative Rückmeldung da
	 * am anderen Datenverteiler mehrere potentielle Zentraldatenverteiler verbunden sind, von denen mehrere
	 * eine positive Rückmeldung gegeben haben. Es gibt also mehrere Quellen oder Senken.
	 */
	ToRemoteMultiple
}
