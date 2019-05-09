package com.bbn.parliament.jena.graph.index.spatial.standard;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class StdConstants {
	public static final int SEGMENTS_PER_QUADRANT = 24;

	public static final String OGC_NS = "http://www.opengis.net/";
	public static final String SRS_NS = OGC_NS + "def/crs/";
	public static final String OGC_SRS_NS = SRS_NS + "OGC/1.3/";
	public static final String EPSG_SRS_NS = SRS_NS + "EPSG/0/";
	public static final String OGC_FUNCTION_NS = OGC_NS + "def/geosparql/function/";

	public static final String OGC_EXT_NS = "http://parliament.semwebcentral.org/spatial/ogc-extended#";
	public static final String RCC_EXT_NS = "http://parliament.semwebcentral.org/spatial/rcc-extended#";
	public static final String SPATIAL_RELATIONS_NS = "http://www.semwebcentral.org/ontology/spatialrelations/";
	public static final String GEORSS_NS = "http://www.georss.org/georss/";

	public static final String BUFFER_NS = "http://parliament.semwebcentral.org/spatial/buffer#";

	public static final String SPATIAL_BUFFER = BUFFER_NS + "Buffer";

	public static final String SPATIAL_EXTENT = BUFFER_NS + "extent";

	public static final String SPATIAL_DISTANCE = BUFFER_NS + "distance";

	public static final String GML_NS = "http://www.opengis.net/gml/";

	public static final String GML_RADIUS = GML_NS + "radius";
	public static final String GML_POS = GML_NS + "pos";

	public static final Node GML_POS_NODE = asNode(GML_POS);

	public static final String GML_POS_LIST = GML_NS + "posList";

	public static final Node GML_POS_LIST_NODE = asNode(GML_POS_LIST);

	public static final String GML_LINEAR_RING = GML_NS + "LinearRing";
	public static final String GML_EXTERIOR = GML_NS + "exterior";

	public static final Node GML_EXTERIOR_NODE = asNode(GML_EXTERIOR);

	public static final String GML_INTERIOR = GML_NS + "interior";

	public static final Node GML_INTERIOR_NODE = asNode(GML_INTERIOR);

	public static final String GML_LINE_STRING = GML_NS + "LineString";
	public static final String GML_POINT = GML_NS + "Point";

	public static final String GML_POLYGON = GML_NS + "Polygon";

	public static final String GML_CIRCLE = GML_NS + "Circle";

	public static final String GML_NS_H = "http://www.opengis.net/gml#";

	public static final String GML_RADIUS_H = GML_NS_H + "radius";
	public static final String GML_POS_H = GML_NS_H + "pos";

	public static final Node GML_POS_NODE_H = asNode(GML_POS_H);
	public static final String GML_POS_LIST_H = GML_NS_H + "posList";

	public static final Node GML_POS_LIST_NODE_H = asNode(GML_POS_LIST_H);
	public static final String GML_LINEAR_RING_H = GML_NS_H + "LinearRing";
	public static final String GML_EXTERIOR_H = GML_NS_H + "exterior";

	public static final Node GML_EXTERIOR_NODE_H = asNode(GML_EXTERIOR_H);

	public static final String GML_INTERIOR_H = GML_NS_H + "interior";

	public static final Node GML_INTERIOR_NODE_H = asNode(GML_INTERIOR_H);

	public static final String GML_LINE_STRING_H = GML_NS_H + "LineString";
	public static final String GML_POINT_H = GML_NS_H + "Point";

	public static final String GML_POLYGON_H = GML_NS_H + "Polygon";

	public static final String GML_CIRCLE_H = GML_NS_H + "Circle";

	public static final String[] VALID_TYPES = new String[] { GML_POINT,
		GML_POINT_H, GML_POLYGON, GML_POLYGON_H, GML_LINE_STRING, GML_LINE_STRING_H };

	private static Node asNode(String uri) {
		return ResourceFactory.createResource(uri).asNode();
	}
}
