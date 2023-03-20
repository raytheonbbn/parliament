package com.bbn.parliament.odda;

import java.util.List;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.bbn.parliament.misc_needing_refactor.QName;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

public abstract class RdfProperty implements Comparable<RdfProperty> {
	@ExcludeFromJson
	private final Entity owner;
	private final Resource propUri;
	private Cardinality cardinality;

	public RdfProperty(Entity owner, Resource propUri) {
		this.owner = ArgCheck.throwIfNull(owner, "owner");
		this.propUri = ArgCheck.throwIfNull(propUri, "propUri");
		cardinality = null; // NOPMD
	}

	public Entity getOwner() {
		return owner;
	}

	public Resource getPropUri() {
		return propUri;
	}

	public abstract void generateRdf(Model model);
	public abstract int size();
	public abstract boolean isEmpty();

	public boolean isValid(List<String> validationErrors, Model model) {
		return getValidationTools().isDomainValid(validationErrors, model,
			owner.getUri(), propUri);
	}

	public Cardinality getCardinality(Model model) {
		if (cardinality == null && owner != null && propUri != null) {
			cardinality = getValidationTools().getCardinality(model, owner, propUri);
		}
		return cardinality;
	}

	protected boolean maxCardinalityIsOne() {
		return getCardinality(null).max <= 1;
	}

	/** Use this when a cardinality restriction in the ontology is not present. */
	public void setCardinality(long minCard, long maxCard) {
		cardinality = new Cardinality(minCard, maxCard);
	}

	protected boolean isCardinalityValid(long actualCardinality, List<String> validationErrors, Model model) {
		boolean result = true;
		Cardinality card = getCardinality(model);
		if (actualCardinality < card.min || actualCardinality > card.max) {
			result = false;
			if (validationErrors != null) {
				validationErrors.add(String.format(
					"%1$s violates the %2$s cardinality constraint on property %3$s: Actual cardinality is %4$d", QName
						.asQName(owner.getUri()), card, QName.asQName(propUri), actualCardinality));
			}
		}
		return result;
	}

	protected boolean isRangeValid(Resource objectUri, List<String> validationErrors, Model model) {
		return getValidationTools().isRangeValid(objectUri, validationErrors, model, owner, propUri);
	}

	private ValidationTools getValidationTools() {
		return owner.getOntTools().getValidationTools();
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
				.append(propUri, that.propUri)
				.isEquals();

		}
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37)
			.append(owner)
			.append(propUri)
			.toHashCode();

	}

	@Override
	public int compareTo(RdfProperty rhs) {
		return new CompareToBuilder()
			.append(owner, rhs.owner)
			.append(propUri, rhs.propUri)
			.build();
	}
}
