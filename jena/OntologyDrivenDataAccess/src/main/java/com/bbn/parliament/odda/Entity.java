package com.bbn.parliament.odda;

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;

import org.apache.commons.lang3.builder.CompareToBuilder;

import com.google.gson.stream.JsonWriter;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

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
				.append(lhs.getOrderIndex(), rhs.getOrderIndex())
				.append(lhs.getUri(), rhs.getUri()).build();
		}
		return result;
	};

	Resource getUri();

	void setUri(Resource newValue);

	boolean isFetched();

	long getOrderIndex();

	RdfType getType();

	void setType(RdfType newType);

	boolean isOfType(Resource type);

	DatatypeProperty getDTProp(Resource prop);

	Collection<DatatypeProperty> getDTProps();

	void addDTProp(DatatypeProperty newProp);

	ObjectProperty getObjProp(Resource prop);

	Collection<ObjectProperty> getObjProps();

	void addObjProp(ObjectProperty newProp);

	void generateRdf(Model model);

	void write(JsonWriter wtr) throws IOException;

	String toJson(boolean pretty);

	OntologyTools getOntTools();
}
