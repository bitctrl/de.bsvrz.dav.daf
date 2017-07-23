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

package de.bsvrz.dav.daf.communication.protocol;

/**
 * Authentifizierungsstatus einer Applikation, Dav-Dav-Verbindung bzw. eines Benutzers
 *
 * @author Kappich Systemberatung
 */
public abstract class UserLogin {
	private static final UserLogin NOT_AUTHENTICATED = new UserLogin(){

		@Override
		public long toLong() {
			return -1;
		}

		@Override
		public boolean isAuthenticated() {
			return false;
		}

		@Override
		public long getRemoteUserId() {
			throw new IllegalStateException("Nicht authentifiziert");
		}

		@Override
		public boolean isRegularUser() {
			return false;
		}

		@Override
		public boolean isSystemUser() {
			return false;
		}

		@Override
		public String toString() {
			return "Nicht authentifiziert";
		}
	};
	private static final UserLogin SYSTEM_USER = new UserLogin() {
		@Override
		public long toLong() {
			return 0;
		}

		@Override
		public boolean isAuthenticated() {
			return true;
		}

		@Override
		public long getRemoteUserId() {
			throw new IllegalStateException("Systembenutzer besitzt keine ID");
		}

		@Override
		public boolean isRegularUser() {
			return false;
		}

		@Override
		public boolean isSystemUser() {
			return true;
		}

		@Override
		public String toString() {
			return "Systembenutzer";
		}
	};

	private static class StandardUser extends UserLogin {
		private final long _remoteUserId;

		public StandardUser(final long remoteUserId) {
			_remoteUserId = remoteUserId;
		}

		@Override
		public long toLong() {
			return _remoteUserId;
		}

		@Override
		public boolean isAuthenticated() {
			return true;
		}

		@Override
		public long getRemoteUserId() {
			return _remoteUserId;
		}

		@Override
		public boolean isRegularUser() {
			return true;
		}

		@Override
		public boolean isSystemUser() {
			return false;
		}

		@Override
		public boolean equals(final Object o) {
			if(this == o) return true;
			if(o == null || getClass() != o.getClass()) return false;

			final StandardUser that = (StandardUser) o;

			return _remoteUserId == that._remoteUserId;

		}

		@Override
		public int hashCode() {
			return (int) (_remoteUserId ^ (_remoteUserId >>> 32));
		}

		@Override
		public String toString() {
			return "Benutzer: " + _remoteUserId;
		}
	}

	/**
	 * Gibt die Instanz zurück, die angibt, dass der Benutzer (noch) nicht authentifiziert ist
	 * @return Nicht-Authentifiziert-Instanz
	 */
	public static UserLogin notAuthenticated() {
		return NOT_AUTHENTICATED;
	}

	/**
	 * Gibt die Instanz zurück, die angibt, dass der Benutzer ein Systembenutzer ist, und keine Rechteprüfung durchgeführt wird
	 * @return Systembenutzer-Instanz
	 */
	public static UserLogin systemUser() {
		return SYSTEM_USER;
	}

	/**
	 * Gibt die Instanz zurück, die angibt, dass es sich um einen normalen Benutzer handelt
	 * @param remoteUserId Benutzer-ID
	 * @return Instanz
	 */
	public static UserLogin user(final long remoteUserId) {
		if(remoteUserId <= 0) throw new IllegalArgumentException("Ungültige ID: " + remoteUserId);
		return new StandardUser(remoteUserId);
	}

	/**
	 * Für Serialisierungszwecke kann ein Long in eine Instanz dieses Objekts umgewandelt werden
	 * @param l Long
	 * @return Instanz
	 */
	public static UserLogin ofLong(long l){
		if(l == -1) return notAuthenticated();
		if(l == 0) return systemUser();
		return user(l);
	}

	/** 
	 * Gibt <tt>true</tt> zurück, wenn der Benutzer erfolgreich authentifiziert wurde
	 * @return <tt>true</tt>, wenn der Benutzer erfolgreich authentifiziert wurde, sonst <tt>false</tt>
	 */
	public abstract boolean isAuthenticated();

	/** 
	 * Gibt die ID des Benutzerobjekts zurück
	 * @return die ID des Benutzerobjekts
	 * @throws IllegalStateException Wenn Benutzer nicht authentifiziert ist oder er keine ID besitzt (Systembenutzer)
	 */
	public abstract long getRemoteUserId();

	/** 
	 * Gibt <tt>true</tt> zurück, wenn es sich um einen normalen Benutzer aus der Konfiguration handelt
	 * @return <tt>true</tt>, wenn es sich um einen normalen Benutzer aus der Konfiguration handelt, sonst <tt>false</tt>
	 */
	public abstract boolean isRegularUser();
	
	/** 
	 * Gibt <tt>true</tt> zurück, wenn es sich um einen speziellen Systembenutzer handelt. 
	 * Dies ist beispielsweise der Benutzer, mit die lokale Datenverteilerverbindung und die Konfiguration
	 * sich beim Datenverteiler authentifiziert. Für Systembenutzer wird keine Rechteprüfung durchgeführt.
	 * 
	 * @return <tt>true</tt>, wenn es sich um einen speziellen Systembenutzer handelt, sonst <tt>false</tt>
	 */
	public abstract boolean isSystemUser();

	/**
	 * Konvertiert dieses Objekt für Serialisierungszwecke in ein Long
	 * @return Long-Wert
	 */
	public abstract long toLong();



}
