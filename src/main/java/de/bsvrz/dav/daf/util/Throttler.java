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

package de.bsvrz.dav.daf.util;


import java.time.Duration;
import java.time.Instant;
import java.util.PriorityQueue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Klasse zum Ausbremsen von Login-Versuchen und ähnlichem. Je häufiger die Methode trigger() (pro Minute) aufgerufen wird, desto länger blockiert die Methode.
 * 
 * Wird der Thread mit {@link Thread#interrupt()} unterbrochen, beendet sich das Warten sofort
 *
 * @author Kappich Systemberatung
 */
public class Throttler {

	/**
	 * Liste mit Zeitstempeln von vorherigen Login-Versuchen
	 */
	private final PriorityQueue<Instant> _timeStamps = new PriorityQueue<>(10);

	/**
	 * Länge des Sliding Window
	 */
	private final Duration _slidingWindowLength;

	/**
	 * Wartezeit pro vorhergehendem Methodenaufruf von {@link #trigger()}
	 */
	private final Duration _throttlePerCall;

	/**
	 * Maximale Wartezeit innerhalb der {@link #trigger()}-methode (bei Verwendung von nur einem Thread)
	 */
	private final Duration _maxThrottleDuration;

	/**
	 * Lock-Objekt zur Synchronisation
	 */
	private final ReentrantLock _lock;

	/** 
	 * Erstellt eine neuen Throttler zum Ausbremsen von häufigen Methodenaufrufen (z. B. Login-Versuchen).
	 * 
	 * Die Methode {@link #trigger()} dieser Klasse ist die wesentliche Methode. Je öfter sie aufgerufen wird, desto länger dauert es, bis die Methode wieder verlassen wird.
	 * 
	 * Diese Klasse besitzt 2 Parameter:
	 * 
	 * - `throttlePerCall` gibt eine Zeitdauer an, die pro Login-Versuch in letzter Zeit gewartet wird.
	 *   gibt es keinen Login-Versuch in letzter Zeit wird die Methode sofort verlassen.
	 *   
	 * - `maxThrottleDuration` gibt die maximale Wartezeit an, die ein Thread ausgebremst wird. Da immer nur ein Thread gleichzeitig warten kann,
	 *   kann es bei mehreren parallelen Aufrufen der {@link #trigger()}-Methode zu entsprechend längeren Wartezeiten kommen (Anzahl Threads * `maxThrottleDuration`)
	 *   
	 * Die Zeitdauer (Sliding Window), in der vorhergehende Methodenaufrufe zur Bestimmung der Wartedauer berücksichtigt werden, 
	 * ergibt sich aus `maxThrottleDuration * (maxThrottleDuration / throttlePerCall)`. Das heißt, das gerade so viele Methodenaufrufe
	 * gemerkt werden, wie zum dauerhaften Beibehalten der `maxThrottleDuration` notwendig ist. Beispiel: maxThrottleDuration = 5 Sekunden, throttlePerCall = 1 Sekunde,
	 * dann müssen mindestens die letzten 5 * (5 / 1) = 25 Sekunden berücksichtigt werden, um bei dauerhaften Login-Versuchen eine konstante Wartezeit von 5 Sekunden
	 * beizubehalten.
	 * 
	 * @param throttlePerCall Zeitdauer, die pro Login-Versuch in letzter Zeit gewartet wird
	 * @param maxThrottleDuration maximale Wartezeit, die ein Thread ausgebremst wird
	 */
	public Throttler(Duration throttlePerCall, Duration maxThrottleDuration) {
		if(maxThrottleDuration.isNegative()) throw new IllegalArgumentException();
		if(maxThrottleDuration.minus(throttlePerCall).isNegative()){
			// Wartezeit pro Anmeldung ist größer als maximale Wartezeit, also anpassen
			throttlePerCall = maxThrottleDuration;
		}
		_throttlePerCall = throttlePerCall;
		_maxThrottleDuration = maxThrottleDuration;
		if(_throttlePerCall.isZero()){
			_slidingWindowLength = Duration.ZERO;
		}
		else {
			_slidingWindowLength = _maxThrottleDuration.multipliedBy(1 + _maxThrottleDuration.toMillis() / _throttlePerCall.toMillis());
		}
		_lock = new ReentrantLock(false);
	}

	/**
	 * Methode, deren Ausführung länger dauert, je öfter sie pro Zeitbereich aufgerufen wird. Der Aufruf der Methode dauert
	 * {@link #getThrottlePerCall() throttlePerCall} for jeden vorhergehenden Methodenaufruf innerhalb des {@linkplain #getSlidingWindowLength() Sliding Window}s.
	 * Die Wartezeit ist maximal {@link #getMaxThrottleDuration() maxThrottleDuration}, kann allerdings bei parallelen Aufrufen aus mehreren Threads noch
	 * länger sein, da sich nur ein Thread gleichzeitig in der Methode befinden darf. (Sonst könnte man das Warten umgehen indem man mehrere Threads gleichzeitig startet
	 * bzw. durch mehrere Verbindungen mehrere Threads erzeugt usw.)
	 */
	public void trigger() {
		trigger(true);
	}

	/**
	 * Methode, deren Ausführung länger dauert, je öfter sie pro Zeitbereich aufgerufen wird. Siehe {@link #trigger()}. 
	 * @param addToQueue Soll dieser Methodenaufruf dafür sorgen, dass folgende Aufrufe länger dauern (`true` ist standard, falls `false` wird nur gewartet)
	 */
	public void trigger(final boolean addToQueue) {
		_lock.lock();
		try {
			Instant now = Instant.now();
			cleanQueue(now.minus(_slidingWindowLength));
			try {
				Thread.sleep(delayMillisFor(_timeStamps.size()));
			}
			catch(InterruptedException ignored) {
			}
			if(addToQueue) {
				_timeStamps.add(now);
			}
		}
		finally {
			_lock.unlock();
		}
	}

	/**
	 * Berechnet die Wartezeit aus der anzahl der vorherigen Login-Versuche
	 * @param numLoginAttempts Anzahl Login-Versuche
	 * @return Wartezeit in Millisekunden
	 */
	protected long delayMillisFor(final int numLoginAttempts) {
		Duration throttleDuration = _throttlePerCall.multipliedBy(numLoginAttempts);
		if(throttleDuration.compareTo(_maxThrottleDuration) > 0) throttleDuration = _maxThrottleDuration;
		return throttleDuration.toMillis();
	}

	/**
	 * Entfernt alle Einträge, die älter sind als der aktuelle Zeitstempel, aus der Queue
	 * @param instant Zeitstempel
	 */
	private void cleanQueue(final Instant instant) {
		while(true){
			Instant peek = _timeStamps.peek();
			if(peek == null || peek.isAfter(instant)) {
				break;
			}
			else {
				_timeStamps.remove();
			}
		}
	}

	/** 
	 * Gibt die Länge des Sliding Window zurück. 
	 * 
	 * Die Zeitdauer (Sliding Window), in der vorhergehende Methodenaufrufe zur Bestimmung der Wartedauer berücksichtigt werden, 
	 * ergibt sich aus `maxThrottleDuration * (maxThrottleDuration / throttlePerCall)`. Das heißt, das gerade so viele Methodenaufrufe
	 * gemerkt werden, wie zum dauerhaften Beibehalten der `maxThrottleDuration` notwendig ist. Beispiel: maxThrottleDuration = 5 Sekunden, throttlePerCall = 1 Sekunde,
	 * dann müssen mindestens die letzten 5 * (5 / 1) = 25 Sekunden berücksichtigt werden, um bei dauerhaften Login-Versuchen eine konstante Wartezeit von 5 Sekunden
	 * beizubehalten.
	 * @return die Länge des Sliding Window
	 */
	public Duration getSlidingWindowLength() {
		return _slidingWindowLength;
	}

	/** 
	 * Gibt die Wartezeit pro Methodenaufruf zurück
	 * @return die Wartezeit pro Methodenaufruf
	 */
	public Duration getThrottlePerCall() {
		return _throttlePerCall;
	}

	/** 
	 * Gibt die maximale Wartezeit bei Verwendung eines einzelnen Threads zurück
	 * @return die maximale Wartezeit bei Verwendung eines einzelnen Threads
	 */
	public Duration getMaxThrottleDuration() {
		return _maxThrottleDuration;
	}
}
