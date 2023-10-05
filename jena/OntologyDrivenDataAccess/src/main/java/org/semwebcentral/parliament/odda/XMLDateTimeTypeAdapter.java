package org.semwebcentral.parliament.odda;

import java.io.IOException;

import javax.xml.datatype.XMLGregorianCalendar;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class XMLDateTimeTypeAdapter extends TypeAdapter<XMLGregorianCalendar> {
	@Override
	public XMLGregorianCalendar read(JsonReader rdr) throws IOException {
		XMLGregorianCalendar result = null;
		if (rdr.peek() == JsonToken.NULL) {
			rdr.nextNull();
		} else {
			String xmlDateTime = rdr.nextString();
			if (xmlDateTime != null && !xmlDateTime.isEmpty()) {
				result = QSUtil.DT_FACT.newXMLGregorianCalendar(xmlDateTime);
			}
		}
		return result;
	}

	@Override
	public void write(JsonWriter wtr, XMLGregorianCalendar value) throws IOException {
		if (value == null) {
			@SuppressWarnings({ "resource", "unused" })
			JsonWriter tmp1 = wtr.nullValue();
		} else {
			@SuppressWarnings({ "resource", "unused" })
			JsonWriter tmp1 = wtr.value(value.toXMLFormat());
		}
	}
}
