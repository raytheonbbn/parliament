package com.bbn.parliament.odda;

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import com.google.gson.stream.JsonWriter;

public interface Entity {
	// This comparator orders entities by their order index property.
	// Secondarily, it imposes a consistent but arbitrary order to
	// serialize test results deterministically.
	Comparator<Entity> COMPARATOR = (lhs, rhs) -> {
		int result;
		if (lhs == null && rhs == null) {
			result = 0;
		} else if (lhs == null) {
			result = -1;
		} else if (rhs == null) {
			result = 1;
		} else {
			result = new CompareToBuilder()
				.append(lhs.orderIndex(), rhs.orderIndex())
				.append(lhs.iri(), rhs.iri()).build();
		}
		return result;
	};

	Resource iri();

	void iri(Resource newValue);

	boolean isFetched();

	long orderIndex();

	RdfType type();

	void type(RdfType newType);

	boolean isOfType(Resource type);

	DatatypeProperty dtProp(Resource prop);

	Collection<DatatypeProperty> dtProps();

	void addDTProp(DatatypeProperty newProp);

	ObjectProperty objProp(Resource prop);

	Collection<ObjectProperty> objProps();

	void addObjProp(ObjectProperty newProp);

	void generateRdf(Model model);

	void write(JsonWriter wtr) throws IOException;

	String toJson(boolean pretty);

	EntityFactory entityFactory();
}
