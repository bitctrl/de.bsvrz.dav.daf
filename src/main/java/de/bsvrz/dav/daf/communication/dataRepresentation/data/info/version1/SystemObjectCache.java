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

package de.bsvrz.dav.daf.communication.dataRepresentation.data.info.version1;

import de.bsvrz.dav.daf.main.config.DataModel;
import de.bsvrz.dav.daf.main.config.SystemObject;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Map-Ähnliche Klasse, die Systemobjekte als Keys pro Datenmodell verwaltet und so die Objekte eines einzelnen Datenmodells schnell löschen kann.
 * 
 * Die Implementierung ist aktuell nicht threadsafe, es muss also extern synchronisiert werden.
 *
 * @author Kappich Systemberatung
 */
public class SystemObjectCache<K extends SystemObject, V> {
	
	private final Map<DataModel, Map<K, V>> _dataModels = new WeakHashMap<>();
	
	public int size() {
		return _dataModels.values().stream().mapToInt(Map::size).sum();
	}
	
	public boolean isEmpty() {
		return _dataModels.values().stream().allMatch(Map::isEmpty);
	}

	private Map<K, V> getDataModelMap(final Object key) {
		SystemObject systemObject = (SystemObject) key;
		DataModel dataModel = systemObject.getDataModel();
		
		Map<K, V> dataModelMap = _dataModels.get(dataModel);
		if(dataModelMap == null){
			dataModelMap = new HashMap<K, V>();
			_dataModels.put(dataModel, dataModelMap);
		}
		return dataModelMap;
	}

	public boolean containsKey(final Object key) {
		return getDataModelMap(key).containsKey(key);
	}

	public boolean containsValue(final Object value) {
		return _dataModels.values().stream().allMatch(map -> map.containsValue(value));
	}
	
	public V get(final Object key) {
		return getDataModelMap(key).get(key);
	}
	
	public V put(final K key, final V value) {
		return getDataModelMap(key).put(key, value);
	}

	
	public V remove(final Object key) {
		return getDataModelMap(key).remove(key);
	}

	
	public void putAll(final Map<? extends K, ? extends V> m) {
		m.forEach(this::put);
	}

	
	public void clear() {
		_dataModels.clear();
	}

	/**
	 * Löscht alle Keys, die das angegeben Datenmodell verwenden
	 * @param dataModel Datenmodell
	 */
	public void forgetDataModel(final DataModel dataModel) {
		_dataModels.remove(dataModel);
	}
}
