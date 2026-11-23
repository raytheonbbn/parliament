package com.bbn.parliament.odda;

import java.util.List;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

public abstract class RdfProperty implements Comparable<RdfProperty> {
	@ExcludeFromJson
	private final Entity owner;
	private final Resource propIri;
	private Cardinality cardinality;

	public RdfProperty(Entity owner, Resource propIri) {
		this.owner = ArgCheck.throwIfNull(owner, "owner");
		this.propIri = ArgCheck.throwIfNull(propIri, "propIri");
		cardinality = null; // NOPMD
	}

	public Entity owner() {
		return owner;
	}

	public Resource propIri() {
		return propIri;
	}

	public abstract void generateRdf(Model model);
	public abstract int size();
	public abstract boolean isEmpty();

	public boolean isValid(List<String> validationErrors, Model model) {
		return validationTools().isDomainValid(validationErrors, model,
			owner.iri(), propIri);
	}

	public Cardinality cardinality(Model model) {
		if (cardinality == null && owner != null && propIri != null) {
			cardinality = validationTools().cardinality(model, owner, propIri);
		}
		return cardinality;
	}

	protected boolean maxCardinalityIsOne() {
		return cardinality(null).max <= 1;
	}

	/** Use this when a cardinality restriction in the ontology is not present. */
	public void cardinality(long minCard, long maxCard) {
		cardinality = new Cardinality(minCard, maxCard);
	}

	protected boolean isCardinalityValid(long actualCardinality, List<String> validationErrors, Model model) {
		boolean result = true;
		Cardinality card = cardinality(model);
		if (actualCardinality < card.min || actualCardinality > card.max) {
			result = false;
			if (validationErrors != null) {
				validationErrors.add(
					"%1$s violates the %2$s cardinality constraint on property %3$s: Actual cardinality is %4$d"
					.formatted(owner.iri(), card, propIri, actualCardinality));
			}
		}
		return result;
	}

	protected boolean isRangeValid(Resource objectIri, List<String> validationErrors, Model model) {
		return validationTools().isRangeValid(objectIri, validationErrors, model, owner, propIri);
	}

	private ValidationTools validationTools() {
		return owner.entityFactory().validationTools();
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		} else if (other == null || getClass() != other.getClass()) {
			return false;
		} else {
			RdfProperty that = (RdfProperty) other;
			return new EqualsBuilder()
				.append(owner, that.owner)
				.append(propIri, that.propIri)
				.isEquals();

		}
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37)
			.append(owner)
			.append(propIri)
			.toHashCode();

	}

	@Override
	public int compareTo(RdfProperty rhs) {
		return new CompareToBuilder()
			.append(owner, rhs.owner)
			.append(propIri, rhs.propIri)
			.build();
	}
}
