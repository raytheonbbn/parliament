package com.bbn.parliament.jena.graph.index.spatial.geosparql.datatypes;

import java.util.List;

import com.bbn.parliament.jena.graph.index.spatial.geosparql.function.SpatialFunctionBase;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.expr.NodeValue;
import com.hp.hpl.jena.sparql.function.FunctionEnv;
import com.hp.hpl.jena.vocabulary.XSD;
import org.locationtech.jts.geom.Geometry;

public abstract class ConstructorFunction<T extends GeoSPARQLLiteral> extends SpatialFunctionBase {
	private T datatype;

	public ConstructorFunction(T datatype) {
		this.datatype = datatype;
	}

	@Override
	protected String[] getArgumentTypes() {
		return new String[] { XSD.xstring.getURI() };
	}

	@Override
	protected NodeValue exec(Binding binding, List<NodeValue> evalArgs, String uri, FunctionEnv env) {
		Geometry g = datatype.parse(evalArgs.get(0).asString());
		return makeNodeValue(g, datatype);
	}
}
