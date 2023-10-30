package com.bbn.parliament.misc_needing_refactor;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

public final class Dmn {
	public static final String NS = "http://bbn.com/tbox/ix/example_domain#";

	// These IRIs are used only in domain-specific example code:
	public static final Resource Association = create("Association");
	public static final Resource EntityOfInterest = create("EntityOfInterest");

	// These are used in the ODDA code proper, but should become parameters to EntityFactory:
	public static final Resource orderIndex = create("orderIndex");

	private static Resource create(String localName) {
		return ResourceFactory.createResource(NS + localName);
	}

	private Dmn() {}	// prevent instantiation
}
