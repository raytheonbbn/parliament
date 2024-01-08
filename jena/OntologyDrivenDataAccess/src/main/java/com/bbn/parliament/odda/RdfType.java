package com.bbn.parliament.odda;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;

public class RdfType extends RdfProperty {
	private final Set<RdfTypeInfo> mostSpecificValues;

	public RdfType(Entity owner) {
		super(owner, RDF.type);
		mostSpecificValues = new TreeSet<>();
	}

	public void addValue(Resource type) {
		addValueInternal(type);
	}

	private void addValueInternal(Resource newType) {
		RdfTypeInfo newRTI = owner().entityFactory().rdfTypeTools().typeInfo(newType);
		if (newRTI == null) {
			throw new IllegalArgumentException("Unrecognized class IRI %1$s".formatted(newType));
		}

		// Check to see if this new type is implied by one of the types we already have:
		if (mostSpecificValues.stream().noneMatch(rti -> rti.isSubTypeOf(newType))) {
			// Now remove any types we already have that are implied by the new one:
			mostSpecificValues.removeIf(rti -> newRTI.isSubTypeOf(rti.iri()));
			mostSpecificValues.add(newRTI);
		}
	}

	public boolean isSubClassOf(Resource superClass) {
		return mostSpecificValues.stream().anyMatch(ci -> ci.isSubTypeOf(superClass));
	}

	public Set<RdfTypeInfo> values() {
		return Collections.unmodifiableSet(mostSpecificValues);
	}

	public Stream<RdfTypeInfo> stream() {
		return mostSpecificValues.stream();
	}

	public RdfTypeInfo firstValue() {
		return mostSpecificValues.stream().findFirst().orElse(null);
	}

	@Override
	public void generateRdf(Model model) {
		if (!mostSpecificValues.isEmpty()) {
			Resource subject = model.createResource(owner().iri());
			mostSpecificValues.stream()
				.map(ci -> model.createResource(ci.iri()))
				.forEach(object -> model.add(subject, propIri().as(Property.class), object));
		}
	}

	@Override
	public int size() {
		return mostSpecificValues.size();
	}

	@Override
	public boolean isEmpty() {
		return mostSpecificValues.isEmpty();
	}

	@Override
	public boolean isValid(List<String> validationErrors, Model model) {
		return super.isValid(validationErrors, model);
	}
}
