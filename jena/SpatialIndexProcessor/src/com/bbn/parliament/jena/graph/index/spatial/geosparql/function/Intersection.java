package com.bbn.parliament.jena.graph.index.spatial.geosparql.function;

import java.util.List;

import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionEnv;
import org.locationtech.jts.geom.Geometry;

import com.bbn.parliament.jena.graph.index.spatial.geosparql.datatypes.GeoSPARQLLiteral;

public class Intersection extends DoubleGeometrySpatialFunction {
	/** {@inheritDoc} */
	@Override
	protected NodeValue exec(Geometry g1, Geometry g2, GeoSPARQLLiteral datatype,
			Binding binding, List<NodeValue> evalArgs, String uri, FunctionEnv env) {
		Geometry toReturn = g1.intersection(g2);
		toReturn.setUserData(g1.getUserData());
		return makeNodeValue(toReturn, datatype);
	}

	/** {@inheritDoc} */
	@Override
	protected String[] getRestOfArgumentTypes() {
		return new String[] {};
	}
}
