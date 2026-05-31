package com.bbn.parliament.kb_graph.index.spatial.geosparql.function.util;

import java.util.List;

import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.QueryException;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionEnv;
import org.geotools.geometry.jts.CircularRing;
import org.geotools.geometry.jts.CircularString;
import org.geotools.geometry.jts.CompoundCurve;
import org.geotools.geometry.jts.CompoundRing;
import org.geotools.geometry.jts.CurvePolygon;
import org.geotools.geometry.jts.MultiCurve;
import org.geotools.geometry.jts.MultiSurface;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import com.bbn.parliament.kb_graph.index.spatial.geosparql.datatypes.GMLLiteral;
import com.bbn.parliament.kb_graph.index.spatial.geosparql.datatypes.GeoSPARQLLiteral;
import com.bbn.parliament.kb_graph.index.spatial.geosparql.datatypes.WKTLiteral;
import com.bbn.parliament.kb_graph.index.spatial.geosparql.function.SpatialFunctionBase;
import com.bbn.parliament.kb_graph.index.spatial.geosparql.vocabulary.WKT;
import com.bbn.parliament.kb_graph.index.spatial.standard.StdConstants;

/** @author rbattle */
public class LiteralToGeometryType extends SpatialFunctionBase {
	@Override
	protected NodeValue exec(Binding binding, List<NodeValue> evalArgs,
		String uri, FunctionEnv env) {
		var nv = evalArgs.get(0);
		checkGeometryLiteral(nv);
		var lit = (GeoSPARQLLiteral) nv.asNode().getLiteralDatatype();
		var g = (Geometry) nv.asNode().getLiteralValue();

		if (lit instanceof WKTLiteral) {
			// The commented cases were valid under GeoTools version 29.0,
			// but under version 34.4 those types seem to have disappeared:
			var type = switch (g) {
			case Point x					-> x.getClass().getSimpleName();
			case CompoundRing x			-> x.getClass().getSimpleName();
			case CircularRing x			-> x.getClass().getSimpleName();
			case LinearRing x				-> x.getClass().getSimpleName();
			case CompoundCurve x			-> x.getClass().getSimpleName();
			case CircularString x		-> x.getClass().getSimpleName();
			case LineString x				-> x.getClass().getSimpleName();
			case CurvePolygon x			-> x.getClass().getSimpleName();
			case Polygon x					-> x.getClass().getSimpleName();
			//case Curve x					-> x.getClass().getSimpleName();
			//case ArcString x			-> x.getClass().getSimpleName();
			//case Surface x				-> x.getClass().getSimpleName();
			//case Triangle x				-> x.getClass().getSimpleName();
			//case PolyhedralSurface x	-> x.getClass().getSimpleName();
			//case Tin _					-> "TIN";
			case MultiPoint x				-> x.getClass().getSimpleName();
			case MultiCurve x				-> x.getClass().getSimpleName();
			case MultiLineString x		-> x.getClass().getSimpleName();
			case MultiSurface x			-> x.getClass().getSimpleName();
			case MultiPolygon x			-> x.getClass().getSimpleName();
			case GeometryCollection x	-> x.getClass().getSimpleName();
			default							-> null;
			};

			if (null == type) {
				throw new QueryException("Invalid geometry type: " + g.getClass().getName());
			}

			return NodeValue.makeNode(
				NodeFactory.createURI(
					WKT.DATATYPE_URI + type));
		} else if (lit instanceof GMLLiteral) {
		}
		return null;
	}

	@Override
	protected String[] getArgumentTypes() {
		return new String[] { StdConstants.OGC_NS + "GeomLiteral" };
	}
}
