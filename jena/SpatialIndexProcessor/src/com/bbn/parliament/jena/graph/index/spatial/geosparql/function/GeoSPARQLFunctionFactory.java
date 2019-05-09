package com.bbn.parliament.jena.graph.index.spatial.geosparql.function;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.bbn.parliament.jena.graph.index.spatial.Constants;
import com.bbn.parliament.jena.graph.index.spatial.IterableFunctionFactory;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.datatypes.GMLLiteral;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.datatypes.WKTLiteral;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.function.RelationFunction.EH_Contains;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.function.RelationFunction.EH_CoveredBy;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.function.RelationFunction.EH_Covers;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.function.RelationFunction.EH_Disjoint;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.function.RelationFunction.EH_Equals;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.function.RelationFunction.EH_Inside;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.function.RelationFunction.EH_Meet;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.function.RelationFunction.EH_Overlap;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.function.RelationFunction.RCC8_DC;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.function.RelationFunction.RCC8_EC;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.function.RelationFunction.RCC8_EQ;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.function.RelationFunction.RCC8_NTTP;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.function.RelationFunction.RCC8_NTTPI;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.function.RelationFunction.RCC8_PO;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.function.RelationFunction.RCC8_TPP;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.function.RelationFunction.RCC8_TPPI;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.function.RelationFunction.SF_Contains;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.function.RelationFunction.SF_Crosses;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.function.RelationFunction.SF_Disjoint;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.function.RelationFunction.SF_Equals;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.function.RelationFunction.SF_Intersects;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.function.RelationFunction.SF_Overlaps;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.function.RelationFunction.SF_Touches;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.function.RelationFunction.SF_Within;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.function.util.CreateWKTPoint;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.function.util.LiteralToGeometryType;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.function.util.Transform;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.vocabulary.GML;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.vocabulary.Geof;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.vocabulary.WKT;
import com.hp.hpl.jena.query.QueryExecException;
import com.hp.hpl.jena.sparql.function.Function;

public class GeoSPARQLFunctionFactory implements IterableFunctionFactory {
	public Map<String, Class<? extends SpatialFunctionBase>> functions;

	public GeoSPARQLFunctionFactory() {
		functions = new HashMap<>();
		functions.put(Geof.distance.getURI(), Distance.class);
		functions.put(Geof.buffer.getURI(), Buffer.class);
		functions.put(Geof.convexHull.getURI(), ConvexHull.class);
		functions.put(Geof.intersection.getURI(), Intersection.class);
		functions.put(Geof.union.getURI(), Union.class);
		functions.put(Geof.difference.getURI(), Difference.class);
		functions.put(Geof.symDifference.getURI(), SymDifference.class);
		functions.put(Geof.envelope.getURI(), Envelope.class);
		functions.put(Geof.boundary.getURI(), Boundary.class);
		functions.put(Geof.relate.getURI(), Relate.class);

		// OGC Simple Features functions
		functions.put(Geof.sf_equals.getURI(), SF_Equals.class);
		functions.put(Geof.sf_disjoint.getURI(), SF_Disjoint.class);
		functions.put(Geof.sf_intersects.getURI(), SF_Intersects.class);
		functions.put(Geof.sf_touches.getURI(), SF_Touches.class);
		functions.put(Geof.sf_crosses.getURI(), SF_Crosses.class);
		functions.put(Geof.sf_within.getURI(), SF_Within.class);
		functions.put(Geof.sf_contains.getURI(), SF_Contains.class);
		functions.put(Geof.sf_overlaps.getURI(), SF_Overlaps.class);

		// Egenhofer functions
		functions.put(Geof.eh_equals.getURI(), EH_Equals.class);
		functions.put(Geof.eh_disjoint.getURI(), EH_Disjoint.class);
		functions.put(Geof.eh_meet.getURI(), EH_Meet.class);
		functions.put(Geof.eh_overlap.getURI(), EH_Overlap.class);
		functions.put(Geof.eh_covers.getURI(), EH_Covers.class);
		functions.put(Geof.eh_coveredBy.getURI(), EH_CoveredBy.class);
		functions.put(Geof.eh_inside.getURI(), EH_Inside.class);
		functions.put(Geof.eh_contains.getURI(), EH_Contains.class);

		// RCC8 functions
		functions.put(Geof.rcc8_eq.getURI(), RCC8_EQ.class);
		functions.put(Geof.rcc8_dc.getURI(), RCC8_DC.class);
		functions.put(Geof.rcc8_ec.getURI(), RCC8_EC.class);
		functions.put(Geof.rcc8_po.getURI(), RCC8_PO.class);
		functions.put(Geof.rcc8_tppi.getURI(), RCC8_TPPI.class);
		functions.put(Geof.rcc8_tpp.getURI(), RCC8_TPP.class);
		functions.put(Geof.rcc8_ntpp.getURI(), RCC8_NTTP.class);
		functions.put(Geof.rcc8_ntppi.getURI(), RCC8_NTTPI.class);

		// extra functions

		// constructors for literals
		functions.put(WKT.WKTLiteral.getURI(), WKTLiteral.WKTConstructor.class);
		functions.put(GML.GMLLiteral.getURI(), GMLLiteral.GMLConstructor.class);

		// extra utility functions
		functions.put(Constants.SPATIAL_FUNCTION_NS + "transform", Transform.class);
		functions.put(Constants.SPATIAL_FUNCTION_NS + "makeWKTPoint", CreateWKTPoint.class);
		functions.put(Constants.SPATIAL_FUNCTION_NS + "WKTToGeometryPoint", LiteralToGeometryType.class);
	}

	@Override
	public Function create(String uri) {
		Class<? extends SpatialFunctionBase> imp = functions.get(uri);
		if (null == imp) {
			throw new QueryExecException(String.format("%s is not a valid function", uri));
		}
		try {
			return imp.newInstance();
		} catch (InstantiationException | IllegalAccessException ex) {
			throw new QueryExecException(String.format(
				"Could not instantiate function for %1$s", uri), ex);
		}
	}

	public String[] getURIs() {
		return functions.keySet().toArray(new String[] { });
	}

	@Override
	public Iterator<String> iterator() {
		return functions.keySet().iterator();
	}
}
