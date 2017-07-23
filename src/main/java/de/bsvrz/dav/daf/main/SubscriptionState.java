/*
 * Copyright 2011 by Kappich Systemberatung Aachen
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
 * Zustand einer Anmeldung am Datenverteiler
 *
 * @author Kappich Systemberatung
 */
public enum SubscriptionState {
	/**
	 * Die Anmeldung als Sender/Quelle ist erfolgreich, aber es sind keine Empfänger/Senke vorhanden.
	 */
	NoReceiversAvailable,
	/**
	 * Die Anmeldung ist wegen fehlenden Rechten nicht erlaubt.
	 */
	NotAllowed,
	/**
	 * Die Anmeldung ist ungültig (z.B. bei mehreren Quellen/Senken).
	 */
	InvalidSubscription,
	/**
	 * Die Anmeldung als Empfänger/Senke ist erfolgreich, aber es sind keine Quelle/Sender vorhanden.
	 */
	NoSendersAvailable,
	/**
	 * Anmeldung als Empfänger/Senke ist erfolgreich, Quelle/Sender sind vorhanden.
	 */
	SendersAvailable,
	/**
	 * Anmeldung als Sender/Quelle ist erfolgreich, Empfänger/Senke sind vorhanden.
	 */
	ReceiversAvailable,
	/**
	 * Bei ausgehenden Anmeldungen zu anderen Zentraldatenverteilern:
	 * Die Anmeldung wartet auf Rückmeldung von anderen Datenverteilern. Ist lokal keine Quelle/Senke vorhanden,
	 * dann verschickt der lokale Datenverteiler Anmeldungen an verbundene potentielle Zentral-Datenverteiler.
	 * Befindet sich eine Anmeldung in diesem Zustand, hat der verbundene Datenverteiler diese Anmeldung noch nicht quittiert.
	 */
	Waiting,
	/**
	 * Bei ausgehenden Anmeldungen zu anderen Zentraldatenverteilern: Die angefragten Datenverteiler sind nicht zuständig für die Daten.
	 */
	NotResponsible,
	/**
	 * Bei ausgehenden Anmeldungen zu anderen Zentraldatenverteilern:  Mehrere verbundene Datenverteiler haben signalisiert,
	 * dass sie der Zentraldatenverteiler sind. Diese Zustand ist ungültig
	 * und sorgt dafür, dass die Anmeldung gesperrt ist, bis das Problem behoben wurde.
	 */
	MultiRemoteLock
}
