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

package de.bsvrz.dav.daf.communication.srpAuthentication;

import de.bsvrz.dav.daf.main.config.ConfigurationTaskException;

import java.util.Objects;

/**
 * Exception die geworfen wird, wenn die Konfiguration kein SRP unterstützt
 *
 * @author Kappich Systemberatung
 */
public class SrpNotSupportedException extends ConfigurationTaskException {
	private static final long serialVersionUID = -7708270017726899848L;

	/**
	 * Erzeugt eine Instanz mit einer Fehlermeldung 
	 * @param message Fehlermeldung
	 */
	public SrpNotSupportedException(final String message) {
		super(message);
		Objects.requireNonNull(message, "message == null");
	}
	
	/**
	 * Erzeugt eine Instanz mit einer Fehlermeldung und einem Grund 
	 * @param message Fehlermeldung
	 * @param cause Grund
	 */
	public SrpNotSupportedException(final String message, final Throwable cause) {
		super(message, cause);
		Objects.requireNonNull(message, "message == null");
	}
}
