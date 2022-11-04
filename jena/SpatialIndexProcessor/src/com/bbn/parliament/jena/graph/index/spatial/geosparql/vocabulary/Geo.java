package com.bbn.parliament.jena.graph.index.spatial.geosparql.vocabulary;

import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

public class Geo {
	public static final String uri = "http://www.opengis.net/ont/geosparql#";

	protected static final Resource resource(String local) {
		return ResourceFactory.createResource(uri + local);
	}

	protected static final Property property(String local) {
		return ResourceFactory.createProperty(uri, local);
	}

	public static final Resource SpatialObject = resource("SpatialObject");
	public static final Resource Feature = resource("Feature");

	// simple features topological relations
	public static final Property sf_equals = property("sfEquals");
	public static final Property sf_disjoint = property("sfDisjoint");
	public static final Property sf_intersects = property("sfIntersects");
	public static final Property sf_touches = property("sfTouches");
	public static final Property sf_within = property("sfWithin");
	public static final Property sf_contains = property("sfContains");
	public static final Property sf_overlaps = property("sfOverlaps");
	public static final Property sf_crosses = property("sfCrosses");

	// egenhofer topological relations
	public static final Property eh_equals = property("ehEquals");
	public static final Property eh_disjoint = property("ehDisjoint");
	public static final Property eh_meet = property("ehMeet");
	public static final Property eh_overlap = property("ehOverlap");
	public static final Property eh_covers = property("ehCovers");
	public static final Property eh_coveredBy = property("ehCoveredBy");
	public static final Property eh_inside = property("ehInside");
	public static final Property eh_contains = property("ehContains");

	// rcc8 relations
	public static final Property rcc8_eq = property("rcc8eq");
	public static final Property rcc8_dc = property("rcc8dc");
	public static final Property rcc8_ec = property("rcc8ec");
	public static final Property rcc8_po = property("rcc8po");
	public static final Property rcc8_tppi = property("rcc8tppi");
	public static final Property rcc8_tpp = property("rcc8tpp");
	public static final Property rcc8_ntppi = property("rcc8ntppi");
	public static final Property rcc8_ntpp = property("rcc8ntpp");

	public static final Resource Geometry = resource("Geometry");

	public static final Property hasGeometry = property("hasGeometry");
	public static final Property defaultGeometry = property("defaultGeometry");
	public static final Property dimension = property("dimension");
	public static final Property coordinateDimension = property("coordinateDimension");
	public static final Property spatialDimension = property("spatialDimension");
	public static final Property isEmpty = property("isEmpty");
	public static final Property isSimple = property("isSimple");
	public static final Property is3D = property("is3D");

	public static final Property hasSerialization = property("hasSerialization");

	public static final Property asWKT = property("asWKT");
	public static final Property asGML = property("asGML");

	@SuppressWarnings("hiding")
	public static class Nodes {
		public static final Node SpatialObject = Geo.SpatialObject.asNode();
		public static final Node Feature = Geo.Feature.asNode();
		// simple features topological relations
		public static final Node sf_equals = Geo.sf_equals.asNode();
		public static final Node sf_disjoint = Geo.sf_equals.asNode();
		public static final Node sf_intersects = Geo.sf_equals.asNode();
		public static final Node sf_touches = Geo.sf_touches.asNode();
		public static final Node sf_within = Geo.sf_within.asNode();
		public static final Node sf_contains = Geo.sf_contains.asNode();
		public static final Node sf_overlaps = Geo.sf_overlaps.asNode();
		public static final Node sf_crosses = Geo.sf_crosses.asNode();

		// egenhofer topological relations
		public static final Node eh_equals = Geo.eh_equals.asNode();
		public static final Node eh_disjoint = Geo.eh_disjoint.asNode();
		public static final Node eh_meet = Geo.eh_meet.asNode();
		public static final Node eh_overlap = Geo.eh_overlap.asNode();
		public static final Node eh_covers = Geo.eh_covers.asNode();
		public static final Node eh_coveredBy = Geo.eh_coveredBy.asNode();
		public static final Node eh_inside = Geo.eh_inside.asNode();
		public static final Node eh_contains = Geo.eh_contains.asNode();

		// rcc8 relations
		public static final Node rcc8_eq = Geo.rcc8_eq.asNode();
		public static final Node rcc8_dc = Geo.rcc8_dc.asNode();
		public static final Node rcc8_ec = Geo.rcc8_ec.asNode();
		public static final Node rcc8_po = Geo.rcc8_po.asNode();
		public static final Node rcc8_tppi = Geo.rcc8_tppi.asNode();
		public static final Node rcc8_tpp = Geo.rcc8_tpp.asNode();
		public static final Node rcc8_ntppi = Geo.rcc8_ntppi.asNode();
		public static final Node rcc8_ntpp = Geo.rcc8_ntpp.asNode();

		public static final Node Geometry = Geo.Geometry.asNode();

		public static final Node hasGeometry = Geo.hasGeometry.asNode();
		public static final Node defaultGeometry = Geo.defaultGeometry.asNode();
		public static final Node dimension = Geo.dimension.asNode();
		public static final Node coordinateDimension = Geo.coordinateDimension.asNode();
		public static final Node spatialDimension = Geo.spatialDimension.asNode();
		public static final Node isEmpty = Geo.isEmpty.asNode();
		public static final Node isSimple = Geo.isSimple.asNode();
		public static final Node is3D = Geo.is3D.asNode();

		public static final Node asWKT = Geo.asWKT.asNode();
		public static final Node asGML = Geo.asGML.asNode();
	}
}
