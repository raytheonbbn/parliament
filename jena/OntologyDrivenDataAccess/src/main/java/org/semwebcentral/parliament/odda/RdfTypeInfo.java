package org.semwebcentral.parliament.odda;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.jena.rdf.model.Resource;

public class RdfTypeInfo implements Comparable<RdfTypeInfo> {
	private final Resource uri;
	private final String label;
	private final Set<RdfTypeInfo> declaredSubTypes;
	private final Set<RdfTypeInfo> declaredSuperTypes;

	public RdfTypeInfo(Resource typeUri, String typeLabel) {
		uri = typeUri;
		label = typeLabel;
		declaredSubTypes = new TreeSet<>();
		declaredSuperTypes = new TreeSet<>();
	}

	public Resource getUri() {
		return uri;
	}

	public String getLabel() {
		return label;
	}

	public Set<RdfTypeInfo> getDeclaredSuperTypes() {
		return Collections.unmodifiableSet(declaredSuperTypes);
	}

	public void addDeclaredSuperType(RdfTypeInfo newSuperType) {
		declaredSuperTypes.add(newSuperType);
	}

	public Set<RdfTypeInfo> getDeclaredSubTypes() {
		return Collections.unmodifiableSet(declaredSubTypes);
	}

	public void addDeclaredSubType(RdfTypeInfo newSubType) {
		declaredSubTypes.add(newSubType);
	}

	public boolean isSubTypeOf(Resource superTypeUri) {
		return declaredSuperTypes.stream().anyMatch(rti ->
		superTypeUri.equals(rti.getUri()) || rti.isSubTypeOf(superTypeUri));
	}

	public boolean isSuperTypeOf(Resource subTypeUri) {
		return declaredSubTypes.stream().anyMatch(rti ->
		subTypeUri.equals(rti.getUri()) || rti.isSuperTypeOf(subTypeUri));
	}

	@Override
	public boolean equals(Object rhs) {
		if (this == rhs) {
			return true;
		} else if (rhs == null || getClass() != rhs.getClass()) {
			return false;
		} else {
			return new EqualsBuilder()
				.append(uri, ((RdfTypeInfo) rhs).uri)
				.isEquals();
		}
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(13, 29)
			.append(uri)
			.toHashCode();
	}

	@Override
	public int compareTo(RdfTypeInfo rhs) {
		return new CompareToBuilder()
			.append(uri, rhs.uri)
			.toComparison();
	}
}
