package com.bbn.parliament.jena.graph.index.spatial.geosparql.function.util;

import java.util.List;

import com.bbn.parliament.jena.graph.index.spatial.geosparql.datatypes.WKTLiteral;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.function.GeoSPARQLFunctionException;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.function.SpatialFunctionBase;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.expr.NodeValue;
import com.hp.hpl.jena.sparql.function.FunctionEnv;
import com.hp.hpl.jena.vocabulary.XSD;

/** @author rbattle */
public class CreateWKTPoint extends SpatialFunctionBase {
	@Override
	protected NodeValue exec(Binding binding, List<NodeValue> evalArgs,
		String uri, FunctionEnv env) {

		NodeValue v1 = evalArgs.get(0);
		NodeValue v2 = evalArgs.get(1);

		if (!v1.getNode().isLiteral() && v1.getNode().getLiteralValue() instanceof Number) {
			throw new GeoSPARQLFunctionException("%s is not a number".formatted(v1.getNode()));
		}
		if (!v2.getNode().isLiteral() && v2.getNode().getLiteralValue() instanceof Number) {
			throw new GeoSPARQLFunctionException("%s is not a number".formatted(v2.getNode()));
		}

		double lat = Double.parseDouble(v1.getNode().getLiteralValue().toString());
		double lon = Double.parseDouble(v2.getNode().getLiteralValue().toString());

		String wkt = "POINT(" + lon + " " + lat + ")";

		WKTLiteral lit = new WKTLiteral();
		return NodeValue.makeNode(ResourceFactory.createTypedLiteral(wkt, lit).asNode());
	}

	@Override
	protected String[] getArgumentTypes() {
		return new String[] { XSD.xdouble.getURI(), XSD.xdouble.getURI() };
	}
}
