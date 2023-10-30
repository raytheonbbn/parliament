package com.bbn.parliament.odda;

import java.io.IOException;

import org.apache.jena.rdf.model.ResourceFactory;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class RdfTypeTypeAdapter extends TypeAdapter<RdfType> {
	private final Entity owner;

	/** For writing */
	public RdfTypeTypeAdapter() {
		super();
		owner = null;
	}

	/** For reading */
	public RdfTypeTypeAdapter(Entity owner) {
		super();
		this.owner = ArgCheck.throwIfNull(owner, "owner");
	}

	@Override
	public RdfType read(JsonReader rdr) throws IOException {
		RdfType result = new RdfType(owner);
		if (rdr.peek() == JsonToken.NULL) {
			rdr.nextNull();
		} else if (rdr.peek() == JsonToken.BEGIN_ARRAY) {
			rdr.beginArray();
			while (rdr.peek() != JsonToken.END_ARRAY) {
				result.addValue(ResourceFactory.createResource(rdr.nextString()));
			}
			rdr.endArray();
		} else {
			result.addValue(ResourceFactory.createResource(rdr.nextString()));
		}
		return result;
	}

	@Override
	public void write(JsonWriter wtr, RdfType rdfType) throws IOException {
		if (rdfType == null || rdfType.size() <= 0) {
			@SuppressWarnings({ "resource", "unused" })
			JsonWriter tmp1 = wtr.nullValue();
		} else {
			boolean encodeAsArray = rdfType.size() > 1;
			if (encodeAsArray) {
				@SuppressWarnings({ "resource", "unused" })
				JsonWriter tmp1 = wtr.beginArray();
			}

			for (RdfTypeInfo value : rdfType.getValues()) {
				@SuppressWarnings({ "resource", "unused" })
				JsonWriter tmp1 = wtr.value(value.getUri().getURI());
			}

			if (encodeAsArray) {
				@SuppressWarnings({ "resource", "unused" })
				JsonWriter tmp1 = wtr.endArray();
			}
		}
	}
}
