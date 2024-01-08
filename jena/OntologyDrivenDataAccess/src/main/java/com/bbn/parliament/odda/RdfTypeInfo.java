package com.bbn.parliament.odda;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.jena.rdf.model.Resource;

public class RdfTypeInfo implements Comparable<RdfTypeInfo> {
	private final Resource iri;
	private final String label;
	private final Set<RdfTypeInfo> declaredSubTypes;
	private final Set<RdfTypeInfo> declaredSuperTypes;

	public RdfTypeInfo(Resource typeIri, String typeLabel) {
		iri = typeIri;
		label = typeLabel;
		declaredSubTypes = new TreeSet<>();
		declaredSuperTypes = new TreeSet<>();
	}

	public Resource iri() {
		return iri;
	}

	public String label() {
		return label;
	}

	public Set<RdfTypeInfo> declaredSuperTypes() {
		return Collections.unmodifiableSet(declaredSuperTypes);
	}

	public void addDeclaredSuperType(RdfTypeInfo newSuperType) {
		declaredSuperTypes.add(newSuperType);
	}

	public Set<RdfTypeInfo> declaredSubTypes() {
		return Collections.unmodifiableSet(declaredSubTypes);
	}

	public void addDeclaredSubType(RdfTypeInfo newSubType) {
		declaredSubTypes.add(newSubType);
	}

	public boolean isSubTypeOf(Resource superTypeIri) {
		return declaredSuperTypes.stream().anyMatch(rti ->
		superTypeIri.equals(rti.iri()) || rti.isSubTypeOf(superTypeIri));
	}

	public boolean isSuperTypeOf(Resource subTypeIri) {
		return declaredSubTypes.stream().anyMatch(rti ->
		subTypeIri.equals(rti.iri()) || rti.isSuperTypeOf(subTypeIri));
	}

	@Override
	public boolean equals(Object rhs) {
		if (this == rhs) {
			return true;
		} else if (rhs == null || getClass() != rhs.getClass()) {
			return false;
		} else {
			return new EqualsBuilder()
				.append(iri, ((RdfTypeInfo) rhs).iri)
				.isEquals();
		}
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(13, 29)
			.append(iri)
			.toHashCode();
	}

	@Override
	public int compareTo(RdfTypeInfo rhs) {
		return new CompareToBuilder()
			.append(iri, rhs.iri)
			.toComparison();
	}
}
