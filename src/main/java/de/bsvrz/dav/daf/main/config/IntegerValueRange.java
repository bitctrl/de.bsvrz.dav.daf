/*
 * Copyright 2005 by Kappich+Kniß Systemberatung Aachen (K2S)
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

package de.bsvrz.dav.daf.main.config;

/**
 * Schnittstellenklasse zum Zugriff auf die Eigenschaften von Wertebereichen. Wertebereiche werden in {@link
 * IntegerAttributeType Ganzzahl-Attributtypen} benutzt.
 *
 * @author Kappich+Kniß Systemberatung Aachen (K2S)
 * @author Roland Schmitz (rs)
 * @author Stephan Homeyer (sth)
 * @version $Revision$ / $Date$ / ($Author$)
 */
public interface IntegerValueRange extends ConfigurationObject {
	/**
	 * Bestimmt den minimal erlaubten Wert dieses Bereichs.
	 *
	 * @return Minimum dieses Bereichs.
	 */
	public long getMinimum();

	/**
	 * Bestimmt den maximal erlaubten Wert dieses Bereichs.
	 *
	 * @return Maximum dieses Bereichs
	 */
	public long getMaximum();

	/**
	 * Bestimmt den Skalierungsfaktor mit dem interne Werte multipliziert werden, um die externe Darstellung zu
	 * erhalten.
	 *
	 * @return Skalierungsfaktor dieses Bereichs.
	 */
	public double getConversionFactor();

	/**
	 * Bestimmt die Maßeinheit von Werten dieses Bereichs nach der Skalierung in die externe Darstellung.
	 *
	 * @return Maßeinheit dieses Bereichs.
	 */
	public String getUnit();
}

