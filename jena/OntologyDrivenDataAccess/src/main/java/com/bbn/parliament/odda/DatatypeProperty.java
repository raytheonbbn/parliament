package com.bbn.parliament.odda;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

public class DatatypeProperty extends RdfProperty implements Iterable<RdfLiteral> {
	private final Set<RdfLiteral> values;

	public DatatypeProperty(Entity owner, Resource propIri) {
		super(owner, propIri);
		values = new TreeSet<>();
	}

	public void addValue(RdfLiteral value) {
		if (value != null) {
			values.add(value);
		}
	}

	public void addValue(Literal value) {
		values.add(new RdfLiteral(value));
	}

	public void clear() {
		values.clear();
	}

	public Set<RdfLiteral> values() {
		return Collections.unmodifiableSet(values);
	}

	public Stream<RdfLiteral> stream() {
		return values.stream();
	}

	public String firstValue() {
		return values.stream()
			.map(RdfLiteral::lexicalForm)
			.findFirst()
			.orElse(null);
	}

	@Override
	public void generateRdf(Model model) {
		if (!values.isEmpty()) {
			Resource subject = model.createResource(owner().iri());
			values.stream()
				.map(value -> value.asLiteral(model))
				.forEach(lit -> model.add(subject, propIri().as(Property.class), lit));
		}
	}

	@Override
	public int size() {
		return values.size();
	}

	@Override
	public boolean isEmpty() {
		return values.isEmpty();
	}

	@Override
	public boolean isValid(List<String> validationErrors, Model model) {
		return super.isValid(validationErrors, model)
			&& isCardinalityValid(values.size(), validationErrors, model);
	}

	@Override
	public Iterator<RdfLiteral> iterator() {
		return values.iterator();
	}
}
