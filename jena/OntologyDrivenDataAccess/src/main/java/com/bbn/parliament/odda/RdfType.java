package com.bbn.parliament.odda;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import com.bbn.parliament.misc_needing_refactor.QName;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

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
		RdfTypeInfo newRTI = getOwner().getOntTools().getRdfTypeTools().getTypeInfo(newType);
		if (newRTI == null) {
			throw new IllegalArgumentException(String.format(
				"Unrecognized class IRI %1$s", QName.asQName(newType)));
		}

		// Check to see if this new type is implied by one of the types we already have:
		if (mostSpecificValues.stream().noneMatch(rti -> rti.isSubTypeOf(newType))) {
			// Now remove any types we already have that are implied by the new one:
			mostSpecificValues.removeIf(rti -> newRTI.isSubTypeOf(rti.getUri()));
			mostSpecificValues.add(newRTI);
		}
	}

	public boolean isSubClassOf(Resource superClass) {
		return mostSpecificValues.stream().anyMatch(ci -> ci.isSubTypeOf(superClass));
	}

	public Set<RdfTypeInfo> getValues() {
		return Collections.unmodifiableSet(mostSpecificValues);
	}

	public Stream<RdfTypeInfo> stream() {
		return mostSpecificValues.stream();
	}

	public RdfTypeInfo getFirstValue() {
		return mostSpecificValues.stream().findFirst().orElse(null);
	}

	@Override
	public void generateRdf(Model model) {
		if (!mostSpecificValues.isEmpty()) {
			Resource subject = model.createResource(getOwner().getUri());
			mostSpecificValues.stream()
				.map(ci -> model.createResource(ci.getUri()))
				.forEach(object -> model.add(subject, getPropUri().as(Property.class), object));
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
