/*
 * Copyright 2007 by Kappich Systemberatung Aachen 
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

package de.bsvrz.dav.daf.main.config.management;

import de.bsvrz.dav.daf.communication.srpAuthentication.SrpClientAuthentication;
import de.bsvrz.dav.daf.communication.srpAuthentication.SrpNotSupportedException;
import de.bsvrz.dav.daf.communication.srpAuthentication.SrpVerifierAndUser;
import de.bsvrz.dav.daf.communication.srpAuthentication.SrpVerifierData;
import de.bsvrz.dav.daf.main.DataAndATGUsageInformation;
import de.bsvrz.dav.daf.main.config.ConfigurationTaskException;
import de.bsvrz.dav.daf.main.config.MutableCollectionChangeListener;
import de.bsvrz.dav.daf.main.config.SystemObject;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;

/**
 * Die Implementation dieses Interfaces erlaubt es, die Benutzer der Kernsoftware zu verwalten. Dies beinhaltet Aktionen wie:<br> <ul> <li>Anlegen neuer
 * Benutzer</li> <li>Ändern von Passwörtern</li> <li>Ändern der Rechte, die ein Benutzer besitzt</li> <li>Erstellung von Einmal-Passwörtern</li> </ul>
 * <p>
 * Alle beschriebenen Aktionen setzen dabei die nötigen Rechte des Benutzers voraus, der die Aktion auslöst. Sind die nötigen Rechte nicht vorhanden, so wird
 * die Aktion nicht durchgeführt.
 *
 * @author Kappich Systemberatung
 * @version $Revision$
 */
public interface UserAdministration {

	/**
	 * Beauftragt die Konfiguration ein neues Benutzerkonto anzulegen. Zu einem Benutzerkonto gehören ein Benutzername, ein Passwort und die Rechte des Benutzers.
	 * <p>
	 * Ein neuer Benutzer kann nur durch einen Benutzer angelegt werden, der die Rechte eines Administrators besitzt. Besitzt der Benutzer diese Rechte nicht, wird
	 * der Auftrag zur Konfiguration übertragen und dort abgelehnt.
	 *
	 * @param orderer              Benutzername des Auftraggebers.
	 * @param ordererPassword      Passwort des Auftraggebers.
	 * @param newUsername          Benutzername des neuen Benutzers.
	 * @param newUserPid           Pid des neuen Benutzers. Wird der Leerstring ("") übergeben, wird dem Benutzer keine explizite Pid zugewiesen.
	 * @param newPassword          Passwort des neuen Benutzers.
	 * @param adminRights          <code>true</code>, wenn der neue Benutzer die Rechte eines Administrators besitzen soll; <code>false</code>, wenn der Benutzer
	 *                             keine speziellen Rechte besitzen soll.
	 * @param pidConfigurationArea Pid des Konfigurationsbereichs, in dem der neue Benutzer angelegt werden soll.
	 *
	 * @throws de.bsvrz.dav.daf.main.config.ConfigurationTaskException
	 *          Die Konfiguration kann den Auftrag nicht ausführen, weil die übergebenen Parameter falsch sind. So kann der Auftraggeber zum Beispiel nicht die
	 *          nötigen Rechte besitzen einen neuen Benutzer anzulegen.
	 */
	public void createNewUser(
			String orderer, String ordererPassword, String newUsername, String newUserPid, String newPassword, boolean adminRights, String pidConfigurationArea
	) throws ConfigurationTaskException;

	/**
	 * Beauftragt die Konfiguration ein neues Benutzerkonto anzulegen. Zu einem Benutzerkonto gehören ein Benutzername, ein Passwort und die Rechte des
	 * Benutzers.
	 * <p>
	 * Ein neuer Benutzer kann nur durch einen Benutzer angelegt werden, der die Rechte eines Administrators besitzt. Besitzt der Benutzer diese Rechte nicht,
	 * wird der Auftrag zur Konfiguration übertragen und dort abgelehnt.
	 *
	 * @param orderer              Benutzername des Auftraggebers.
	 * @param ordererPassword      Passwort des Auftraggebers.
	 * @param newUsername          Benutzername des neuen Benutzers.
	 * @param newUserPid           Pid des neuen Benutzers. Wird der Leerstring ("") übergeben, wird dem Benutzer keine explizite Pid zugewiesen.
	 * @param newPassword          Passwort des neuen Benutzers.
	 * @param adminRights          <code>true</code>, wenn der neue Benutzer die Rechte eines Administrators besitzen soll; <code>false</code>, wenn der Benutzer
	 *                             keine speziellen Rechte besitzen soll.
	 * @param pidConfigurationArea Pid des Konfigurationsbereichs, in dem der neue Benutzer angelegt werden soll.
	 * @param data                 Konfigurierende Datensätze mit den dazugehörigen Attributgruppenverwendungen, die für das neue Benutzer-Objekt gespeichert
	 *                             werden sollen. Oder <code>null</code> falls keine Datensätze angelegt werden sollen.
	 *
	 * @see de.bsvrz.dav.daf.main.config.ConfigurationArea#createDynamicObject(de.bsvrz.dav.daf.main.config.DynamicObjectType, String, String, java.util.Collection)
	 *
	 * @throws de.bsvrz.dav.daf.main.config.ConfigurationTaskException
	 *          Die Konfiguration kann den Auftrag nicht ausführen, weil die übergebenen Parameter falsch sind. So kann der Auftraggeber zum Beispiel nicht die
	 *          nötigen Rechte besitzen einen neuen Benutzer anzulegen.
	 */
	public void createNewUser(
			String orderer,
			String ordererPassword,
			String newUsername,
			String newUserPid,
			String newPassword,
			boolean adminRights,
			String pidConfigurationArea,
			Collection<DataAndATGUsageInformation> data) throws ConfigurationTaskException;

	/**
	 * Beauftragt die Konfiguration ein Benutzerkonto zu löschen. Alle Informationen, die zu einem Benutzerkonto gehören, sind mit dem Passwort des
	 * Auftraggebers zu verschlüsseln und verschlüsselt zu übertragen.
	 * <p>
	 * Ein Benutzerkonto kann nur durch einen Benutzer gelöscht werden, der die Rechte eines Administrators besitzt.
	 *
	 * @param orderer              Benutzername des Auftraggebers.
	 * @param ordererPassword      Passwort des Auftraggebers.
	 * @param userToDelete         Benutzername des zu löschenden Benutzers.
	 *
	 * @throws de.bsvrz.dav.daf.main.config.ConfigurationTaskException
	 *          Die Konfiguration kann den Auftrag nicht ausführen.
	 */
	public void deleteUser(
			String orderer, String ordererPassword, String userToDelete) throws ConfigurationTaskException;

	/**
	 * Prüft, ob ein angegebener Benutzer Admin-Rechte besitzt. Jeder Benutzer kann diese Aktion ausführen.
	 *
	 * @param orderer              Benutzername des Auftraggebers.
	 * @param ordererPassword      Passwort des Auftraggebers.
	 * @param username             Name des zu prüfenden Benutzers
	 *
	 * @return true falls der Benutzer Admin-Rechte hat
	 *
	 * @throws de.bsvrz.dav.daf.main.config.ConfigurationTaskException
	 *          Die Konfiguration kann den Auftrag nicht ausführen.
	 */
	public boolean isUserAdmin(String orderer, String ordererPassword, String username) throws ConfigurationTaskException;
	
	/**
	 * Beauftragt die Konfiguration die Rechte eines Benutzers zu ändern.
	 * <p>
	 * Nur ein Benutzer mit den Rechten eines Administrators darf die Rechte anderer Benutzer ändern. Besitzt ein Benutzer diese Rechte nicht wird der Auftrag an
	 * die Konfiguration verschickt und dort von der Konfiguration abgelehnt.
	 *
	 * @param orderer         Auftraggeber, der die Rechte eines Benutzers ändern möchte.
	 * @param ordererPassword Passwort des Auftraggebers.
	 * @param user            Benutzer, dessen Rechte geändert werden sollen.
	 * @param adminRights     <code>true</code>, wenn der Benutzer, der mit <code>user</code> identifiziert wird, die Rechte eines Administrators erhalten soll;
	 *                        <code>false</code>, wenn der Benutzer, der mit <code>user</code> identifiziert wird, keine speziellen Rechte erhalten soll.
	 *
	 * @throws de.bsvrz.dav.daf.main.config.ConfigurationTaskException Die Konfiguration kann den Auftrag nicht ausführen, weil die übergebenen Parameter falsch sind. So kann der Auftraggeber
	 *                                    zum Beispiel nicht die nötigen Rechte besitzen die Rechte eines anderen Benutzers zu ändern.
	 */
	public void changeUserRights(String orderer, String ordererPassword, String user, boolean adminRights) throws ConfigurationTaskException;

	/**
	 * Beauftragt die Konfiguration das Passwort eines Benutzers zu ändern. Diese Methode kann von jedem Benutzer aufgerufen werden. Es ist jedem Benutzer
	 * gestattet das Passwort seines Benutzerkontos zu ändern. Soll das Passwort eines fremden Benutzerkontos geändert werden, sind die Rechte eines Administrators
	 * nötig.
	 * <p>
	 *
	 * @param orderer         Benutzername des Auftraggebers
	 * @param ordererPassword Derzeit gültiges Passwort, falls der Benutzername <code>orderer</code> und <code>user</code> identisch sind. Sind die Parameter nicht
	 *                        identisch, muss der Benutzer, der mit <code>orderer</code> identifiziert wird, die Rechte eines Administrators besitzen und sein
	 *                        Passwort übergeben
	 * @param user            Benutzername des Benutzerkontos, dessen Passwort geändert werden soll
	 * @param newUserPassword Neues Passwort des Benutzers, der mit <code>user</code> identifiziert wurde
	 *
	 * @throws ConfigurationTaskException Die Konfiguration kann den Auftrag nicht ausführen, weil die übergebenen Parameter falsch sind. So kann der Auftraggeber
	 *                                    zum Beispiel nicht die nötigen Rechte besitzen das Passwort eines anderen Benutzers zu ändern oder das Passwort zum
	 *                                    ändern ist falsch.
	 */
	public void changeUserPassword(String orderer, String ordererPassword, String user, String newUserPassword) throws ConfigurationTaskException;

	/**
	 * Setzt den SRP-Überprüfungscode des angegebenen Benutzers. Diese Methode tut dasselbe wie {@link #changeUserPassword(String, String, String, String)},
	 * mit dem Unterschied, das `changeUserPassword` den Überprüfungscode anhand des gegebenen Passworts berechnet, während diese Methode einen bereits irgendwo berechneten
	 * Überprüfungscode an die Konfiguration überträgt
	 *
	 * @param orderer         Benutzername des Auftraggebers
	 * @param ordererPassword Derzeit gültiges Passwort, falls der Benutzername <code>orderer</code> und <code>user</code> identisch sind. Sind die Parameter nicht
	 *                        identisch, muss der Benutzer, der mit <code>orderer</code> identifiziert wird, die Rechte eines Administrators besitzen und sein
	 *                        Passwort übergeben
	 * @param user            Benutzername des Benutzerkontos, dessen Passwort geändert werden soll
	 * @param verifier          neuer Überprüfungscode für den angegebenen Benutzer, wird vo nder Konfiguration zur Überprüfung des Passworts benutzt, ohne dass diese selbst das Passwort wissen muss
	 * @throws ConfigurationTaskException Die Konfiguration kann den Auftrag nicht ausführen, weil die übergebenen Parameter falsch sind. So kann der Auftraggeber
	 *                                    zum Beispiel nicht die nötigen Rechte besitzen das Passwort eines anderen Benutzers zu ändern oder das Passwort zum
	 *                                    ändern ist falsch.
	 */
	public void setSrpVerifier(String orderer, String ordererPassword, String user, SrpVerifierData verifier) throws ConfigurationTaskException;

	/**
	 * Beauftragt die Konfiguration ein Einmal-Passwort zu erzeugen und es einem Benutzer zu zuordnen.
	 * <p>
	 * Damit dieser Auftrag ausgeführt werden kann, muss der Auftraggeber <code>orderer</code> die Rechte eines Administrators besitzen. Besitzt der Auftraggeber
	 * diese Rechte nicht, wird der Auftrag zwar zur Konfiguration übertragen, dort aber abgelehnt.
	 *
	 * @param orderer               Benutzername des Auftraggebers
	 * @param ordererPassword       Passwort des Auftraggebers.
	 * @param username              Benutzername, dem ein Einmal-Passwort hinzugefügt werden soll.
	 * @param singleServingPassword Einmal-Passwort das dem Benutzer, der mit <code>username</code> identifiziert wird, hinzugefügt wird.
	 *
	 * @throws de.bsvrz.dav.daf.main.config.ConfigurationTaskException Die Konfiguration kann den Auftrag nicht ausführen, weil die übergebenen Parameter falsch sind. So kann der Auftraggeber
	 *                                    zum Beispiel nicht die nötigen Rechte besitzen ein Einmal-Passwort anzulegen oder das Passwort existierte bereits, usw..
	 *                                    
	 * @deprecated Bei Verwendung von SRP bzw. verschlüsselten Logins wird systembedingt der Index des Einmalpassworts benötigt. Diesen gibt die Methode nicht zurück.
	 * Daher ist die Methode (zumindest ohne direkten Zugriff auf die benutzerverwaltung.xml) bei Verwendung von verschlüsselter Anmeldung nicht mehr sinnvoll nutzbar.
	 * Als Alternative gibt es die Methode {@link #createOneTimePasswords(String, String, String, String...)}, die allerdings eine aktuelle Konfiguration voraussetzt.
	 */
	@Deprecated
	public void createSingleServingPassword(String orderer, String ordererPassword, String username, String singleServingPassword)
			throws ConfigurationTaskException;

	/**
	 * Ermittelt die Anzahl der noch vorhandenen, gültigen Einmal-Passwörter. Jeder Benutzer kann diese Anzahl für seinen eigenen Account ermitteln, 
	 * für fremde Accounts sind Admin-Rechte notwendig.
	 *
	 * @param orderer               Benutzername des Auftraggebers
	 * @param ordererPassword       Passwort des Auftraggebers, dies wird zum verschlüsseln benutzt und darf nicht mit übertragen werden
	 * @param username              Benutzername, dessen Einmalpasswörter geprüft werden sollen.
	 *
	 * @return Anzahl der noch vorhandenen, gültigen Einmal-Passwörter
	 *
	 * @throws ConfigurationTaskException Die Konfiguration kann den Auftrag nicht ausführen.
	 * 
	 * @see #getValidOneTimePasswordIDs(String, String, String) 
	 */
	public int getSingleServingPasswordCount(String orderer, String ordererPassword, String username) throws ConfigurationTaskException;

	/**
	 * Löscht alle Einmalpasswörter für einen angegebenen Benutzer. Es ist jedem Benutzer
	 * gestattet die Passwörter seines eigenen Accounts zu löschen. Soll ein fremdes Benutzerkonto geändert werden, sind Admin-Rechte nötig.
	 *
	 * @param orderer         Benutzername des Auftraggebers
	 * @param ordererPassword Passwort des Auftraggebers, dies wird zum verschlüsseln benutzt und darf nicht mit übertragen werden
	 * @param username        Benutzername, dessen Einmalpasswörter gelöscht werden sollen.
	 *
	 * @throws ConfigurationTaskException Die Konfiguration kann den Auftrag nicht ausführen.
	 */
	public void clearSingleServingPasswords(String orderer, String ordererPassword, String username) throws ConfigurationTaskException;

	/**
	 * Prüft, ob ein angegebener Benutzername gültig ist, d.h. ob er ein zugeordnetes Systemobjekt und einen Eintrag in der benutzerverwaltung.xml hat. Jeder
	 * Benutzer kann diese Aktion ausführen. Zur (verschlüsselten) Übertragung des Vorgangs ist dennoch die Angabe eines gültigen Benutzernamens und Passworts
	 * notwendig. Mit dieser Funktion kann geprüft werden, ob die Benutzernamen, die {@link #subscribeUserChangeListener(de.bsvrz.dav.daf.main.config.MutableCollectionChangeListener)
	 * } liefert, tatsächlichen zu einem gültigen Benutzer gehören, da subscribeUserChangeListener nur die Systemobjekte berücksichtigt.
	 *
	 * @param orderer         Benutzername des Auftraggebers.
	 * @param ordererPassword Passwort des Auftraggebers.
	 * @param username        Name des zu prüfenden Benutzers
	 *
	 * @return true falls der Benutzer in der Konfiguration gespeichert ist
	 *
	 * @throws de.bsvrz.dav.daf.main.config.ConfigurationTaskException
	 *          Die Konfiguration kann den Auftrag nicht ausführen.
	 */
	public boolean isUserValid(String orderer, String ordererPassword, String username) throws ConfigurationTaskException;

	/**
	 * Erstellt einen Listener der Änderungen an den Benutzern überwacht. Gibt eine aktuelle Liste aller Benutzer zurück.
	 * @param listener Objekt, an das Rückmeldungen gesendet werden sollen. <code>null</code>, wenn nur die Liste der aktuellen Benutzer geholt werden soll.
	 * @return Liste der aktuell vorhandenen Benutzer. Es ist eventuell ratsam, mit isUserValid zu prüfen, ob die Benutzer tatsächlich in der
	 * benutzerverwaltung.xml abgelegt sind, da hier nur die SystemObjekte berücksichtigt werden.
	 */
	public List<SystemObject> subscribeUserChangeListener(MutableCollectionChangeListener listener);

	/**
	 * Entfernt den mit subscribeUserChangeListener erstellten Listener
	 * @param listener Objekt, and das keine Rückmeldungen mehr gesendet werden sollen.
	 */
	public void unsubscribeUserChangeListener(MutableCollectionChangeListener listener);

	/**
	 * Gibt den aktuellen Überprüfungscode zurück, mit dem eine Applikation (oder der Datenverteiler) eine SRP-Authentifizierung durchführen kann und damit
	 * feststellen kann, ob ein Benutzer sein Passwort kennt.
	 * <p>
	 * In der Antwort ist außerdem das Passwort-Salt enthalten, welches benötigt wird um (bei Kenntnis des Passworts) ein Authentifizierungs-Token für eine
	 * passwd-Datei zu erzeugen. Hierfür kann die Methode {@link SrpClientAuthentication#createLoginToken(de.bsvrz.dav.daf.communication.srpAuthentication.SrpVerifierData, java.lang.String, java.lang.String)}
	 * benutzt werden.
	 * <p>
	 * Es ist jedem Benutzer gestattet seinen eigenen Überprüfungscode abzufragen. Benutzer mit Admin-Rechten können auch fremde Überprüfungs-Codes abfragen. Zu
	 * beachten ist, dass es mit einem Überprüfungscode nur möglich ist, zu prüfen ob ein Passwort gültig ist. Es sind keine Rückschlüsse auf das
	 * Klartextpasswort oder auf den Authentifizierungs-Token x möglich.
	 * <p>
	 * Ist der Benutzer nicht vorhanden, ist die User-ID 0, aber die Konfiguration generiert dennoch einen "gefälschten" Verifier. Auf diese Weise kann ein
	 * Verwender dieser Klasse erreichen, dass nicht-eingeloggte Clients nicht einfach ausprobieren können, welche Benutzer existieren und welche nicht. Dieses
	 * Verhalten wird besonders bei der Authentifizierung am Datenverteiler benötigt, andere Anwender dieser Klasse können den gefälschten Verifier in der Regel
	 * ignorieren, außer es soll ein ähnliches Verhalten erreicht werden. Siehe hierzu z.B. {@link de.bsvrz.dav.daf.communication.srpAuthentication.SrpServerAuthentication#step1(String, BigInteger, BigInteger, boolean)}.
	 *
	 * @param orderer         Auftraggeber
	 * @param ordererPassword Passwort des Auftraggebers
	 * @param username        Benutzername des Benutzers, von dem der Überprüfungscode geholt werden soll.
	 * @param passwordIndex   Optional kann der Verifier eines Einmalpassworts mit dem angegebenen Index angefragt werden. 
	 *                           
	 * @return ID des Benutzers (oder 0) und die zugehörigen SRP-Daten
	 * @throws de.bsvrz.dav.daf.main.config.ConfigurationTaskException Die Konfiguration kann den Auftrag nicht ausführen. Insbesondere wird die spezifischere {@link SrpNotSupportedException}
	 * geworfen, wenn die Konfiguration SRP nicht unterstützt und aktualisiert werden muss.
	 */
	public SrpVerifierAndUser getSrpVerifier(String orderer, String ordererPassword, String username, final int passwordIndex) throws ConfigurationTaskException;
	
	/**
	 * Gibt den aktuellen Überprüfungscode zurück, mit dem eine Applikation (oder der Datenverteiler) eine SRP-Authentifizierung durchführen kann und damit
	 * feststellen kann, ob ein Benutzer sein Passwort kennt.
	 * <p>
	 * In der Antwort ist außerdem das Passwort-Salt enthalten, welches benötigt wird um (bei Kenntnis des Passworts) ein Authentifizierungs-Token für eine
	 * passwd-Datei zu erzeugen. Hierfür kann die Methode {@link SrpClientAuthentication#createLoginToken(de.bsvrz.dav.daf.communication.srpAuthentication.SrpVerifierData, java.lang.String, java.lang.String)}
	 * benutzt werden.
	 * <p>
	 * Es ist jedem Benutzer gestattet seinen eigenen Überprüfungscode abzufragen. Benutzer mit Admin-Rechten können auch fremde Überprüfungs-Codes abfragen. Zu
	 * beachten ist, dass es mit einem Überprüfungscode nur möglich ist, zu prüfen ob ein Passwort gültig ist. Es sind keine Rückschlüsse auf das
	 * Klartextpasswort oder auf den Token x möglich.
	 * <p>
	 * Ist der Benutzer nicht vorhanden, ist die User-ID 0, aber die Konfiguration generiert dennoch einen "gefälschten" Verifier. Auf diese Weise kann ein
	 * Verwender dieser Klasse erreichen, dass nicht-eingeloggte Clients nicht einfach ausprobieren können, welche Benutzer existieren und welche nicht. Dieses
	 * Verhalten wird besonders bei der Authentifizierung am Datenverteiler benötigt, andere Anwender dieser Klasse können den gefälschten Verifier in der Regel
	 * ignorieren, außer es soll ein ähnliches Verhalten erreicht werden. Siehe hierzu z.B. {@link de.bsvrz.dav.daf.communication.srpAuthentication.SrpServerAuthentication#step1(String, BigInteger, BigInteger, boolean)}.
	 *
	 * @param orderer         Auftraggeber
	 * @param ordererPassword Passwort des Auftraggebers
	 * @param username        Benutzername des Benutzers, von dem der Überprüfungscode geholt werden soll.
	 * @return ID des Benutzers (oder 0) und die zugehörigen SRP-Daten
	 * @throws de.bsvrz.dav.daf.main.config.ConfigurationTaskException Die Konfiguration kann den Auftrag nicht ausführen. Insbesondere wird die spezifischere {@link SrpNotSupportedException}
	 * geworfen, wenn die Konfiguration SRP nicht unterstützt und aktualisiert werden muss.
	 */
	public default SrpVerifierAndUser getSrpVerifier(String orderer, String ordererPassword, String username) throws ConfigurationTaskException {
		return getSrpVerifier(orderer, ordererPassword, username, -1);
	}

	/**
	 * Markiert ein einzelnes Einmalpasswort als ungültig. Es ist jedem Benutzer
	 * gestattet die Passwörter seines eigenen Accounts zu löschen. Soll ein fremdes Benutzerkonto geändert werden, sind Admin-Rechte nötig.
	 * @param orderer Auftraggeber
	 * @param ordererPassword Auftraggeber-Passwort
	 * @param username Benutzer, dessen Einmalpasswort als ungültig/benutzt markiert werden soll
	 * @param passwordIndex Index des Einmalpassworts (>= 0)
	 * @throws de.bsvrz.dav.daf.main.config.ConfigurationTaskException Die Konfiguration kann den Auftrag nicht ausführen (z.B. Login-Daten falsch oder die Konfiguration unterstützt den Auftrag nicht und muss aktualisiert werden).
	 */
	public void disableOneTimePassword(String orderer, String ordererPassword, String username, int passwordIndex)  throws ConfigurationTaskException;

	/**
	 * Fügt einem Benutzer eine Liste von Einmalpasswörtern hinzu. Die Einmalpasswörter werden in der Reihenfolge in der Konfiguration gespeichert, wie sie übergeben werden,
	 * d.h. das Passwort mit dem Array-Index 0 erhält von der Konfiguration den nächsten verfügbaren Index. Diesen gibt diese Methode zurück. Beispiel: Diese Methode gibt 9 zurück,
	 * dann erhält das erste übergebene Passwort den Index 9, das zweite den Index 10 usw. Will sich der Benutzer später mit einem Einmalpasswort einloggen, muss er
	 * den Passwort-Index mit einem Minus getrennt an seinen Benutzernamen anhängen, z.B. "Tester-19" für das Einmalpasswort mit Index 19 am Benutzer Tester.
	 *
	 * Damit dieser Auftrag ausgeführt werden kann, muss der Auftraggeber <code>orderer</code> die Rechte eines Administrators besitzen. Besitzt der Auftraggeber
	 * diese Rechte nicht, wird der Auftrag zwar zur Konfiguration übertragen, dort aber abgelehnt.
	 * 
	 * @param orderer Auftraggeber
	 * @param ordererPassword Passwort des Auftraggebers
	 * @param username Benutzername für den Passwörter angelegt werden sollen
	 * @param passwords Anzulegende Passwörter
	 * @throws de.bsvrz.dav.daf.main.config.ConfigurationTaskException Die Konfiguration kann den Auftrag nicht ausführen (z.B. Login-Daten falsch oder die Konfiguration unterstützt den Auftrag nicht und muss aktualisiert werden).
	 * @return Index des ersten eingefügten Passworts
	 */
	public int createOneTimePasswords(String orderer, String ordererPassword, String username, String... passwords) throws ConfigurationTaskException;

	/**
	 * Gibt von einem Benutzer die Indizes der noch unbenutzten, verwendbaren, Einmalpasswörter zurück. Die Länge des Arrays entspricht der Rückgabe von {@link #getSingleServingPasswordCount(String, String, String)}.
	 *
	 * Damit dieser Auftrag für einen fremden Benutzer ausgeführt werden kann, muss der Auftraggeber <code>orderer</code> die Rechte eines Administrators besitzen. Besitzt der Auftraggeber
	 * diese Rechte nicht, wird der Auftrag zwar zur Konfiguration übertragen, dort aber abgelehnt. Für den eigenen Benutzer ist die Anfrage in jedem Fall erlaubt.
	 *
	 * @param orderer Auftraggeber
	 * @param ordererPassword Passwort des Auftraggebers
	 * @param username Benutzername für den Passwörter abgefragt werden sollen
	 * @throws de.bsvrz.dav.daf.main.config.ConfigurationTaskException Die Konfiguration kann den Auftrag nicht ausführen (z.B. Login-Daten falsch oder die Konfiguration unterstützt den Auftrag nicht und muss aktualisiert werden).
	 * @return Indizes der noch verwendbaren Einmalpasswörter (leeres Array falls keine Passwörter mehr verfügbar sind), aufsteigend sortiert.
	 */
	public int[] getValidOneTimePasswordIDs(String orderer, String ordererPassword, String username) throws ConfigurationTaskException;
}
