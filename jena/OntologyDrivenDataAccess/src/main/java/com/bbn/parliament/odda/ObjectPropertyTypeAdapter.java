package com.bbn.parliament.odda;

import java.io.IOException;

import org.apache.jena.rdf.model.Resource;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class ObjectPropertyTypeAdapter extends TypeAdapter<ObjectProperty> {
	private final Entity owner;
	private final Resource propUri;
	private final EntityTypeAdapter entTypeAdapter;

	/** For writing */
	public ObjectPropertyTypeAdapter(EntityTypeAdapter entityTypeAdapter) {
		super();
		owner = null;
		propUri = null;
		entTypeAdapter = ArgCheck.throwIfNull(entityTypeAdapter, "entityTypeAdapter");
	}

	/** For reading */
	public ObjectPropertyTypeAdapter(Entity owner, Resource propUri, EntityTypeAdapter entityTypeAdapter) {
		super();
		this.owner = ArgCheck.throwIfNull(owner, "owner");
		this.propUri = ArgCheck.throwIfNull(propUri, "propUri");
		entTypeAdapter = ArgCheck.throwIfNull(entityTypeAdapter, "entityTypeAdapter");
	}

	@Override
	public ObjectProperty read(JsonReader rdr) throws IOException {
		ObjectProperty result = new ObjectProperty(owner, propUri);
		if (rdr.peek() == JsonToken.NULL) {
			rdr.nextNull();
		} else if (rdr.peek() == JsonToken.BEGIN_ARRAY) {
			rdr.beginArray();
			long orderIndex = -1;
			while (rdr.peek() != JsonToken.END_ARRAY) {
				entTypeAdapter.setOrderIndexForNextRead(++orderIndex);
				result.addValue(entTypeAdapter.read(rdr));
			}
			rdr.endArray();
		} else {
			entTypeAdapter.setOrderIndexForNextRead(0);
			result.addValue(entTypeAdapter.read(rdr));
		}
		return result;
	}

	@Override
	public void write(JsonWriter wtr, ObjectProperty prop) throws IOException {
		if (prop == null || prop.size() <= 0) {
			@SuppressWarnings({ "resource", "unused" })
			JsonWriter tmp1 = wtr.nullValue();
		} else {
			boolean maxCardinalityIsOne = prop.maxCardinalityIsOne();
			boolean encodeAsArray = prop.size() > 1 || !maxCardinalityIsOne;

			if (encodeAsArray) {
				@SuppressWarnings({ "resource", "unused" })
				JsonWriter tmp1 = wtr.beginArray();
			}

			for (Entity ent : prop.getValues()) {
				entTypeAdapter.write(wtr, ent);
			}

			if (encodeAsArray) {
				@SuppressWarnings({ "resource", "unused" })
				JsonWriter tmp1 = wtr.endArray();
			}
		}
	}
}
