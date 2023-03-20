package com.bbn.parliament.odda;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class Geo {
	public static final String NS = "http://www.opengis.net/ont/geosparql#";

	// Classes:
	public static final Resource Geometry = create("Geometry");

	// Predicates:
	public static final Resource asWKT = create("asWKT");

	// Datatypes:
	public static final Resource wktLiteral = create("wktLiteral");

	private static Resource create(String localName) {
		return ResourceFactory.createResource(NS + localName);
	}

	private Geo() {}	// prevent instantiation
}
