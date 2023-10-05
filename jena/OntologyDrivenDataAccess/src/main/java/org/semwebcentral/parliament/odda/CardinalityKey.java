package org.semwebcentral.parliament.odda;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.jena.rdf.model.Resource;

public class CardinalityKey implements Comparable<CardinalityKey> {
	public final Resource cls;
	public final Resource prop;

	public CardinalityKey(Resource cls, Resource prop) {
		this.cls = cls;
		this.prop = prop;
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(19, 31)
			.append(cls)
			.append(prop)
			.toHashCode();
	}

	@Override
	public int compareTo(CardinalityKey rhs) {
		return new CompareToBuilder()
			.append(cls, rhs.cls)
			.append(prop, rhs.prop)
			.toComparison();
	}

	@Override
	public boolean equals(Object rhs) {
		if (this == rhs) {
			return true;
		} else if (rhs == null || getClass() != rhs.getClass()) {
			return false;
		} else {
			CardinalityKey other = (CardinalityKey) rhs;
			return new EqualsBuilder()
				.append(cls, other.cls)
				.append(prop, other.prop)
				.isEquals();
		}
	}
}
