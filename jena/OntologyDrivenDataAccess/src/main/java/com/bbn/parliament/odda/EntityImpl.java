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

import com.bbn.parliament.misc_needing_refactor.Dmn;
import com.google.gson.stream.JsonWriter;

public class EntityImpl implements Entity {
	private Resource uri;
	private RdfType type;
	private final Map<Resource, DatatypeProperty> dtProps;
	private final Map<Resource, ObjectProperty> objProps;
	private final OntologyTools ontTools;

	EntityImpl(Resource uri, OntologyTools ontTools) {
		this.uri = ArgCheck.throwIfNull(uri, "uri");
		type = new RdfType(this);
		dtProps = new TreeMap<>();
		objProps = new TreeMap<>();
		this.ontTools = ArgCheck.throwIfNull(ontTools, "ontTools");
	}

	@Override
	public Resource getUri() {
		return uri;
	}

	@Override
	public void setUri(Resource newValue) {
		uri = newValue;
	}

	@Override
	public boolean isFetched() {
		return (uri != null) && (!type.isEmpty()
			|| dtProps.values().stream().anyMatch(prop -> !prop.isEmpty())
			|| objProps.values().stream().anyMatch(prop -> !prop.isEmpty()));
	}

	@Override
	public long getOrderIndex() {
		String propVal = getDTProp(Dmn.orderIndex).getFirstValue();
		return (propVal == null) ? Long.MAX_VALUE : Long.parseLong(propVal);
	}

	@Override
	public RdfType getType() {
		return type;
	}

	@Override
	public void setType(RdfType newType) {
		if (type == null || type.isEmpty()) {
			type = newType;
		} else {
			newType.getValues().forEach(ci -> type.addValue(ci.getUri()));
		}
	}

	@Override
	public boolean isOfType(Resource typeToTest) {
		return type.isSubClassOf(typeToTest);
	}

	@Override
	public DatatypeProperty getDTProp(Resource prop) {
		return dtProps.computeIfAbsent(prop, key -> new DatatypeProperty(this, key));
	}

	@Override
	public Collection<DatatypeProperty> getDTProps() {
		return Collections.unmodifiableCollection(dtProps.values());
	}

	@Override
	public void addDTProp(DatatypeProperty newProp) {
		DatatypeProperty prop = dtProps.get(newProp.getPropUri());
		if (prop == null) {
			dtProps.put(newProp.getPropUri(), newProp);
		} else {
			newProp.getValues().forEach(prop::addValue);
		}
	}

	@Override
	public ObjectProperty getObjProp(Resource prop) {
		return objProps.computeIfAbsent(prop, key -> new ObjectProperty(this, key));
	}

	@Override
	public Collection<ObjectProperty> getObjProps() {
		return Collections.unmodifiableCollection(objProps.values());
	}

	@Override
	public void addObjProp(ObjectProperty newProp) {
		ObjectProperty prop = objProps.get(newProp.getPropUri());
		if (prop == null) {
			objProps.put(newProp.getPropUri(), newProp);
		} else {
			newProp.getValues().forEach(prop::addValue);
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
		GsonUtil.create(ontTools).toJson(this, getClass(), wtr);
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
	public OntologyTools getOntTools() {
		return ontTools;
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(41, 43)
			.append(uri)
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
				.append(uri, other.uri)
				.isEquals();
		}
	}

	@Override
	public String toString() {
		return String.format("EntityImpl{uri=<%1$s>}", uri);
	}
}
