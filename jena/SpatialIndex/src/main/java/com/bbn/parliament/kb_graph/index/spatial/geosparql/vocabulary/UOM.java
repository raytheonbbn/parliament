package com.bbn.parliament.kb_graph.index.spatial.geosparql.vocabulary;

import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

// Units Of Measure
public class UOM {
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
		public static final Node degree = UOM.degree.asNode();
		public static final Node metre = UOM.metre.asNode();
		public static final Node radian = UOM.radian.asNode();
		public static final Node unity = UOM.unity.asNode();
		public static final Node GridSpacing = UOM.GridSpacing.asNode();
	}
}
