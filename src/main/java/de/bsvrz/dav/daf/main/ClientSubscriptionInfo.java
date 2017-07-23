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


import de.bsvrz.dav.daf.main.config.AttributeGroupUsage;
import de.bsvrz.dav.daf.main.config.DavApplication;
import de.bsvrz.dav.daf.main.config.SystemObject;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Diese Klasse gibt die Anmeldungen an einer Attributgruppenverwendung am Datenverteiler zurück.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 11467 $
 * @see ClientDavInterface#getSubscriptionInfo(DavApplication, SystemObject, AttributeGroupUsage, short)
 */
public class ClientSubscriptionInfo {

	/**
	 * Verbindung
	 */
	private final ClientDavConnection _connection;

	/**
	 * Sende-Anmeldungen
	 */
	private final List<ClientSendingSubscription> _senderSubscriptions = new ArrayList<ClientSendingSubscription>();

	/**
	 * Empfangsanmeldungen
	 */
	private final List<ClientReceivingSubscription> _receiverSubscriptions = new ArrayList<ClientReceivingSubscription>();

	/**
	 * Potentielle Zentraldatenverteiler
	 */
	private final List<DavInformation> _potentialCentralDavs = new ArrayList<DavInformation>();

	/**
	 * Erstellt eine neue ClientSubscriptionInfo
	 *
	 * @param connection Verbindung
	 * @param bytes      Serialisierte Daten vom Datenverteiler
	 */
	ClientSubscriptionInfo(final ClientDavConnection connection, final byte[] bytes) throws IOException {
		_connection = connection;
		final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
		try(DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream)) {
			final int numReceivingSubscriptions = dataInputStream.readInt();
			for(int i = 0; i < numReceivingSubscriptions; i++) {
				boolean isLocal = dataInputStream.readBoolean();
				final long applicationId = dataInputStream.readLong();
				final long userId = dataInputStream.readLong();
				final boolean isSource = dataInputStream.readBoolean();
				final boolean isRequestSupported = dataInputStream.readBoolean();
				final int state = dataInputStream.readInt();
				final int constate = dataInputStream.readInt();
				final ClientSendingSubscription clientSendingSubscription =
						new ClientSendingSubscription(
								isLocal,
								applicationId,
								userId,
								isSource,
								isRequestSupported,
								state,
								constate
						);
				_senderSubscriptions.add(clientSendingSubscription);
			}
			final int numSendingSubscriptions = dataInputStream.readInt();
			for(int i = 0; i < numSendingSubscriptions; i++) {
				boolean isLocal = dataInputStream.readBoolean();
				final long applicationId = dataInputStream.readLong();
				final long userId = dataInputStream.readLong();
				final boolean isDrain = dataInputStream.readBoolean();
				final boolean isDelayed = dataInputStream.readBoolean();
				final boolean isDelta = dataInputStream.readBoolean();
				final int state = dataInputStream.readInt();
				final int constate = dataInputStream.readInt();
				final ClientReceivingSubscription clientReceivingSubscription =
						new ClientReceivingSubscription(
								isLocal,
								applicationId,
								userId,
								isDrain,
								isDelayed,
								isDelta,
								state,
								constate
						);
				_receiverSubscriptions.add(clientReceivingSubscription);
			}
			if(dataInputStream.available() > 0) {
				int numpotCentralDavs = dataInputStream.readInt();
				for(int i = 0; i < numpotCentralDavs; i++) {
					long centralDavId = dataInputStream.readLong();
					long connectionDavId = dataInputStream.readLong();
					int throughputResistance = dataInputStream.readInt();
					long userId = dataInputStream.readLong();
					_potentialCentralDavs.add(new DavInformation(centralDavId, connectionDavId, userId, throughputResistance));
				}
			}
		}
	}

	/**
	 * Gibt alle Senderanmeldungen zurück, die für die angefragte Datenidentifikation bestehen
	 *
	 * @return alle Senderanmeldungen
	 */
	public List<ClientSendingSubscription> getSenderSubscriptions() {
		return Collections.unmodifiableList(_senderSubscriptions);
	}

	/**
	 * Gibt alle Empfängeranmeldungen zurück, die für die angefragte Datenidentifikation bestehen
	 *
	 * @return alle Empfängeranmeldungen
	 */
	public List<ClientReceivingSubscription> getReceiverSubscriptions() {
		return Collections.unmodifiableList(_receiverSubscriptions);
	}

	/**
	 * Gibt eine Liste mit den potentiellen Zentraldatenverteilern zurück
	 *
	 * @return eine Liste mit den potentiellen Zentraldatenverteilern
	 */
	public List<DavInformation> getPotentialCentralDavs() {
		return Collections.unmodifiableList(_potentialCentralDavs);
	}

	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("Ankommende Anmeldungen:\n");
		for(ClientReceivingSubscription receiverSubscription : _receiverSubscriptions) {
			stringBuilder.append(receiverSubscription.toString()).append("\n");
		}
		stringBuilder.append("Ausgehende Anmeldungen:\n");
		for(ClientSendingSubscription sendingSubscription : _senderSubscriptions) {
			stringBuilder.append(sendingSubscription.toString()).append("\n");
		}
		return stringBuilder.toString();
	}

	/**
	 * Diese Klasse enthält Informationen über eine sendende Anmeldung (Quelle oder Sender) einer Datenidentifikation
	 */
	public class ClientSendingSubscription {

		private final SystemObject _application;

		private final boolean _local;

		private final long _applicationId;

		private final long _userId;

		private final boolean _source;

		private final boolean _requestSupported;

		private final SubscriptionState _state;

		private final ClientConnectionState _connectionState;

		private final SystemObject _user;

		ClientSendingSubscription(
				final boolean isLocal,
				final long applicationId,
				final long userId,
				final boolean source,
				final boolean requestSupported,
				final int state,
				final int conState) {
			_local = isLocal;
			_applicationId = applicationId;
			_userId = userId;
			_source = source;
			_requestSupported = requestSupported;
			_application = _connection.getDataModel().getObject(applicationId);
			_user = _connection.getDataModel().getObject(userId);
			switch(state) {
				case 1:
					_state = SubscriptionState.ReceiversAvailable;
					break;
				case 2:
					_state = SubscriptionState.NoReceiversAvailable;
					break;
				case 3:
					_state = SubscriptionState.Waiting;
					break;
				case 4:
					_state = SubscriptionState.NotAllowed;
					break;
				case 6:
					_state = SubscriptionState.NotResponsible;
					break;
				case 7:
					_state = SubscriptionState.MultiRemoteLock;
					break;
				default:
					_state = SubscriptionState.InvalidSubscription;
			}
			_connectionState = ClientConnectionState.values()[conState];
		}

		/**
		 * Gibt das Programm, das die Anmeldung durchgeführt hat, zurück. Das zurückgegebene Objekt muss nicht vom Typ `typ.applikation` sein,
		 * sondern kann beispielsweise auch ein Datenverteiler sein.
		 *
		 * @return das Programm, das die Anmeldung durchgeführt hat oder null falls nicht ermittelbar
		 */
		public SystemObject getApplication() {
			return _application;
		}

		/**
		 * Gibt den Benutzer zurück, unter dem die Anmeldung durchgeführt wurde
		 *
		 * @return Benutzerobjekt oder null.
		 */
		public SystemObject getUser() {
			return _user;
		}

		/**
		 * Gibt <tt>true</tt> zurück, wenn es sich um eine Quellanmeldung handelt
		 *
		 * @return <tt>true</tt>, wenn es sich um eine Quellanmeldung handelt, sonst (bei Sender) <tt>false</tt>
		 */
		public boolean isSource() {
			return _source;
		}

		/**
		 * Gibt <tt>true</tt> zurück, wenn der Sender bzw. die Quelle Sendesteuerung unterstützt
		 *
		 * @return <tt>true</tt>, wenn der Sender bzw. die Quelle Sendesteuerung unterstützt, sonst <tt>false</tt>
		 */
		public boolean isRequestSupported() {
			return _requestSupported;
		}

		/**
		 * Gibt den Anmeldestatus zurück. Es handelt sich um die interne Information im Datenverteiler, die den Zustand der Anmeldung beschreibt.
		 *
		 * @return den Anmeldestatus
		 */
		public SubscriptionState getState() {
			return _state;
		}

		/**
		 * Gibt den Verbindungsstatus zurück. Enthält Informationen über die Kommunikation mit dem verbundenen Programm.
		 *
		 * @return den Verbindungsstatus
		 */
		public ClientConnectionState getConnectionState() {
			return _connectionState;
		}

		/**
		 * Gibt die ID der anmeldenden Applikation zurück
		 *
		 * @return die ID der anmeldenden Applikation, 0 falls Anmeldung durch lokalen Datenverteiler o.ä. mit Systemrechten erfolgte
		 */
		public long getApplicationId() {
			return _applicationId;
		}

		/**
		 * Gibt die ID des anmeldenden Benutzers zurück
		 *
		 * @return die ID des anmeldenden Benutzers, 0 falls Anmeldung durch lokalen Datenverteiler o.ä. mit Systemrechten erfolgte
		 */
		public long getUserId() {
			return _userId;
		}

		@Override
		public String toString() {
			return getApplicationPidOrId();
		}

		/**
		 * Gibt Applikationsnamen und Pid zurück falls bekannt, sonst die ID.
		 *
		 * @return Formatierte Applikation, genaues Format kann sich ändern
		 */
		public String getApplicationPidOrId() {
			return _application == null ? "[" + _applicationId + "]" : _application.toString();
		}

		/**
		 * Gibt Benutzernamen und Pid zurück falls bekannt, sonst die ID.
		 *
		 * @return Formatierter Benutzer, genaues Format kann sich ändern
		 */
		public String getUserPidOrId() {
			return _user == null ? "[" + _userId + "]" : _user.toString();
		}

		/**
		 * Gibt <tt>true</tt> zurück, wenn es sich um eine lokale Anmeldung handelt
		 *
		 * @return <tt>true</tt>, wenn es sich um eine lokale Anmeldung handelt, sonst <tt>false</tt>
		 */
		public boolean isLocal() {
			return _local;
		}
	}

	/**
	 * Diese Klasse enthält Informationen über eine empfangende Anmeldung (Empfänger oder Senke) einer Datenidentifikation
	 */
	public class ClientReceivingSubscription {

		private final boolean _local;

		private final long _applicationId;

		private final long _userId;

		private final boolean _drain;

		private final boolean _delayed;

		private final boolean _delta;

		private final SystemObject _application;

		private final SubscriptionState _state;

		private final ClientConnectionState _connectionState;

		private final SystemObject _user;

		ClientReceivingSubscription(
				final boolean isLocal,
				final long applicationId,
				final long userId,
				final boolean drain,
				final boolean delayed,
				final boolean delta,
				final int state,
				final int conState) {
			_local = isLocal;
			_applicationId = applicationId;
			_userId = userId;
			_drain = drain;
			_delayed = delayed;
			_delta = delta;
			if(applicationId == 0) {
				_application = _connection.getDataModel().getConfigurationAuthority();
				_user = _connection.getDataModel().getConfigurationAuthority();
			}
			else {
				_application = _connection.getDataModel().getObject(applicationId);
				_user = _connection.getDataModel().getObject(userId);
			}
			switch(state) {
				case 1:
					_state = SubscriptionState.NoSendersAvailable;
					break;
				case 2:
					_state = SubscriptionState.SendersAvailable;
					break;
				case 3:
					_state = SubscriptionState.Waiting;
					break;
				case 4:
					_state = SubscriptionState.NotAllowed;
					break;
				case 6:
					_state = SubscriptionState.NotResponsible;
					break;
				case 7:
					_state = SubscriptionState.MultiRemoteLock;
					break;
				default:
					_state = SubscriptionState.InvalidSubscription;
			}
			_connectionState = ClientConnectionState.values()[conState];
		}

		/**
		 * Gibt <tt>true</tt> zurück, wenn es sich um eine lokale Anmeldung handelt
		 *
		 * @return <tt>true</tt>, wenn es sich um eine lokale Anmeldung handelt, sonst <tt>false</tt>
		 */
		public boolean isLocal() {
			return _local;
		}

		/**
		 * Gibt <tt>true</tt> zurück, wenn es sich um eine Senke handelt
		 *
		 * @return <tt>true</tt>, wenn es sich um eine Senke handelt, sonst (Empfänger) <tt>false</tt>
		 */
		public boolean isDrain() {
			return _drain;
		}

		/**
		 * Gibt <tt>true</tt> zurück, wenn nachgelieferte Daten empfangen werden sollen
		 *
		 * @return <tt>true</tt>, wenn nachgelieferte Daten empfangen werden sollen, sonst <tt>false</tt>
		 */
		public boolean isDelayed() {
			return _delayed;
		}

		/**
		 * Gibt <tt>true</tt> zurück, wenn es sich um eine Delta-Anmeldung handelt
		 *
		 * @return <tt>true</tt>, wenn es sich um eine Delta-Anmeldung handelt, sonst <tt>false</tt>
		 */
		public boolean isDelta() {
			return _delta;
		}

		/**
		 * Gibt das Programm, das die Anmeldung durchgeführt hat, zurück. Das zurückgegebene Objekt muss nicht vom Typ `typ.applikation` sein,
		 * sondern kann beispielsweise auch ein Datenverteiler sein.
		 *
		 * @return das Programm, das die Anmeldung durchgeführt hat oder null falls nicht ermittelbar
		 */
		public SystemObject getApplication() {
			return _application;
		}

		/**
		 * Gibt den Benutzer zurück, unter dem die Anmeldung durchgeführt wurde
		 *
		 * @return Benutzerobjekt oder null.
		 */
		public SystemObject getUser() {
			return _user;
		}

		/**
		 * Gibt den Anmeldestatus zurück. Es handelt sich um die interne Information im Datenverteiler, die den Zustand der Anmeldung beschreibt.
		 *
		 * @return den Anmeldestatus
		 */
		public SubscriptionState getState() {
			return _state;
		}

		/**
		 * Gibt den Verbindungsstatus zurück. Enthält Informationen über die Kommunikation mit dem verbundenen Programm.
		 *
		 * @return den Verbindungsstatus
		 */
		public ClientConnectionState getConnectionState() {
			return _connectionState;
		}

		/**
		 * Gibt die ID der anmeldenden Applikation zurück
		 *
		 * @return die ID der anmeldenden Applikation, 0 falls Anmeldung durch lokalen Datenverteiler o.ä. mit Systemrechten erfolgte
		 */
		public long getApplicationId() {
			return _applicationId;
		}

		/**
		 * Gibt die ID des anmeldenden Benutzers zurück
		 *
		 * @return die ID des anmeldenden Benutzers, 0 falls Anmeldung durch lokalen Datenverteiler o.ä. mit Systemrechten erfolgte
		 */
		public long getUserId() {
			return _userId;
		}

		@Override
		public String toString() {
			return getApplicationPidOrId();
		}

		/**
		 * Gibt Applikationsnamen und Pid zurück falls bekannt, sonst die ID.
		 *
		 * @return Formatierte Applikation, genaues Format kann sich ändern
		 */
		public String getApplicationPidOrId() {
			return _application == null ? "[" + _applicationId + "]" : _application.toString();
		}

		/**
		 * Gibt Benutzernamen und Pid zurück falls bekannt, sonst die ID.
		 *
		 * @return Formatierter Benutzer, genaues Format kann sich ändern
		 */
		public String getUserPidOrId() {
			return _user == null ? "[" + _userId + "]" : _user.toString();
		}
	}

	/**
	 * Informationen über einen potentiellen Zentraldatenverteiler
	 */
	public class DavInformation {
		private final long _centralDavId;
		private final long _connectionDavId;
		private final long _userId;
		private final int _throughputResistance;

		private DavInformation(final long centralDavId, final long connectionDavId, final long userId, final int throughputResistance) {
			_centralDavId = centralDavId;
			_connectionDavId = connectionDavId;
			_userId = userId;
			_throughputResistance = throughputResistance;
		}

		/** 
		 * Gibt die ID des potentiellen Zentraldatenverteilers zurück
		 * @return die ID
		 */
		public long getCentralDavId() {
			return _centralDavId;
		}

		/** 
		 * Gibt die ID des angeschlossenen Datenverteilers zurück, über den der potentielle Zentraldatenverteiler (derzeit am besten) erreichbar ist
		 * @return die ID des Proxy-Datenverteilers
		 */
		public long getConnectionDavId() {
			return _connectionDavId;
		}

		/** 
		 * Gibt das Datenverteilerobjekt des potentiellen Zentraldatenverteilers zurück
		 * @return das Datenverteilerobjekt des potentiellen Zentraldatenverteilers
		 */
		public DavApplication getCentralDav() {
			return (DavApplication) _connection.getDataModel().getObject(_centralDavId);
		}

		/** 
		 * Gibt das Datenverteilerobjekt des Proxy-Datenverteilers zurück
		 * @return das Datenverteilerobjekt des Proxy-Datenverteilers
		 */
		public DavApplication getConnectionDav() {
			return (DavApplication) _connection.getDataModel().getObject(_connectionDavId);
		}

		/** 
		 * Gibt die Benutzer-ID für die Anmeldung am Proxy-Datenverteiler zurück
		 * @return die Benutzer-ID
		 */
		public long getUserId() {
			return _userId;
		}

		/** 
		 * Gibt den Benutzer zum Proxy-Datenverteiler zurück
		 * @return den Benutzer
		 */
		public SystemObject getUser() {
			return _connection.getDataModel().getObject(_userId);
		}

		/**
		 * Gibt den Widerstand/die Gewichtung dieser (direkten) Verbindung zurück. Wird bei der Bestimmung der besten Wege verwendet.
		 * @return Positive-Integer-Zahl. Je größer die Zahl, desto eher werden andere Routen mit kleiner Zahl bevorzugt.
		 */
		public int getThroughputResistance() {
			return _throughputResistance;
		}

		@Override
		public String toString() {
			return getCentralDavPidOrId();
		}

		/**
		 * Formatierte Rückgabe des potentiellen Zentraldatenverteilers
		 * @return Genaue String-Ausgabe ist Änderungen vorbehalten
		 */
		public String getCentralDavPidOrId() {
			DavApplication centralDav = getCentralDav();
			return centralDav == null ? "[" + _centralDavId + "]" : centralDav.toString();
		}

		/**
		 * Formatierte Rückgabe des Proxy-Datenverteilers
		 * @return Genaue String-Ausgabe ist Änderungen vorbehalten
		 */
		public String getConnectionDavPidOrId() {
			DavApplication connectionDav = getConnectionDav();
			return connectionDav == null ? "[" + _connectionDavId + "]" : connectionDav.toString();
		}

		/**
		 * Formatierte Rückgabe des Benutzers
		 * @return Genaue String-Ausgabe ist Änderungen vorbehalten
		 */
		public String getUserPidOrId() {
			SystemObject user = getUser();
			return user == null ? "[" + _userId + "]" : user.toString();
		}
	}
}
