package com.bbn.parliament.kb_graph.index.spatial.geosparql.vocabulary;

import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;

public class Geof {
	public static final String uri = "http://www.opengis.net/def/function/geosparql/";

	protected static final Property property(String local) {
		return ResourceFactory.createProperty(uri, local);
	}

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

	// non topological functions
	public static final Property distance = property("distance");
	public static final Property buffer = property("buffer");
	public static final Property convexHull = property("convexHull");
	public static final Property intersection = property("intersection");
	public static final Property union = property("union");
	public static final Property difference = property("difference");
	public static final Property symDifference = property("symDifference");
	public static final Property envelope = property("envelope");
	public static final Property boundary = property("boundary");
	public static final Property relate = property("relate");

	@SuppressWarnings("hiding")
	public static class Nodes {
		// simple features topological relations
		public static final Node sf_equals = Geof.sf_equals.asNode();
		public static final Node sf_disjoint = Geof.sf_equals.asNode();
		public static final Node sf_intersects = Geof.sf_equals.asNode();
		public static final Node sf_touches = Geof.sf_touches.asNode();
		public static final Node sf_within = Geof.sf_within.asNode();
		public static final Node sf_contains = Geof.sf_contains.asNode();
		public static final Node sf_overlaps = Geof.sf_overlaps.asNode();
		public static final Node sf_crosses = Geof.sf_crosses.asNode();

		// egenhofer topological relations
		public static final Node eh_equals = Geof.eh_equals.asNode();
		public static final Node eh_disjoint = Geof.eh_disjoint.asNode();
		public static final Node eh_meet = Geof.eh_meet.asNode();
		public static final Node eh_overlap = Geof.eh_overlap.asNode();
		public static final Node eh_covers = Geof.eh_covers.asNode();
		public static final Node eh_coveredBy = Geof.eh_coveredBy.asNode();
		public static final Node eh_inside = Geof.eh_inside.asNode();
		public static final Node eh_contains = Geof.eh_contains.asNode();

		// rcc8 relations
		public static final Node rcc8_eq = Geof.rcc8_eq.asNode();
		public static final Node rcc8_dc = Geof.rcc8_dc.asNode();
		public static final Node rcc8_ec = Geof.rcc8_ec.asNode();
		public static final Node rcc8_po = Geof.rcc8_po.asNode();
		public static final Node rcc8_tppi = Geof.rcc8_tppi.asNode();
		public static final Node rcc8_tpp = Geof.rcc8_tpp.asNode();
		public static final Node rcc8_ntppi = Geof.rcc8_ntppi.asNode();
		public static final Node rcc8_ntpp = Geof.rcc8_ntpp.asNode();

		// non topological functions
		public static final Node distance = Geof.distance.asNode();
		public static final Node buffer = Geof.buffer.asNode();
		public static final Node convexHull = Geof.convexHull.asNode();
		public static final Node intersection = Geof.intersection.asNode();
		public static final Node union = Geof.union.asNode();
		public static final Node difference = Geof.difference.asNode();
		public static final Node symDifference = Geof.symDifference.asNode();
		public static final Node envelope = Geof.envelope.asNode();
		public static final Node boundary = Geof.boundary.asNode();
		public static final Node relate = Geof.relate.asNode();
	}
}
