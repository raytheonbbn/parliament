package com.bbn.parliament.odda;

import java.io.IOException;

import org.apache.jena.rdf.model.Resource;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class DatatypePropertyTypeAdapter extends TypeAdapter<DatatypeProperty> {
	private final Entity owner;
	private final Resource propUri;

	/** For writing */
	public DatatypePropertyTypeAdapter() {
		super();
		owner = null;
		propUri = null;
	}

	/** For reading */
	public DatatypePropertyTypeAdapter(Entity owner, Resource propUri) {
		super();
		this.owner = ArgCheck.throwIfNull(owner, "owner");
		this.propUri = ArgCheck.throwIfNull(propUri, "propUri");
	}

	@Override
	public DatatypeProperty read(JsonReader rdr) throws IOException {
		try {
			DatatypeProperty result = new DatatypeProperty(owner, propUri);
			if (rdr.peek() == JsonToken.NULL) {
				rdr.nextNull();
			} else if (rdr.peek() == JsonToken.BEGIN_ARRAY) {
				rdr.beginArray();
				while (rdr.peek() != JsonToken.END_ARRAY) {
					result.addValue(RdfLiteral.readFromJson(rdr));
				}
				rdr.endArray();
			} else {
				result.addValue(RdfLiteral.readFromJson(rdr));
			}
			return result;
		} catch (IllegalStateException ex) {
			throw new IOException("Unable to parse JSON string:", ex);
		}
	}

	@Override
	public void write(JsonWriter wtr, DatatypeProperty prop) throws IOException {
		if (prop == null || prop.size() <= 0) {
			@SuppressWarnings({ "resource", "unused" })
			JsonWriter tmp1 = wtr.nullValue();
		} else {
			boolean maxCardinalityIsOne = prop.maxCardinalityIsOne();
			if (Geo.asWKT.equals(prop.getPropUri())) {
				maxCardinalityIsOne = true;
			}
			boolean encodeAsArray = prop.size() > 1 || !maxCardinalityIsOne;
			if (encodeAsArray) {
				@SuppressWarnings({ "resource", "unused" })
				JsonWriter tmp1 = wtr.beginArray();
			}

			for (RdfLiteral lit : prop.getValues()) {
				lit.writeAsJson(wtr);
			}

			if (encodeAsArray) {
				@SuppressWarnings({ "resource", "unused" })
				JsonWriter tmp1 = wtr.endArray();
			}
		}
	}
}
