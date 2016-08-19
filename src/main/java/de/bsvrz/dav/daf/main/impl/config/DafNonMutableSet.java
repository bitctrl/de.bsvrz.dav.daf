/*
 * Copyright 2008 by Kappich Systemberatung, Aachen
 * Copyright 2007 by Kappich Systemberatung, Aachen
 * Copyright 2004 by Kappich+Kniß Systemberatung, Aachen
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

package de.bsvrz.dav.daf.main.impl.config;

import de.bsvrz.dav.daf.main.config.NonMutableSet;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.sys.funclib.debug.Debug;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Klasse, die den Zugriff auf Konfigurationsmengen seitens der Datenverteiler-Applikationsfunktionen ermöglicht.
 *
 * @author Kappich Systemberatung
 * @version $Revision$
 */
public class DafNonMutableSet extends DafObjectSet implements NonMutableSet {

	/** DebugLogger für Debug-Ausgaben */
	private static final Debug _debug = Debug.getLogger();

	/**
	 * Erzeugt ein neues Objekt dessen Eigenschaften im Anschluss mit der read-Methode eingelesen werden sollten.
	 *
	 * @param dataModel DataModel Implementierung, der das neue Objekt zugeordnet ist.
	 */
	public DafNonMutableSet(DafDataModel dataModel) {
		super(dataModel);
		_internType = NON_MUTABLE_SET;
	}

	/** Erzeugt ein neues Objekt mit den angegebenen Eigenschaften */
	public DafNonMutableSet(
			long id,
			String pid,
			String name,
			long typId,
			byte state,
			String error,
			DafDataModel dataModel,
			short validFromVersionNumber,
			short validToVersionNumber,
			long responsibleObjectId,
			long[] setIds,
			ArrayList<Long> setElementIds
	) {
		super(
				id, pid, name, typId, state, error, dataModel, validFromVersionNumber, validToVersionNumber, responsibleObjectId, setIds, setElementIds
		);
		_internType = NON_MUTABLE_SET;
	}
	
	/** Erzeugt ein neues Objekt mit den angegebenen Eigenschaften */
	public DafNonMutableSet(
			long id,
			String pid,
			String name,
			long typId,
			byte state,
			String error,
			DafDataModel dataModel,
			short validFromVersionNumber,
			short validToVersionNumber,
			long responsibleObjectId,
			long[] setIds,
			long[] setElementIds
	) {
		super(
				id, pid, name, typId, state, error, dataModel, validFromVersionNumber, validToVersionNumber, responsibleObjectId, setIds, setElementIds
		);
		_internType = NON_MUTABLE_SET;
	}

	public final String parseToString() {
		String str = "Statische Menge: \n";
		str += super.parseToString();
		return str;
	}

	public final List<SystemObject> getElements() {
		if((_setElementIds == null) || (_setElementIds.length == 0)) {
			return new ArrayList<SystemObject>();
		}
		if(_setElements == null) {
			_setElements = new ArrayList<>(_dataModel.getObjects(_setElementIds));
			int idx = 0;
			for(Iterator<SystemObject> iterator = _setElements.iterator(); iterator.hasNext(); ) {
				final SystemObject setElement = iterator.next();
				if(setElement == null) {
					_debug.warning("Element der Menge " + getName() + " mit ID " + _setElementIds[idx] + " nicht gefunden (wird ignoriert)");
					iterator.remove();
				}
				idx++;
			}
		}
		return _setElements;
	}

	public final List<SystemObject> getElementsInModifiableVersion() {
		return Collections.unmodifiableList(_dataModel.getSetElementsInNextVersion(this));
	}

	public final List<SystemObject> getElementsInVersion(short version) {
		return Collections.unmodifiableList(_dataModel.getSetElementsInVersion(this, version));
	}

	public final List<SystemObject> getElementsInAllVersions(short fromVersion, short toVersion) {
		return Collections.unmodifiableList(_dataModel.getSetElementsInAllVersions(this, fromVersion, toVersion));
	}

	public final List<SystemObject> getElementsInAnyVersions(short fromVersion, short toVersion) {
		return Collections.unmodifiableList(_dataModel.getSetElementsInAnyVersions(this, fromVersion, toVersion));
	}
}
