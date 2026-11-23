package com.bbn.parliament.odda;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

public class ObjectProperty extends RdfProperty implements Iterable<Entity> {
	private final Set<Entity> values;

	public ObjectProperty(Entity owner, Resource propIri) {
		super(owner, propIri);
		values = new TreeSet<>(Entity.COMPARATOR);
	}

	public void addValue(Entity ent) {
		values.add(ent);
	}

	public Set<Entity> values() {
		return Collections.unmodifiableSet(values);
	}

	public Stream<Entity> stream() {
		return values.stream();
	}

	public Entity firstValue() {
		return values.stream().findFirst().orElse(null);
	}

	@Override
	public void generateRdf(Model model) {
		if (!values.isEmpty()) {
			Resource subject = model.createResource(owner().iri());
			values.forEach(ent -> {
				model.add(subject, propIri().as(Property.class), model.createResource(ent.iri()));
				ent.generateRdf(model);
			});
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
			&& isCardinalityValid(values.size(), validationErrors, model)
			&& values.stream()
				.map(ent -> isRangeValid(ent.iri(), validationErrors, model))
				.reduce(true, (result, element) -> result && element);
	}

	@Override
	public Iterator<Entity> iterator() {
		return values.iterator();
	}
}
