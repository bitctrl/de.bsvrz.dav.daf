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

package de.bsvrz.dav.daf.communication.lowLevel.telegrams;

import de.bsvrz.dav.daf.communication.srpAuthentication.SrpTelegramEncryption;
import de.bsvrz.dav.daf.main.impl.CommunicationConstant;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Dieses Telegramm enthält verschlüsselte Daten, bestehend aus mehreren einzelnen {@linkplain DataTelegram Telegrammen}. Dieses Telegramm wird speziell behandelt und schon von der {@link de.bsvrz.dav.daf.communication.lowLevel.LowLevelCommunication LowLevelCommunication}
 * erstellt bzw. zerlegt
 *
 * @author Kappich Systemberatung
 */
public class EncryptedTelegram extends DataTelegram {

	/**
	 * Verschlüsselte Daten
	 */
	private byte[] _encryptedData;

	/** 
	 * Erstellt ein neues EncryptedTelegram
	 */
	public EncryptedTelegram() {
		type = ENCRYPTED_TYPE;
		priority = CommunicationConstant.SYSTEM_TELEGRAM_PRIORITY;
	}

	/** 
	 * 
	 * Erstellt ein neues EncryptedTelegram mit Telegrammen als Inhalt
	 * @param encryption Verschlüsselungsmodul zur Verschlüsselung
	 * @param telegrams Zu verschlüsselnde Telegramme
	 */
	public EncryptedTelegram(SrpTelegramEncryption encryption, Collection<DataTelegram> telegrams) throws IOException {
		this();
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
		for(DataTelegram telegram : telegrams) {
			dataOutputStream.writeByte(telegram.getType());
			telegram.write(dataOutputStream);
		}
		_encryptedData = encryption.encrypt(byteArrayOutputStream.toByteArray());
		if(_encryptedData.length > Short.MAX_VALUE) {
			throw new IOException("Länge für verschlüsseltes Telegramm zu groß: " + _encryptedData.length);
		}
		length = _encryptedData.length;
	}
	
	/**
	 * Gibt die verschlüsselten enthaltenen Telegramme zurück
	 * @param encryption Verschlüsselungsmodul zur Entschlüsselung
	 * @return die verschlüsselten enthaltenen Telegramme
	 */
	public Collection<DataTelegram> getTelegrams(SrpTelegramEncryption encryption) throws IOException {
		byte[] decryptedBytes = encryption.decrypt(_encryptedData);
		DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(decryptedBytes));
		final List<DataTelegram> telegrams = new ArrayList<>();
		while(true){
			int telegramType = dataInputStream.read();
			if(telegramType == -1) {
				// Stream zuende
				return telegrams;
			}
			DataTelegram telegram = DataTelegram.getTelegram((byte) telegramType);
			if (telegram == null){
				throw new IOException("Telegramm mit unbekanntem Typ empfangen: " + type);
			}
			telegram.read(dataInputStream);
			telegrams.add(telegram);
		}
	}

	@Override
	public void read(final DataInputStream in) throws IOException {
		int dataLength = in.readShort();
		byte[] data = new byte[dataLength];
		in.readFully(data);
		_encryptedData = data;
		length = dataLength;
	}

	@Override
	public void write(final DataOutputStream out) throws IOException {
		out.writeShort(_encryptedData.length);
		out.write(_encryptedData);
	}


	@Override
	public String parseToString() {
		return "Systemtelegram verschlüsselt\n";
	}

	@Override
	public String toString() {
		return super.toString();
	}
}
