package com.bbn.parliament.odda;

import java.io.IOException;

import org.apache.jena.rdf.model.Resource;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class ObjectPropertyTypeAdapter extends TypeAdapter<ObjectProperty> {
	private final Entity owner;
	private final Resource propIri;
	private final EntityTypeAdapter entTypeAdapter;

	/** For writing */
	public ObjectPropertyTypeAdapter(EntityTypeAdapter entityTypeAdapter) {
		super();
		owner = null;
		propIri = null;
		entTypeAdapter = ArgCheck.throwIfNull(entityTypeAdapter, "entityTypeAdapter");
	}

	/** For reading */
	public ObjectPropertyTypeAdapter(Entity owner, Resource propIri, EntityTypeAdapter entityTypeAdapter) {
		super();
		this.owner = ArgCheck.throwIfNull(owner, "owner");
		this.propIri = ArgCheck.throwIfNull(propIri, "propIri");
		entTypeAdapter = ArgCheck.throwIfNull(entityTypeAdapter, "entityTypeAdapter");
	}

	@Override
	public ObjectProperty read(JsonReader rdr) throws IOException {
		ObjectProperty result = new ObjectProperty(owner, propIri);
		if (rdr.peek() == JsonToken.NULL) {
			rdr.nextNull();
		} else if (rdr.peek() == JsonToken.BEGIN_ARRAY) {
			rdr.beginArray();
			long orderIndex = -1;
			while (rdr.peek() != JsonToken.END_ARRAY) {
				entTypeAdapter.orderIndexForNextRead(++orderIndex);
				result.addValue(entTypeAdapter.read(rdr));
			}
			rdr.endArray();
		} else {
			entTypeAdapter.orderIndexForNextRead(0);
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

			for (Entity ent : prop.values()) {
				entTypeAdapter.write(wtr, ent);
			}

			if (encodeAsArray) {
				@SuppressWarnings({ "resource", "unused" })
				JsonWriter tmp1 = wtr.endArray();
			}
		}
	}
}
