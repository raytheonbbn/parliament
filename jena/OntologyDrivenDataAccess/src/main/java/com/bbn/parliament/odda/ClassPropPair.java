package com.bbn.parliament.odda;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

import com.bbn.parliament.misc_needing_refactor.QName;

public class ClassPropPair implements Comparable<ClassPropPair> {
	private final Resource rdfClassUri;
	private final Resource rdfPropUri;

	public ClassPropPair(Resource rdfClass, Resource rdfProp) {
		this.rdfClassUri = rdfClass;
		this.rdfPropUri = rdfProp;
	}

	public ClassPropPair(String rdfClassUri, String rdfPropUri) {
		this.rdfClassUri = ResourceFactory.createResource(rdfClassUri);
		this.rdfPropUri = ResourceFactory.createResource(rdfPropUri);
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37)
			.append(rdfClassUri)
			.append(rdfPropUri)
			.toHashCode();
	}

	@Override
	public int compareTo(ClassPropPair rhs) {
		return new CompareToBuilder()
			.append(rdfClassUri, rhs.rdfClassUri)
			.append(rdfPropUri, rhs.rdfPropUri)
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
				.append(rdfClassUri, other.rdfClassUri)
				.append(rdfPropUri, other.rdfPropUri)
				.isEquals();
		}
	}

	@Override
	public String toString() {
		return String.format("ClassPropPair[%1$s, %2$s]",
			QName.asQName(rdfClassUri), QName.asQName(rdfPropUri));
	}
}
