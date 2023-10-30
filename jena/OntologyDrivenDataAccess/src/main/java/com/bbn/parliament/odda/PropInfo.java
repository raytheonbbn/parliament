package com.bbn.parliament.odda;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;

public class PropInfo {
	private final Resource prop;
	private final Resource propFamily;
	private final Set<Resource> domainTypes;
	private String jsonFieldName;

	public PropInfo(Resource property, Resource propertyFamily) {
		if (!OWL.DatatypeProperty.equals(propertyFamily) && !OWL.ObjectProperty.equals(propertyFamily)) {
			throw new IllegalArgumentException("propFamily must be either owl:DatatypeProperty or owl:ObjectProperty");
		}
		prop = property;
		propFamily = propertyFamily;
		domainTypes = new TreeSet<>();
	}

	public Resource getProp() {
		return prop;
	}

	public Resource getPropFamily() {
		return propFamily;
	}

	public boolean isDTProp() {
		return OWL.DatatypeProperty.equals(propFamily);
	}

	public boolean isObjProp() {
		return OWL.ObjectProperty.equals(propFamily);
	}

	public Set<Resource> getDomainTypes() {
		return Collections.unmodifiableSet(domainTypes);
	}

	public void addDomainType(Resource newDomainType) {
		if (newDomainType != null) {
			domainTypes.add(newDomainType);
		}
	}

	public String getJsonFieldName() {
		return jsonFieldName;
	}

	public void setJsonFieldName(String newValue) {
		jsonFieldName = newValue;
	}
}
