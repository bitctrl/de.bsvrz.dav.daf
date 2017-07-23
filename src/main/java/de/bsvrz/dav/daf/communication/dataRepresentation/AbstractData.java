/*
 * Copyright 2007 by Kappich Systemberatung, Aachen
 * Copyright 2006 by Kappich Systemberatung Aachen
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

package de.bsvrz.dav.daf.communication.dataRepresentation;

import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.config.*;
import de.bsvrz.sys.funclib.debug.Debug;

import java.text.*;
import java.util.Date;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Diese abstrakte Klasse stellt eine Oberklasse von Datentypen dar. Es werden die Methoden des Interfaces <code>data</code> erstmalig implementiert. Je nach
 * Bedarf werden diese wieder in den Subklassen überschrieben.
 *
 * @author Kappich Systemberatung
 * @version $Revision$
 */
public abstract class AbstractData implements Data {

	/** DebugLogger für Debug-Ausgaben */
	private static final Debug _debug = Debug.getLogger();

	private static final DateFormat _absoluteMillisecondsFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss,SSS");

	private static final DateFormat _absoluteSecondsFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

	private static final DateFormat[] _parseDateFormats = new DateFormat[]{
			new SimpleDateFormat("dd.MM.yy HH:mm:ss,SSS"), new SimpleDateFormat("dd.MM.yy HH:mm:ss"), new SimpleDateFormat("dd.MM.yy HH:mm"),
			new SimpleDateFormat("dd.MM.yy"),
	};

	/** Erzeugt ein neues Objekt der Klasse AbstractData */
	public AbstractData() {
	}

	@Override
	public Data createModifiableCopy() {
		throw new IllegalStateException("getModifiableCopy(): Kopie kann nur von ganzen Datensätzen erzeugt werden, this: " + toString());
	}

	@Override
	public Data createUnmodifiableCopy() {
		throw new IllegalStateException("getUnmodifiableCopy(): Kopie kann nur von ganzen Datensätzen erzeugt werden, this: " + toString());
	}

	public String toString() {
		return getName() + ":" + valueToString();
	}

	@Override
	public Data getItem(String itemName) {
		for(final Data item : this) {
			if(itemName.equals(item.getName())) return item;
		}
		throw new NoSuchElementException("Attribut " + itemName + " nicht im Datensatz enthalten: " + this);
	}

	@Override
	public Data.Array getArray(String itemName) {
		return getItem(itemName).asArray();
	}

	@Override
	public Data.NumberValue getUnscaledValue(String itemName) {
		return getItem(itemName).asUnscaledValue();
	}

	@Override
	public Data.NumberArray getUnscaledArray(String itemName) {
		return getItem(itemName).asUnscaledArray();
	}

	@Override
	public Data.TimeValue getTimeValue(String itemName) {
		return getItem(itemName).asTimeValue();
	}

	@Override
	public Data.TimeArray getTimeArray(String itemName) {
		return getItem(itemName).asTimeArray();
	}

	@Override
	public Data.TextValue getTextValue(String itemName) {
		return getItem(itemName).asTextValue();
	}

	@Override
	public Data.TextArray getTextArray(String itemName) {
		return getItem(itemName).asTextArray();
	}

	@Override
	public Data.NumberValue getScaledValue(String itemName) {
		return getItem(itemName).asScaledValue();
	}

	@Override
	public Data.NumberArray getScaledArray(String itemName) {
		return getItem(itemName).asScaledArray();
	}

	@Override
	public Data.ReferenceValue getReferenceValue(String itemName) {
		return getItem(itemName).asReferenceValue();
	}

	@Override
	public Data.ReferenceArray getReferenceArray(String itemName) {
		return getItem(itemName).asReferenceArray();
	}

	@Override
	public Data.NumberValue asUnscaledValue() {
		throw new UnsupportedOperationException("Attribut " + getName() + " kann nicht in einen unskaliertem Zahlwert dargestellt werden");
	}

	@Override
	public Data.TimeValue asTimeValue() {
		throw new UnsupportedOperationException("Attribut " + getName() + " kann nicht in einem Zeitwert dargestellt werden");
	}

	@Override
	public Data.NumberValue asScaledValue() {
		throw new UnsupportedOperationException("Attribut " + getName() + " kann nicht in einem skalierten Zahlwert dargestellt werden");
	}

	@Override
	public Data.ReferenceValue asReferenceValue() {
		throw new UnsupportedOperationException("Attribut " + getName() + " kann nicht in einem Referenzwert dargestellt werden");
	}

	@Override
	public Data.NumberArray asUnscaledArray() {
		return asArray().asUnscaledArray();
	}

	@Override
	public Data.TimeArray asTimeArray() {
		return asArray().asTimeArray();
	}

	@Override
	public Data.TextArray asTextArray() {
		return asArray().asTextArray();
	}

	@Override
	public Data.NumberArray asScaledArray() {
		return asArray().asScaledArray();
	}

	@Override
	public Data.ReferenceArray asReferenceArray() {
		return asArray().asReferenceArray();
	}

	@Override
	public Data.Array asArray() {
		throw new UnsupportedOperationException("Attribut " + getName() + " kann nicht in einem Array dargestellt werden");
	}

	/** Subklasse von <code>AbstractData</code>. */
	public abstract static class PlainData extends AbstractData {

		@Override
		public boolean isPlain() {
			return true;
		}

		@Override
		public boolean isList() {
			return false;
		}

		@Override
		public boolean isArray() {
			return false;
		}

		@Override
		public String valueToString() {
			try {
				if(isDefined()) return asTextValue().getText();
				return "<Undefiniert>";
			}
			catch(Exception e) {
				e.printStackTrace();
				return "<<Fehler:" + e.getMessage() + ">>";
			}
		}

		@Override
		public Iterator<Data> iterator() {
			throw new java.lang.UnsupportedOperationException("Über das Attribut " + getName() + " kann nicht iteriert werden");
		}
	}

	/** Subklasse von <code>AbstractData</code>. */
	public abstract static class StructuredData extends AbstractData {

		@Override
		public boolean isPlain() {
			return false;
		}

		@Override
		public Data.TextValue asTextValue() {
			throw new UnsupportedOperationException("Attributliste " + getName() + " kann nicht in einem Textwert dargestellt werden");
		}

		@Override
		public boolean isDefined() {
			// Es handelt sich um eine Liste oder ein Array, alle Elemente durchlaufen und
			// ebenfalls prüfen.

			for(final Data data : this) {
				if(!data.isDefined()) {
					return false;
				}
			}
			return true;
		}

		@Override
		public void setToDefault() {
			// Ist das Objekt ein Array, so muss:
			// - bei Arrays mit variabler Länge die Länge auf 0 gesetzt werden
			// - Arrays mit fester Länge werden auf die Länge gesetzt und die Elemente dann initialisiert
			if(isArray()) {
				final Data.Array array = asArray();
				if(array.isCountVariable()) {
					array.setLength(0);
				}
				else {
					array.setLength(array.getMaxCount());
				}
			}

			// Sowohl bei Arrays als auch bei Listen müssen alle Elemente mit ihrem Defaultwert (oder undefiniert)
			// initialisiert werden.
			for(final Data data : this) {
				data.setToDefault();
			}
		}
	}

	/** Subklasse von <code>AbstractData</code>, abgeleitet von <code>AbstractData.StructuredData</code>. */
	public abstract static class ListData extends StructuredData {

		@Override
		public boolean isList() {
			return true;
		}

		@Override
		public boolean isArray() {
			return false;
		}

		@Override
		public String valueToString() {
			StringBuffer result = new StringBuffer();
			result.append("{");
			try {
				Iterator<Data> i = iterator();
				while(i.hasNext()) {
					try {
						Data item = i.next();
						result.append(item.toString());
					}
					catch(Exception e) {
						result.append("<<Fehler:").append(e.getMessage()).append(">>");
					}
					if(i.hasNext()) result.append("; ");
				}
			}
			catch(Exception e) {
				result.append("<<").append(e.getMessage()).append(">>");
			}
			result.append("}");
			return result.toString();
		}
	}

	/** Subklasse von <code>AbstractData</code>, abgeleitet von <code>AbstractData.StructuredData</code>. */
	public abstract static class ArrayData extends StructuredData {

		@Override
		public boolean isList() {
			return false;
		}

		@Override
		public boolean isArray() {
			return true;
		}

		@Override
		public String valueToString() {
			StringBuffer result = new StringBuffer();
			result.append("[");
			try {
				Iterator<Data> i = iterator();
				while(i.hasNext()) {
					try {
						Data item = i.next();
						result.append(item.valueToString());
					}
					catch(Exception e) {
						result.append("<<Fehler:").append(e.getMessage()).append(">>");
					}
					if(i.hasNext()) result.append("; ");
				}
			}
			catch(Exception e) {
				result.append("<<").append(e.getMessage()).append(">>");
			}
			result.append("]");
			return result.toString();
		}
	}

	/** Subklasse von <code>AbstractData</code>, implementiert das Interface <code>Data.TextValue</code>. */
	public abstract static class TextValue implements Data.TextValue {

		@Override
		public String getSuffixText() {
			return "";
		}

		@Override
		public String getText() {
			String valueText = getValueText();
			String suffixText = getSuffixText();
			if(suffixText.equals("")) return valueText;
			if(valueText.equals("")) return suffixText;
			return valueText + " " + suffixText;
		}

		public String toString() {
			return getText();
		}
	}

	/** Subklasse von <code>AbstractData</code>, abgeleitet von <code>AbstractData.TextValue</code>, implementiert das Interface <code>Data.NumberValue</code>. */
	public abstract static class NumberValue extends AbstractData.TextValue implements Data.NumberValue {

		private static final NumberFormat _parseNumberFormat = NumberFormat.getNumberInstance();

		static {
			_parseNumberFormat.setMinimumIntegerDigits(1);
			_parseNumberFormat.setMaximumIntegerDigits(999);
			_parseNumberFormat.setMinimumFractionDigits(0);
			_parseNumberFormat.setMaximumFractionDigits(999);
			_parseNumberFormat.setGroupingUsed(false);
		}

		@Override
		public boolean isNumber() {
			return true;
		}

		@Override
		public boolean isState() {
			return getState() != null;
		}

		@Override
		public byte byteValue() {
			throw new UnsupportedOperationException("Attribut  kann nicht im gewüschten Zahlentyp dargestellt werden");
		}

		@Override
		public short shortValue() {
			return (short)byteValue();
		}

		@Override
		public int intValue() {
			return (int)shortValue();
		}

		@Override
		public long longValue() {
			return (long)intValue();
		}

		@Override
		public float floatValue() {
			return (float)doubleValue();
		}

		@Override
		public double doubleValue() {
			return (double)longValue();
		}

		@Override
		public IntegerValueState getState() {
			return null;
		}

		@Override
		public void setState(IntegerValueState state) {
			throw new UnsupportedOperationException("Beim Attribut sind keine Zustände erlaubt");
		}

		@Override
		public void set(int value) {
			set((long)value);
		}

		@Override
		public void set(long value) {
			set((double)value);
		}

		@Override
		public void set(float value) {
			set((double)value);
		}

		@Override
		public void set(double value) {
			throw new UnsupportedOperationException("gewünschte Wertkonvertierung nicht erlaubt");
		}

		@Override
		public void setText(String text) {
			Number number;
			ParsePosition parsePosition = new ParsePosition(0);
			synchronized(_parseNumberFormat) {
				number = _parseNumberFormat.parse(text.replace('.', ','), parsePosition);
			}
			if(number == null) throw new IllegalArgumentException("Text " + text + " kann nicht in eine Zahl konvertiert werden");
			if(number instanceof Long) {
				set(number.longValue());
			}
			else {
				set(number.doubleValue());
			}
		}
	}

	/** Subklasse von <code>AbstractData</code>, abgeleitet von <code>AbstractData.TextValue</code>, implementiert das Interface <code>Data.ReferenceValue</code>. */
	public abstract static class ReferenceValue extends AbstractData.TextValue implements Data.ReferenceValue {

		protected abstract DataModel getDataModel();

		/**
		 * Liefert den Wert dieses Referenzattributs als Text zurück. Wenn das referenzierte Objekt eine Pid hat wird diese zurückgegeben, ansonsten wird die Id des
		 * Objekts zurückgegeben.
		 *
		 * @return pid oder id des referenzierten Objekts als Text.
		 *
		 * @see #getSuffixText
		 * @see #getText
		 * @see SystemObject#getPid
		 * @see SystemObject#getId
		 */
		@Override
		public String getValueText() {
			try {
				SystemObject object = getSystemObject();
				if(object == null) return "undefiniert";
				String pid = object.getPid();
				if(pid != null && !pid.equals("")) return pid;
			}
			catch(Exception e) {
				//Fehler beim Lesen der pid -> weiter mit Rückgabe der id
			}
			try {
				return String.valueOf(getId());
			}
			catch(Exception ee) {
				return "<<" + ee.getMessage() + ">>";
			}
		}


		/**
		 * Liefert Zusatzinformationen zum Wert dieses Referenzattributs. Der zurückgelieferte Text ist als Ergänzung zum Rückgabewert der Methode {@link
		 * #getValueText} zu verstehen. Das Ergebnis der Methode enthält abhängig vom Ergebnis der Methode {@link #getValueText} den konstanten Text "id" bzw. "pid"
		 * und zusätzlich den Namen des referenzierten Objekts (wenn vorhanden).
		 *
		 * @return Text mit Zusatzinformation zum Wert dieses Referenzattributs.
		 *
		 * @see #getSuffixText
		 * @see #getText
		 */
		@Override
		public String getSuffixText() {
			try {
				String name = null;
				String pid = null;
				String exceptionMessage = null;
				StringBuffer suffix = new StringBuffer();
				try {
					SystemObject object = getSystemObject();
					if(object == null) return "";
					name = object.getName();
					pid = object.getPid();
				}
				catch(Exception e) {
					exceptionMessage = " " + e.getLocalizedMessage();
				}
				if(pid == null || pid.equals("")) {
					suffix.append("id");
				}
				else {
					suffix.append("pid");
				}
				if(name != null && !name.equals("")) suffix.append(" (Name: ").append(name).append(")");
				if(exceptionMessage != null) suffix.append(" ").append(exceptionMessage);
				return suffix.toString();
			}
			catch(Exception ee) {
				return "<<" + ee.getMessage() + ">>";
			}
		}

		@Override
		public void setText(String text) {
			int startIndex;
			boolean tryPid = true;
			boolean tryId = true;
			String lowercaseText = text.toLowerCase();
			startIndex = lowercaseText.lastIndexOf("pid:");
			if(startIndex >= 0) {
				startIndex += 4;
				tryId = false;
			}
			else {
				startIndex = lowercaseText.lastIndexOf("id:");
				if(startIndex >= 0) {
					startIndex += 3;
					tryPid = false;
				}
				else {
					startIndex = 0;
				}
			}
			text = text.substring(startIndex).trim();
			if(tryId) {
				String numberText = text.split("\\D", 2)[0];
				if(numberText.length() > 0) {
					long id = Long.parseLong(numberText);
					if(id == 0) {
						setSystemObject(null);
						return;
					}
					SystemObject object;
					try {
						object = getDataModel().getObject(id);
					}
					catch(Exception e) {
						object = null;
					}
					if(object != null) {
						setSystemObject(object);
						return;
					}
				}
			}
			if(tryPid) {
				String pid = text.split("[\\s\\Q[]{}():\\E]", 2)[0];
				if(pid.equals("null") || pid.equals("undefiniert")) {
					setSystemObject(null);
					return;
				}
				SystemObject object;
				try {
					object = getDataModel().getObject(pid);
				}
				catch(Exception e) {
					object = null;
				}
				if(object != null) {
					setSystemObject(object);
					return;
				}
			}
			throw new IllegalArgumentException("Der Text '" + text + "' kann nicht als Objektreferenz interpretiert werden.");
		}

		@Override
		public SystemObject getSystemObject() {
			try {
				long id = getId();
				if(id == 0) return null;
				SystemObject object = getDataModel().getObject(id);
				if(object == null) throw new IllegalStateException("Ungültiges Objekt mit id " + id);
				return object;
			}
			catch(Exception e) {
				throw new RuntimeException(e);
			}
		}
		
		public void checkObject(SystemObject object, Attribute attribute){
			if(object == null) return;
			if(attribute == null) return;
			AttributeType attributeType = attribute.getAttributeType();
			if(attributeType instanceof ReferenceAttributeType) {
				ReferenceAttributeType referenceAttributeType = (ReferenceAttributeType) attributeType;
				SystemObjectType referencedObjectType = referenceAttributeType.getReferencedObjectType();
				if(referencedObjectType == null) {
					// Es können beliebige Objekte referenziert werden
					return;
				}
				if(!object.isOfType(referencedObjectType)){
					throw new IllegalArgumentException("Objekt " + object + " soll am Attribut " + attribute + " gespeichert werden, aber der Attributtyp erlaubt nur Objekte vom Typ " + referencedObjectType.getPidOrNameOrId());
				}
			}
		}

		@Override
		public void setSystemObjectPid(String objectPid, ObjectLookup datamodel) {
			final SystemObject systemObject;
			if(objectPid.length() == 0) systemObject = null;
			else {
				systemObject = datamodel.getObject(objectPid);
				if(systemObject == null) {
					if(tryToStorePid(objectPid)) {
						_debug.warning("Eine optionale Referenz auf das Objekt mit der Pid '" + objectPid + "' konnte nicht aufgelöst werden");
						return;
					}
					else {
						throw new IllegalArgumentException("Das referenzierte Objekt '" + objectPid + "' wurde nicht gefunden");
					}
				}
			}
			setSystemObject(systemObject);
		}

		@Override
		public void setSystemObjectPid(final String objectPid) {
			setSystemObjectPid(objectPid, getDataModel());
		}

		abstract boolean tryToStorePid(final String objectPid);

		@Override
		public String getSystemObjectPid() {
			final SystemObject systemObject = getSystemObject();
			if(systemObject == null) {
				return getStoredPid();
			}
			else {
				return systemObject.getPid();
			}
		}

		abstract String getStoredPid();
	}

	/** Subklasse von <code>AbstractData</code>, abgeleitet von <code>AbstractData.TextValueext</code> zur Bestimmung der Zeit. */
	private abstract static class TimeValue extends AbstractData.TextValue implements Data.TimeValue {

	}

	/** Subklasse von <code>AbstractData</code>, abgeleitet von <code>AbstractData.TimeValue</code> der Relativen(vergangenen) Zeit. */
	public abstract static class RelativeTimeValue extends AbstractData.TimeValue {

		@Override
		public String getValueText() {
			try {
				StringBuffer text = new StringBuffer();
				long val = getMillis();
				//Beispiel= "234 Tage 12 Stunden 34 Minuten 33 Sekunden 443 Millisekunden"
				int millis = (int)(val % 1000);
				val /= 1000;
				int seconds = (int)(val % 60);
				val /= 60;
				int minutes = (int)(val % 60);
				val /= 60;
				int hours = (int)(val % 24);
				val /= 24;
				long days = val;
				if(days != 0) {
					if(days == 1) {
						text.append("1 Tag ");
					}
					else if(days == -1) {
						text.append("-1 Tag ");
					}
					else {
						text.append(days).append(" Tage ");
					}
				}
				if(hours != 0) {
					if(hours == 1) {
						text.append("1 Stunde ");
					}
					else if(hours == -1) {
						text.append("-1 Stunde ");
					}
					else {
						text.append(hours).append(" Stunden ");
					}
				}
				if(minutes != 0) {
					if(minutes == 1) {
						text.append("1 Minute ");
					}
					else if(minutes == -1) {
						text.append("-1 Minute ");
					}
					else {
						text.append(minutes).append(" Minuten ");
					}
				}
				if(seconds != 0 || (days == 0 && hours == 0 && minutes == 0 && millis == 0)) {
					if(seconds == 1) {
						text.append("1 Sekunde ");
					}
					else if(seconds == -1) {
						text.append("-1 Sekunde ");
					}
					else {
						text.append(seconds).append(" Sekunden ");
					}
				}
				if(millis != 0) {
					if(millis == 1) {
						text.append("1 Millisekunde ");
					}
					else if(millis == -1) {
						text.append("-1 Millisekunde ");
					}
					else {
						text.append(millis).append(" Millisekunden ");
					}
				}
				text.setLength(text.length() - 1);
				return text.toString();
			}
			catch(Exception e) {
				return "<<" + e.getMessage() + ">>";
			}
		}

		private static final String _relNumberPattern = "-?(?:(?:-?0[0-7]{1,22}+)|(?:-?[1-9][0-9]{0,18}+)|(?:-?(?:#|0x|0X)[0-9a-fA-F]{0,16}+)|(?:-?0))";

		private static final String _relNamePattern = "[tThHsSmM][a-zA-Z]{0,15}+";

		private static final String _relNumberNamePattern = "(?<=" + _relNumberPattern + ")\\s*(?=" + _relNamePattern + ")";

		private static final String _relNameNumberPattern = "(?<=" + _relNamePattern + ")\\s*(?=" + _relNumberPattern + ")";

		private static final String _relPattern = "(?:" + _relNumberNamePattern + ")|(?:" + _relNameNumberPattern + ")";

		@Override
		public void setText(String text) {
			String[] splitted = text.trim().split(_relPattern);
			long number;
			long millis = 0;
			for(int i = 0; i < splitted.length; ++i) {
				String word = splitted[i];
				number = Long.decode(word).longValue();
				if(++i < splitted.length) {
					word = splitted[i].toLowerCase();
					if(word.equals("t") || word.startsWith("tag")) {
						millis += (1000 * 60 * 60 * 24) * number;
					}
					else if(word.equals("h") || word.startsWith("stunde")) {
						millis += (1000 * 60 * 60) * number;
					}
					else if(word.equals("m") || word.startsWith("minute")) {
						millis += (1000 * 60) * number;
					}
					else if(word.equals("s") || word.startsWith("sekunde")) {
						millis += 1000 * number;
					}
					else if(word.equals("ms") || word.startsWith("milli")) {
						millis += number;
					}
					else {
						throw new IllegalArgumentException("Ungültige relative Zeitangabe: " + splitted[i]);
					}
				}
				else {
					throw new IllegalArgumentException("Fehlende Einheit bei relativer Zeitangabe: " + text);
				}
			}
			setMillis(millis);
		}

		@Override
		public String getSuffixText() {
			return "";
		}
	}

	/** Subklasse von <code>AbstractData</code>, abgeleitet von <code>AbstractData.AbsoluteTimeValue</code> zur Bestimmung der Systemzeit in Millisekunden. */
	public abstract static class AbsoluteMillisTimeValue extends AbsoluteTimeValue {

		@Override
		public String getValueText() {
			try {
				Date date = new Date(getMillis());
				synchronized(_absoluteMillisecondsFormat) {
					return _absoluteMillisecondsFormat.format(date);
				}
			}
			catch(Exception e) {
				return "<<" + e.getMessage() + ">>";
			}
		}
	}

	/** Subklasse von <code>AbstractData</code>, abgeleitet von <code>AbstractData.AbsoluteTimeValue</code> zur Bestimmung der Systemzeit in Sekunden. */
	public abstract static class AbsoluteSecondsTimeValue extends AbsoluteTimeValue {

		@Override
		public String getValueText() {
			try {
				Date date = new Date(getMillis());
				synchronized(_absoluteSecondsFormat) {
					return _absoluteSecondsFormat.format(date);
				}
			}
			catch(Exception e) {
				return "<<" + e.getMessage() + ">>";
			}
		}
	}

	/** Subklasse von <code>AbstractData</code>, abgeleitet von <code>AbstractData.TimeValue</code> zur Bestimmmung der Systemzeit. */
	private abstract static class AbsoluteTimeValue extends AbstractData.TimeValue {

		@Override
		public void setText(String text) {
			DateFormat format;
			Date date;
			for(int i = 0; i < _parseDateFormats.length; ++i) {
				format = _parseDateFormats[i];
				try {
					synchronized(format) {
						date = format.parse(text);
					}
					setMillis(date.getTime());
					return;
				}
				catch(ParseException e) {
					//continue with next Format
				}
			}
			throw new IllegalArgumentException(
					"Ungültig Zeitangabe '" + text + "' (Unterstützte Formate: 'dd.MM.yy HH:mm:ss,SSS', 'dd.MM.yy HH:mm:ss', 'dd.MM.yy HH:mm', 'dd.MM.yy')"
			);
		}

		@Override
		public String getSuffixText() {
			return "Uhr";
		}
	}

	/** Subklasse von <code>AbstractData</code>, implementiert das Interface <code>Data.Array</code>. */
	abstract public static class Array implements Data.Array {

		@Override
		public Data.NumberValue[] getUnscaledValues() {
			return asUnscaledArray().getValues();
		}

		@Override
		public Data.NumberValue getUnscaledValue(int itemIndex) {
			return asUnscaledArray().getValue(itemIndex);
		}

		@Override
		public Data.TimeValue[] getTimeValues() {
			return asTimeArray().getTimeValues();
		}

		@Override
		public Data.TimeValue getTimeValue(int itemIndex) {
			return asTimeArray().getTimeValue(itemIndex);
		}

		@Override
		public Data.TextValue[] getTextValues() {
			return asTextArray().getTextValues();
		}

		@Override
		public Data.TextValue getTextValue(int itemIndex) {
			return asTextArray().getTextValue(itemIndex);
		}

		@Override
		public Data.NumberValue[] getScaledValues() {
			return asScaledArray().getValues();
		}

		@Override
		public Data.NumberValue getScaledValue(int itemIndex) {
			return asScaledArray().getValue(itemIndex);
		}

		@Override
		public Data.ReferenceValue[] getReferenceValues() {
			return asReferenceArray().getReferenceValues();
		}

		@Override
		public Data.ReferenceValue getReferenceValue(int itemIndex) {
			return asReferenceArray().getReferenceValue(itemIndex);
		}

		@Override
		public Data.NumberArray asUnscaledArray() {
			throw new UnsupportedOperationException("Attribut kann nicht in einem Zahlen-Array dargestellt werden");
		}

		@Override
		public Data.TimeArray asTimeArray() {
			throw new UnsupportedOperationException("Attribut kann nicht in einem Zeitwert-Array dargestellt werden");
		}

		@Override
		public Data.TextArray asTextArray() {
			throw new UnsupportedOperationException("Attribut kann nicht in einem Text-Array dargestellt werden");
		}

		@Override
		public Data.NumberArray asScaledArray() {
			throw new UnsupportedOperationException("Attribut kann nicht in einem Zahlen-Array dargestellt werden");
		}

		@Override
		public Data.ReferenceArray asReferenceArray() {
			throw new UnsupportedOperationException("Attribut kann nicht in einem Referenz-Array dargestellt werden");
		}
	}

	/** Subklasse von <code>AbstractData</code>, implementiert das Interface <code>Data.NumberArray</code>. */
	abstract public static class NumberArray implements Data.NumberArray {

		@Override
		public Data.NumberValue[] getValues() {
			int length = getLength();
			Data.NumberValue[] result = new Data.NumberValue[length];
			for(int i = 0; i < length; ++i) {
				result[i] = getValue(i);
			}
			return result;
		}

		@Override
		public byte byteValue(int itemIndex) {
			return getValue(itemIndex).byteValue();
		}

		@Override
		public short shortValue(int itemIndex) {
			return getValue(itemIndex).shortValue();
		}

		@Override
		public int intValue(int itemIndex) {
			return getValue(itemIndex).intValue();
		}

		@Override
		public long longValue(int itemIndex) {
			return getValue(itemIndex).longValue();
		}

		@Override
		public float floatValue(int itemIndex) {
			return getValue(itemIndex).floatValue();
		}

		@Override
		public double doubleValue(int itemIndex) {
			return getValue(itemIndex).doubleValue();
		}

		@Override
		public byte[] getByteArray() {
			int length = getLength();
			byte[] result = new byte[length];
			for(int i = 0; i < length; ++i) {
				result[i] = getValue(i).byteValue();
			}
			return result;
		}

		abstract void setLengthUninitialized(int length);
		
		@Override
		public void set(byte[] bytes) {
			setLengthUninitialized(bytes.length);
			for(int i = 0; i < bytes.length; ++i) {
				getValue(i).set(bytes[i]);
			}
		}

		@Override
		public void set(short[] shorts) {
			setLengthUninitialized(shorts.length);
			for(int i = 0; i < shorts.length; ++i) {
				getValue(i).set(shorts[i]);
			}
		}

		@Override
		public void set(int[] ints) {
			setLengthUninitialized(ints.length);
			for(int i = 0; i < ints.length; ++i) {
				getValue(i).set(ints[i]);
			}
		}

		@Override
		public void set(long[] longs) {
			setLengthUninitialized(longs.length);
			for(int i = 0; i < longs.length; ++i) {
				getValue(i).set(longs[i]);
			}
		}

		@Override
		public void set(float[] floats) {
			setLengthUninitialized(floats.length);
			for(int i = 0; i < floats.length; ++i) {
				getValue(i).set(floats[i]);
			}
		}

		@Override
		public void set(double[] doubles) {
			setLengthUninitialized(doubles.length);
			for(int i = 0; i < doubles.length; ++i) {
				getValue(i).set(doubles[i]);
			}
		}

		@Override
		public short[] getShortArray() {
			int length = getLength();
			short[] result = new short[length];
			for(int i = 0; i < length; ++i) {
				result[i] = getValue(i).shortValue();
			}
			return result;
		}

		@Override
		public int[] getIntArray() {
			int length = getLength();
			int[] result = new int[length];
			for(int i = 0; i < length; ++i) {
				result[i] = getValue(i).intValue();
			}
			return result;
		}

		@Override
		public long[] getLongArray() {
			int length = getLength();
			long[] result = new long[length];
			for(int i = 0; i < length; ++i) {
				result[i] = getValue(i).longValue();
			}
			return result;
		}

		@Override
		public float[] getFloatArray() {
			int length = getLength();
			float[] result = new float[length];
			for(int i = 0; i < length; ++i) {
				result[i] = getValue(i).floatValue();
			}
			return result;
		}

		@Override
		public double[] getDoubleArray() {
			int length = getLength();
			double[] result = new double[length];
			for(int i = 0; i < length; ++i) {
				result[i] = getValue(i).doubleValue();
			}
			return result;
		}
	}
}

