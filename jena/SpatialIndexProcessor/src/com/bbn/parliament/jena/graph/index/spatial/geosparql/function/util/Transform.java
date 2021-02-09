package com.bbn.parliament.jena.graph.index.spatial.geosparql.function.util;

import java.util.List;

import org.locationtech.jts.geom.Geometry;

import com.bbn.parliament.jena.graph.index.spatial.geosparql.datatypes.GeoSPARQLLiteral;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.function.SingleGeometrySpatialFunction;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.expr.NodeValue;
import com.hp.hpl.jena.sparql.function.FunctionEnv;
import com.hp.hpl.jena.vocabulary.XSD;

public class Transform extends SingleGeometrySpatialFunction {
	/** {@inheritDoc} */
	@Override
	protected NodeValue exec(Geometry g, GeoSPARQLLiteral datatype,
		Binding binding, List<NodeValue> evalArgs, String uri, FunctionEnv env) {

		NodeValue destinationVal = evalArgs.get(1);
		String authDestination = GeoSPARQLLiteral.getCoordinateReferenceSystemCode(
			destinationVal.getNode().getURI());

		// set coordinate reference system
		g.setUserData(authDestination);

		return makeNodeValue(g, datatype);
	}

	/** {@inheritDoc} */
	@Override
	protected String[] getRestOfArgumentTypes() {
		return new String[] { XSD.anyURI.toString() };
	}
}
