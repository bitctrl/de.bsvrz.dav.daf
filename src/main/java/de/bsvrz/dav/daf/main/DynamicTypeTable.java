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

package de.bsvrz.dav.daf.main;

import de.bsvrz.dav.daf.main.config.*;
import de.bsvrz.sys.funclib.debug.Debug;

import java.util.*;

/**
 * Optimierte Implementierung der Verwaltung dynamischer Objekte zur direkten Verwendung im Datenverteiler und in den DAF. Bietet die gleiche Kern-Funktionalität wie die de.bsvrz.sys.funclib.dynobj,
 * aber keine erweiterten Möglichkeiten zum Anlegen/Löschen von Objekten.
 *
 * @author Kappich Systemberatung
 */
class DynamicTypeTable {

	/** der Standard-Konfigurationsbereich der AOE. */
	private final ConfigurationArea _defaultArea;

	/**
	 * Typ "typ.dynamischesObjekt"
	 */
	private final DynamicObjectType _baseType;

	/**
	 * Zuordnungstabelle von dynamischen Objekttypen zu Konfigurationsbereichen.
	 */
	private volatile Map<DynamicObjectType, ConfigurationArea> _typesToDefaultArea = Collections.emptyMap();

	/**
	 * Debug-Logger
	 */
	private static final Debug _debug = Debug.getLogger();

	/** 
	 * Erstellt eine neue DynamicTypeTable-Instanz
	 */
	DynamicTypeTable(final ClientDavInterface connection, final ConfigurationAuthority authority) {

		Objects.requireNonNull(connection, "connection ist null");
		Objects.requireNonNull(authority, "authority ist null");
		
		final DataModel dataModel = connection.getDataModel();

		_baseType = (DynamicObjectType) dataModel.getObject("typ.dynamischesObjekt");
		_defaultArea = authority.getDefaultConfigurationArea();

		final AttributeGroup atg = dataModel.getAttributeGroup("atg.verwaltungDynamischerObjekte");
		final Aspect aspect = dataModel.getAspect("asp.parameterSoll");

		if (atg == null || aspect == null) {
			_debug.error("Der Parameterdatensatz für die Verwaltung dynamischer Objekte ist nicht verfügbar.");
			return;
		}
		
		final DataDescription parameterDataDescription = new DataDescription(atg, aspect);
		final ResultData initialData = connection.getData(authority, parameterDataDescription, 10_000L);
		
		final Updater updater = new Updater();
		updater.update(initialData);
		connection.subscribeReceiver(updater, authority, parameterDataDescription, ReceiveOptions.normal(), ReceiverRole.receiver());
	}

	/**
	 * Gibt den Standardbereich für den übergebenen dynamischen Typ zurück
	 * @param dynamicObjectType Zu prüfender Typ
	 * @return Konfigurationsbereich in dem Objekte dieses Typs abgelegt werden sollen. (Kann in seltenen Fällen null sein, beispielsweise wenn kein Parameter vorhanden ist und {@link ConfigurationAuthority#getDefaultConfigurationArea()}
	 * null zurückliefert.)
	 */
	ConfigurationArea getDefaultArea(final DynamicObjectType dynamicObjectType) {
		Map<DynamicObjectType, ConfigurationArea> map = _typesToDefaultArea;
		ConfigurationArea result = map.get(dynamicObjectType);
		if(result != null) {
			return result;
		}
		if(dynamicObjectType != null){
			// Supertypen betrachten
			final Set<SystemObjectType> seenTypes = new HashSet<>();
			List<SystemObjectType> superTypes = dynamicObjectType.getSuperTypes();
			while(superTypes.size() == 1){
				SystemObjectType superType = superTypes.get(0);
				if(!seenTypes.add(superType)) {
					// Unendliche Schleife verhindern, typ bereits gesehen
					break;
				}
				result = map.get(superType);
				if(result != null) {
					return result;
				}
				superTypes = superType.getSuperTypes();
			}
		}
		result = map.get(_baseType);
		if(result != null) {
			return result;
		}
		return _defaultArea;
	}

	private class Updater implements ClientReceiverInterface{

		@Override
		public void update(final ResultData... results) {
			for(final ResultData result : results) {
				Map<DynamicObjectType, ConfigurationArea> newMap = new HashMap<>();
				final Data data = result.getData();
				if(data != null) {
					final Data array = data.getItem("ZuordnungDynamischerObjektTypZuKB");
					for(Data entry : array) {
						final DynamicObjectType typ = (DynamicObjectType) entry.getReferenceValue("DynamischerTypReferenz").getSystemObject();
						final ConfigurationArea kb = (ConfigurationArea) entry.getReferenceValue("KonfigurationsBereichReferenz").getSystemObject();
						newMap.put(typ, kb);
					}
				}
				else {
					_debug.warning("Der Datensatz zur Verwaltung der dynamischen Objekte kann nicht gelesen werden: " + result);
				}
				_typesToDefaultArea = newMap;
			}
		}
	}
}
