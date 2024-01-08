package com.bbn.parliament.odda;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import com.google.gson.stream.JsonWriter;

public class EntityImpl implements Entity {
	private Resource iri;
	private RdfType type;
	private final Map<Resource, DatatypeProperty> dtProps;
	private final Map<Resource, ObjectProperty> objProps;
	private final EntityFactory entFact;

	EntityImpl(Resource iri, EntityFactory entityFactory) {
		this.iri = ArgCheck.throwIfNull(iri, "iri");
		type = new RdfType(this);
		dtProps = new TreeMap<>();
		objProps = new TreeMap<>();
		entFact = ArgCheck.throwIfNull(entityFactory, "entityFactory");
	}

	@Override
	public Resource iri() {
		return iri;
	}

	@Override
	public void iri(Resource newValue) {
		iri = newValue;
	}

	@Override
	public boolean isFetched() {
		return (iri != null) && (!type.isEmpty()
			|| dtProps.values().stream().anyMatch(prop -> !prop.isEmpty())
			|| objProps.values().stream().anyMatch(prop -> !prop.isEmpty()));
	}

	@Override
	public long orderIndex() {
		String propVal = dtProp(entFact.orderIndexPredicate()).firstValue();
		return (propVal == null) ? Long.MAX_VALUE : Long.parseLong(propVal);
	}

	@Override
	public RdfType type() {
		return type;
	}

	@Override
	public void type(RdfType newType) {
		if (type == null || type.isEmpty()) {
			type = newType;
		} else {
			newType.values().forEach(ti -> type.addValue(ti.iri()));
		}
	}

	@Override
	public boolean isOfType(Resource typeToTest) {
		return type.isSubClassOf(typeToTest);
	}

	@Override
	public DatatypeProperty dtProp(Resource prop) {
		return dtProps.computeIfAbsent(prop, key -> new DatatypeProperty(this, key));
	}

	@Override
	public Collection<DatatypeProperty> dtProps() {
		return Collections.unmodifiableCollection(dtProps.values());
	}

	@Override
	public void addDTProp(DatatypeProperty newProp) {
		DatatypeProperty prop = dtProps.get(newProp.propIri());
		if (prop == null) {
			dtProps.put(newProp.propIri(), newProp);
		} else {
			newProp.values().forEach(prop::addValue);
		}
	}

	@Override
	public ObjectProperty objProp(Resource prop) {
		return objProps.computeIfAbsent(prop, key -> new ObjectProperty(this, key));
	}

	@Override
	public Collection<ObjectProperty> objProps() {
		return Collections.unmodifiableCollection(objProps.values());
	}

	@Override
	public void addObjProp(ObjectProperty newProp) {
		ObjectProperty prop = objProps.get(newProp.propIri());
		if (prop == null) {
			objProps.put(newProp.propIri(), newProp);
		} else {
			newProp.values().forEach(prop::addValue);
		}
	}

	@Override
	public void generateRdf(Model model) {
		type.generateRdf(model);
		dtProps.values().forEach((prop) -> prop.generateRdf(model));
		objProps.values().forEach((prop) -> prop.generateRdf(model));
	}

	@Override
	public void write(JsonWriter wtr) throws IOException {
		GsonUtil.create(entFact).toJson(this, getClass(), wtr);
	}

	@Override
	public String toJson(boolean pretty) {
		StringWriter sw = new StringWriter();
		try (JsonWriter jsonWriter = GsonUtil.createJsonWriter(sw, pretty)) {
			write(jsonWriter);
		} catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
		return sw.toString();
	}

	@Override
	public EntityFactory entityFactory() {
		return entFact;
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(41, 43)
			.append(iri)
			.toHashCode();
	}

	@Override
	public boolean equals(Object rhs) {
		if (this == rhs) {
			return true;
		} else if (rhs == null || getClass() != rhs.getClass()) {
			return false;
		} else {
			EntityImpl other = (EntityImpl) rhs;
			return new EqualsBuilder()
				.append(iri, other.iri)
				.isEquals();
		}
	}

	@Override
	public String toString() {
		return String.format("EntityImpl{iri=<%1$s>}", iri);
	}
}
