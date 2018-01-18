package com.bbn.parliament.jena.graph.index.spatial.geosparql.vocabulary;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class Units {
	public static final String uri = "http://www.opengis.net/def/uom/OGC/1.0/";

	protected static final Resource resource(String local) {
		return ResourceFactory.createResource(uri + local);
	}

	public static final Resource degree = resource("degree");
	public static final Resource metre = resource("metre");
	public static final Resource radian = resource("radian");
	public static final Resource unity = resource("unity");
	public static final Resource GridSpacing = resource("GridSpacing");

	@SuppressWarnings("hiding")
	public static class Nodes {
		public static final Node degree = Units.degree.asNode();
		public static final Node metre = Units.metre.asNode();
		public static final Node radian = Units.radian.asNode();
		public static final Node unity = Units.unity.asNode();
		public static final Node GridSpacing = Units.GridSpacing.asNode();
	}
}
