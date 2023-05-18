package com.bbn.parliament.kb_graph.index.spatial.geosparql.vocabulary;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

public class GML {
	public static final String DATATYPE_URI = "http://www.opengis.net/ont/gml#";
	public static final Resource GMLLiteral = ResourceFactory.createProperty(Geo.uri + "gmlLiteral");

	public enum Type {
		Point, LineString, Curve, OrientableCurve, LineStringSegment, ArcString, Arc, Circle,
		ArcStringByBuldge, ArcByBuldge, ArcByCenterPoint, CircleByCenterPoint, CubicSpline,
		BSpline, Clothoid, CompositeCurve, CompositeSolid, CompositeSurface, Cone, Cylinder,
		Geodesic, GeodesicString, GriddedSurfacePatch, LinearRing, MultiCurve, MultiSolid,
		MultiSurface, OffsetCurve, OrientableSurface, PolygonPatch, PolyhedralSurface,
		Rectangle, Ring, Solid, Sphere, Surface, Tin, Triangle, TriangulatedSurface;

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
