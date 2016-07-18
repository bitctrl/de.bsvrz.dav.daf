/*
 * Copyright 2004 by Kappich+Kniß Systemberatung Aachen (K2S)
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
package de.bsvrz.dav.daf.communication.dataRepresentation.data.info;

import de.bsvrz.dav.daf.main.Data;

/**
 * Schnittstelle, die noch zu dokumentieren ist.
 *
 * @author Kappich+Kniß Systemberatung Aachen (K2S)
 * @author Roland Schmitz (rs)
 * @version $Revision$ / $Date$ / ($Author$)
 */
public interface AttributeInfo {

	public abstract String getName();

	public abstract AttributeDefinitionInfo getDefinitionInfo();

	public abstract boolean isArray();

	public abstract void dump(int indent);

//	boolean isPlain();
//	AttributeInfo getItem(String itemName);
//	int getStart();
//	int getSize();
//	int getEnd();

	public abstract boolean isSizeFixed();

	public abstract int getFixedSize();
	int getSize(byte[] bytes, int offset);

	int getRelativeOffset();

	AttributeInfo getOffsetReferral();

	int getAbsoluteOffset(byte[] bytes, int parentOffset);

	int getElementCount(byte[] bytes, int offset);

	int getAbsoluteElementOffset(byte[] bytes, int offset, int elementIndex);

	AttributeInfo getElementInfo();

	public boolean isCountVariable();

	public boolean isCountLimited();

	public int getMaxCount();

	Data createModifiableData(byte[] bytes);
}
