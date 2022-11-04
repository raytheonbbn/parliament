package com.bbn.parliament.jena.graph.index.spatial.geosparql.function.util;

import java.util.List;

import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionEnv;
import org.apache.jena.vocabulary.XSD;

import com.bbn.parliament.jena.graph.index.spatial.geosparql.datatypes.WKTLiteral;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.function.GeoSPARQLFunctionException;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.function.SpatialFunctionBase;

/** @author rbattle */
public class CreateWKTPoint extends SpatialFunctionBase {
	@Override
	protected NodeValue exec(Binding binding, List<NodeValue> evalArgs,
		String uri, FunctionEnv env) {

		Node v1 = checkNodeValue(evalArgs.get(0));
		Node v2 = checkNodeValue(evalArgs.get(1));

		double lat = Double.parseDouble(v1.getLiteralValue().toString());
		double lon = Double.parseDouble(v2.getLiteralValue().toString());

		String wkt = "POINT(" + lon + " " + lat + ")";

		WKTLiteral lit = new WKTLiteral();
		return NodeValue.makeNode(ResourceFactory.createTypedLiteral(wkt, lit).asNode());
	}

	private static Node checkNodeValue(NodeValue nv) {
		Node n = nv.getNode();
		if (!(n.isLiteral() && n.getLiteralValue() instanceof Number)) {
			throw new GeoSPARQLFunctionException("%s is not a number".formatted(n));
		}
		return n;
	}

	@Override
	protected String[] getArgumentTypes() {
		return new String[] { XSD.xdouble.getURI(), XSD.xdouble.getURI() };
	}
}
