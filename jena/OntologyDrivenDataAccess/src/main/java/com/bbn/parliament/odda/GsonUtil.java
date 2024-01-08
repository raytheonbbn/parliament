package com.bbn.parliament.odda;

import java.io.Writer;

import javax.xml.datatype.XMLGregorianCalendar;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;

public final class GsonUtil {
	private GsonUtil() {} // prevent instantiation

	public static Gson create(EntityFactory entityFactory) {
		EntityTypeAdapter eta = new EntityTypeAdapter(entityFactory);
		return new GsonBuilder()
			.registerTypeAdapter(XMLGregorianCalendar.class, new XMLDateTimeTypeAdapter())
			.registerTypeAdapter(Entity.class, eta)
			.registerTypeAdapter(RdfType.class, new RdfTypeTypeAdapter())
			.registerTypeAdapter(DatatypeProperty.class, new DatatypePropertyTypeAdapter())
			.registerTypeAdapter(ObjectProperty.class, new ObjectPropertyTypeAdapter(eta))
			.setExclusionStrategies(new AnnotationExclusionStrategy())
			.serializeSpecialFloatingPointValues()
			//.disableHtmlEscaping()
			//.serializeNulls()
			.setPrettyPrinting()
			.create();
	}

	public static JsonWriter createJsonWriter(Writer writer, boolean pretty) {
		JsonWriter jsonWriter = new JsonWriter(writer);
		jsonWriter.setLenient(true);
		jsonWriter.setSerializeNulls(false);
		jsonWriter.setHtmlSafe(true);
		if (pretty) {
			jsonWriter.setIndent("\t");
		}
		return jsonWriter;
	}
}
