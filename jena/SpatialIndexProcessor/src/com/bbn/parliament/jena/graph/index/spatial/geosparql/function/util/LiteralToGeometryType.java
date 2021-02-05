package com.bbn.parliament.jena.graph.index.spatial.geosparql.function.util;

import java.util.List;

import org.geotools.gml3.Curve;
import org.geotools.gml3.MultiCurve;
import org.geotools.gml3.MultiSurface;
import org.opengis.geometry.coordinate.ArcString;
import org.opengis.geometry.coordinate.PolyhedralSurface;
import org.opengis.geometry.coordinate.Tin;
import org.opengis.geometry.coordinate.Triangle;
import org.opengis.geometry.primitive.Surface;

import com.bbn.parliament.jena.graph.index.spatial.geosparql.datatypes.GMLLiteral;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.datatypes.GeoSPARQLLiteral;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.datatypes.WKTLiteral;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.function.SpatialFunctionBase;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.vocabulary.WKT;
import com.bbn.parliament.jena.graph.index.spatial.standard.StdConstants;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.QueryException;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.expr.NodeValue;
import com.hp.hpl.jena.sparql.function.FunctionEnv;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

/** @author rbattle */
public class LiteralToGeometryType extends SpatialFunctionBase {
	@Override
	protected NodeValue exec(Binding binding, List<NodeValue> evalArgs,
		String uri, FunctionEnv env) {
		NodeValue nv = evalArgs.get(0);
		checkGeometryLiteral(nv);
		GeoSPARQLLiteral lit = (GeoSPARQLLiteral)nv.asNode().getLiteralDatatype();
		Geometry g = (Geometry)nv.asNode().getLiteralValue();

		if (lit instanceof WKTLiteral) {
			String type = null;
			if (g instanceof Point) {
				type = "Point";
			} else if (g instanceof Curve) {
				type = "Curve";
			} else if (g instanceof LineString) {
				type = "LineString";
			} else if (g instanceof ArcString) {
				type = "ArcString";
			} else if (g instanceof Surface) {
				type = "Surface";
			} else if (g instanceof Polygon) {
				type = "Polygon";
			} else if (g instanceof Triangle) {
				type = "Triangle";
			} else if (g instanceof PolyhedralSurface) {
				type = "PolyhedralSurface";
			} else if (g instanceof Tin) {
				type = "TIN";
			} else if (g instanceof MultiPoint) {
				type = "MultiPoint";
			} else if (g instanceof MultiCurve) {
				type = "MultiCurve";
			} else if (g instanceof MultiLineString) {
				type = "MultiLineString";
			} else if (g instanceof MultiPolygon) {
				type = "MultiPolygon";
			} else if (g instanceof MultiSurface) {
				type = "MultiSurface";
			} else if (g instanceof GeometryCollection) {
				type = "GeometryCollection";
			}

			if (null == type) {
				throw new QueryException("Invalid geometry type: " + g.getClass().getName());
			}

			type = WKT.DATATYPE_URI + type;
			Node n = Node.createURI(type);
			return NodeValue.makeNode(n);
		} else if (lit instanceof GMLLiteral) {
		}
		return null;
	}

	@Override
	protected String[] getArgumentTypes() {
		return new String[] { StdConstants.OGC_NS + "GeomLiteral"  };
	}
}
