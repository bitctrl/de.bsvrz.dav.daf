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
import de.bsvrz.dav.daf.main.config.ClientApplication;
import de.bsvrz.dav.daf.main.config.DavApplication;
import de.bsvrz.dav.daf.main.config.SystemObject;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Diese Klasse gibt die Anmeldungen einer Applikation am Datenverteiler zurück.
 *
 * @author Kappich Systemberatung
 * @version $Revision: 11467 $
 * @see ClientDavInterface#getSubscriptionInfo(DavApplication, ClientApplication)
 */
public class ApplicationSubscriptionInfo {

	private final ClientDavConnection _connection;

	private final List<ApplicationSendingSubscription> _senderSubscriptions = new ArrayList<ApplicationSendingSubscription>();

	private final List<ApplicationReceivingSubscription> _receiverSubscriptions = new ArrayList<ApplicationReceivingSubscription>();

	ApplicationSubscriptionInfo(final ClientDavConnection connection, final byte[] bytes) throws IOException {
		_connection = connection;
		final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
		try(DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream)) {
			final int numReceivingSubscriptions = dataInputStream.readInt();
			for(int i = 0; i < numReceivingSubscriptions; i++) {
				final long objectId = dataInputStream.readLong();
				final long usageId = dataInputStream.readLong();
				final short simVar = dataInputStream.readShort();
				final boolean isSource = dataInputStream.readBoolean();
				final boolean isRequestSupported = dataInputStream.readBoolean();
				final int state = dataInputStream.readInt();
				final ApplicationSendingSubscription applicationSendingSubscription =
						new ApplicationSendingSubscription(
								objectId,
								usageId,
								simVar,
								isSource,
								isRequestSupported,
								state
						);
				_senderSubscriptions.add(applicationSendingSubscription);
			}
			final int numSendingSubscriptions = dataInputStream.readInt();
			for(int i = 0; i < numSendingSubscriptions; i++) {
				final long objectId = dataInputStream.readLong();
				final long usageId = dataInputStream.readLong();
				final short simVar = dataInputStream.readShort();
				final boolean isDrain = dataInputStream.readBoolean();
				final boolean isDelayed = dataInputStream.readBoolean();
				final boolean isDelta = dataInputStream.readBoolean();
				final int state = dataInputStream.readInt();
				final ApplicationReceivingSubscription applicationReceivingSubscription =
						new ApplicationReceivingSubscription(
								objectId,
								usageId,
								simVar,
								isDrain,
								isDelayed,
								isDelta,
								state
						);
				_receiverSubscriptions.add(applicationReceivingSubscription);
			}
		}
		_senderSubscriptions.sort(Comparator.<ApplicationSendingSubscription, String>comparing(it -> it.getUsage().getAttributeGroup().getNameOrPidOrId())
				                          .thenComparing(it -> it.getUsage().getAspect().getNameOrPidOrId())
				                          .thenComparing(it -> it.getObject().getNameOrPidOrId()));
		_receiverSubscriptions.sort(Comparator.<ApplicationReceivingSubscription, String>comparing(it -> it.getUsage().getAttributeGroup().getNameOrPidOrId())
				                            .thenComparing(it -> it.getUsage().getAspect().getNameOrPidOrId())
				                            .thenComparing(it -> it.getObject().getNameOrPidOrId()));
	}

	/**
	 * Gibt alle sendenden Anmeldungen (Sender und Quellen) zurück
	 *
	 * @return alle sendenden Anmeldungen
	 */
	public List<ApplicationSendingSubscription> getSenderSubscriptions() {
		return Collections.unmodifiableList(_senderSubscriptions);
	}

	/**
	 * Gibt alle empfangenden Anmeldungen (Senken und Empfänger) zurück
	 *
	 * @return alle empfangenden Anmeldungen
	 */
	public List<ApplicationReceivingSubscription> getReceiverSubscriptions() {
		return Collections.unmodifiableList(_receiverSubscriptions);
	}

	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("Ankommende Anmeldungen:\n");
		for(ApplicationReceivingSubscription receiverSubscription : _receiverSubscriptions) {
			stringBuilder.append(receiverSubscription.toString()).append("\n");
		}
		stringBuilder.append("Ausgehende Anmeldungen:\n");
		for(ApplicationSendingSubscription sendingSubscription : _senderSubscriptions) {
			stringBuilder.append(sendingSubscription.toString()).append("\n");
		}
		return stringBuilder.toString();
	}

	/**
	 * Informationen über eine Anmeldung, die Daten sendet
	 */
	public final class ApplicationSendingSubscription {

		private final boolean _source;

		private final boolean _requestSupported;

		private final SubscriptionState _state;

		private final long _objectId;

		private final long _usageId;

		private final short _simVar;

		private final SystemObject _object;

		private final AttributeGroupUsage _usage;

		ApplicationSendingSubscription(
				final long objectId, final long usageId, final short simVar, final boolean source, final boolean requestSupported, final int state) {
			_objectId = objectId;
			_usageId = usageId;
			_simVar = simVar;

			_object = _connection.getDataModel().getObject(objectId);
			_usage = _connection.getDataModel().getAttributeGroupUsage(usageId);

			_source = source;
			_requestSupported = requestSupported;
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
		}

		/**
		 * Gibt die Objekt-ID zurück, für das Daten angemeldet wurden
		 *
		 * @return die Objekt-ID
		 */
		public long getObjectId() {
			return _objectId;
		}

		/**
		 * Gibt die Attributgruppenverwendung zurück, für die Daten angemeldet wurden
		 *
		 * @return die Attributgruppenverwendung
		 */
		public long getUsageId() {
			return _usageId;
		}

		/**
		 * Gibt die Simulationsvariante der Anmeldung zurück
		 *
		 * @return die Simulationsvariante der Anmeldung
		 */
		public short getSimVar() {
			return _simVar;
		}

		/**
		 * Gibt das Objekt zurück, falls bekannt
		 *
		 * @return das Objekt oder null
		 */
		public SystemObject getObject() {
			return _object;
		}

		/**
		 * Gibt die Attributgruppenverwendung zurück, falls bekannt
		 *
		 * @return die Attributgruppenverwendung oder null
		 */
		public AttributeGroupUsage getUsage() {
			return _usage;
		}

		/**
		 * Gibt <tt>true</tt> zurück, wenn es sich um eine Quelle handelt
		 *
		 * @return <tt>true</tt>, wenn es sich um eine Quelle handelt, sonst (Sender) <tt>false</tt>
		 */
		public boolean isSource() {
			return _source;
		}

		/**
		 * Gibt <tt>true</tt> zurück, wenn Sendesteuerung benutzt wird
		 *
		 * @return <tt>true</tt>, wenn Sendesteuerung benutzt wird, sonst <tt>false</tt>
		 */
		public boolean isRequestSupported() {
			return _requestSupported;
		}

		/**
		 * Gibt den Zustand der Anmeldung zurück
		 *
		 * @return den Zustand
		 */
		public SubscriptionState getState() {
			return _state;
		}

		@Override
		public String toString() {
			return _usage.getAttributeGroup().getPidOrNameOrId() + ":" + _usage.getAspect().getPidOrNameOrId() + ":" + _object.getPidOrNameOrId();
		}
	}

	/**
	 * Informationen über eine Anmeldung, die Daten empfängt
	 */
	public final class ApplicationReceivingSubscription {

		private final boolean _drain;

		private final boolean _delayed;

		private final boolean _delta;

		private final SubscriptionState _state;

		private final long _objectId;

		private final long _usageId;

		private final short _simVar;

		private final SystemObject _object;

		private final AttributeGroupUsage _usage;

		ApplicationReceivingSubscription(
				final long objectId,
				final long usageId,
				final short simVar,
				final boolean drain,
				final boolean delayed,
				final boolean delta,
				final int state) {
			_objectId = objectId;
			_usageId = usageId;
			_simVar = simVar;

			_object = _connection.getDataModel().getObject(objectId);
			_usage = _connection.getDataModel().getAttributeGroupUsage(usageId);
			_drain = drain;
			_delayed = delayed;
			_delta = delta;
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
		 * Gibt <tt>true</tt> zurück, wenn nachgelieferte Daten angefordert wurden
		 *
		 * @return <tt>true</tt>, wenn nachgelieferte Daten angefordert wurden, sonst <tt>false</tt>
		 */
		public boolean isDelayed() {
			return _delayed;
		}

		/**
		 * Gibt <tt>true</tt> zurück, wenn eine Delta-Anmeldung durchgeführt wurde
		 *
		 * @return <tt>true</tt>, wenn eine Delta-Anmeldung durchgeführt wurde, sonst <tt>false</tt>
		 */
		public boolean isDelta() {
			return _delta;
		}

		/**
		 * Gibt den Zustand der Anmeldung zurück
		 *
		 * @return den Zustand
		 */
		public SubscriptionState getState() {
			return _state;
		}

		@Override
		public String toString() {
			return _usage.getAttributeGroup().getPidOrNameOrId() + ":" + _usage.getAspect().getPidOrNameOrId() + ":" + _object.getPidOrNameOrId();
		}

		/**
		 * Gibt die Objekt-ID zurück, für das Daten angemeldet wurden
		 *
		 * @return die Objekt-ID
		 */
		public long getObjectId() {
			return _objectId;
		}

		/**
		 * Gibt die Attributgruppenverwendung zurück, für die Daten angemeldet wurden
		 *
		 * @return die Attributgruppenverwendung
		 */
		public long getUsageId() {
			return _usageId;
		}

		/**
		 * Gibt die Simulationsvariante der Anmeldung zurück
		 *
		 * @return die Simulationsvariante der Anmeldung
		 */
		public short getSimVar() {
			return _simVar;
		}

		/**
		 * Gibt das Objekt zurück, falls bekannt
		 *
		 * @return das Objekt oder null
		 */
		public SystemObject getObject() {
			return _object;
		}

		/**
		 * Gibt die Attributgruppenverwendung zurück, falls bekannt
		 *
		 * @return die Attributgruppenverwendung oder null
		 */
		public AttributeGroupUsage getUsage() {
			return _usage;
		}
	}
}
