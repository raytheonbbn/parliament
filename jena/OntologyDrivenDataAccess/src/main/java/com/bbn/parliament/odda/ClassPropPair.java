package com.bbn.parliament.odda;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

public class ClassPropPair implements Comparable<ClassPropPair> {
	private final Resource rdfClassIri;
	private final Resource rdfPropIri;

	public ClassPropPair(Resource rdfClass, Resource rdfProp) {
		this.rdfClassIri = rdfClass;
		this.rdfPropIri = rdfProp;
	}

	public ClassPropPair(String rdfClassIri, String rdfPropIri) {
		this.rdfClassIri = ResourceFactory.createResource(rdfClassIri);
		this.rdfPropIri = ResourceFactory.createResource(rdfPropIri);
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37)
			.append(rdfClassIri)
			.append(rdfPropIri)
			.toHashCode();
	}

	@Override
	public int compareTo(ClassPropPair rhs) {
		return new CompareToBuilder()
			.append(rdfClassIri, rhs.rdfClassIri)
			.append(rdfPropIri, rhs.rdfPropIri)
			.toComparison();
	}

	@Override
	public boolean equals(Object rhs) {
		if (this == rhs) {
			return true;
		} else if (rhs == null || getClass() != rhs.getClass()) {
			return false;
		} else {
			ClassPropPair other = (ClassPropPair) rhs;
			return new EqualsBuilder()
				.appendSuper(super.equals(rhs))
				.append(rdfClassIri, other.rdfClassIri)
				.append(rdfPropIri, other.rdfPropIri)
				.isEquals();
		}
	}

	@Override
	public String toString() {
		return "ClassPropPair[%1$s, %2$s]".formatted(rdfClassIri, rdfPropIri);
	}
}
