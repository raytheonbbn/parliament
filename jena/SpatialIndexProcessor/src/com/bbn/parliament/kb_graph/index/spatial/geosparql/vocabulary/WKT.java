package com.bbn.parliament.kb_graph.index.spatial.geosparql.vocabulary;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

public class WKT {
	public static final String DATATYPE_URI = "http://www.opengis.net/ont/sf#";
	public static final Resource WKTLiteral = ResourceFactory.createProperty(Geo.uri + "wktLiteral");

	public enum Type {
		Point, Curve, Surface, GeometryCollection, LineString, Polygon, PolyhedralSurface,
		MultiSurface, MultiCurve, MultiPoint, Line, LinearRing, MultiPolygon, MultiLineString;

		public Resource asResource() {
			return ResourceFactory.createResource(DATATYPE_URI + name());
		}

		public String getURI() {
			return asResource().getURI();
		}

		@Override
		public String toString() {
			return getURI();
		}

		public static Type valueOfURI(String uri) {
			for (Type t : Type.values()) {
				if (t.getURI().equals(uri)) {
					return t;
				}
			}
			return null;
		}
	}
}
