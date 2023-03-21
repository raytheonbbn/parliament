package com.bbn.parliament.odda;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.shared.PrefixMapping;

public class OntologyTools {
	private final Resource rootEntType;
	private final PrefixMapping pm;
	private final RdfTypeTools rdfTypeTools;
	private final RdfPropertyTools rdfPropTools;
	private final ValidationTools valTools;

	public OntologyTools(Resource rootEntityType, PrefixMapping prefixMapping, SparqlEndpointSink sparqlEndpointSink) {
		rootEntType = ArgCheck.throwIfNull(rootEntityType, "rootEntityType");
		pm = ArgCheck.throwIfNull(prefixMapping, "prefixMapping");
		rdfTypeTools = new RdfTypeTools(sparqlEndpointSink, prefixMapping);
		rdfPropTools = new RdfPropertyTools(sparqlEndpointSink, prefixMapping);
		valTools = new ValidationTools(sparqlEndpointSink, prefixMapping);
	}

	public Resource getRootEntityType() {
		return rootEntType;
	}

	public PrefixMapping getPrefixMapping() {
		return pm;
	}

	public RdfTypeTools getRdfTypeTools() {
		return rdfTypeTools;
	}

	public RdfPropertyTools getRdfPropertyTools() {
		return rdfPropTools;
	}

	public ValidationTools getValidationTools() {
		return valTools;
	}
}
