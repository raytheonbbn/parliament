package com.bbn.parliament.kb_graph.index.spatial.geosparql.function;

import java.util.List;

import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionEnv;
import org.locationtech.jts.geom.Geometry;

import com.bbn.parliament.kb_graph.index.spatial.geosparql.datatypes.GeoSPARQLLiteral;

/** @author rbattle */
public class ConvexHull extends SingleGeometrySpatialFunction {
	/** {@inheritDoc} */
	@Override
	protected NodeValue exec(Geometry g, GeoSPARQLLiteral datatype, Binding binding,
		List<NodeValue> evalArgs, String uri, FunctionEnv env) {
		Geometry toReturn = g.convexHull();
		toReturn.setUserData(g.getUserData());
		return makeNodeValue(toReturn, datatype);
	}

	/** {@inheritDoc} */
	@Override
	protected String[] getRestOfArgumentTypes() {
		return new String[] {};
	}
}
