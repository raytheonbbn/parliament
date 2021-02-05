package com.bbn.parliament.jena.graph.index.spatial.geosparql.function;

import java.util.List;

import com.bbn.parliament.jena.graph.index.spatial.geosparql.datatypes.GeoSPARQLLiteral;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.expr.NodeValue;
import com.hp.hpl.jena.sparql.function.FunctionEnv;
import org.locationtech.jts.geom.Geometry;

/** @author rbattle */
public abstract class DoubleGeometrySpatialFunction extends SpatialFunctionBase {
	/** {@inheritDoc} */
	@Override
	protected NodeValue exec(Binding binding, List<NodeValue> evalArgs, String uri, FunctionEnv env) {
		NodeValue geom1 = evalArgs.get(0);
		NodeValue geom2 = evalArgs.get(1);

		checkGeometryLiteral(geom1);
		checkGeometryLiteral(geom2);

		Geometry g1 = (Geometry) geom1.getNode().getLiteralValue();
		Geometry g2 = (Geometry) geom2.getNode().getLiteralValue();

		GeoSPARQLLiteral datatype = (GeoSPARQLLiteral) geom1.getNode().getLiteralDatatype();

		return exec(g1, g2, datatype, binding, evalArgs, uri, env);
	}

	protected abstract NodeValue exec(Geometry g1, Geometry g2, GeoSPARQLLiteral datatype,
		Binding binding, List<NodeValue> evalArgs, String uri, FunctionEnv env);

	protected abstract String[] getRestOfArgumentTypes();

	/** {@inheritDoc} */
	@Override
	protected String[] getArgumentTypes() {
		String[] rest = getRestOfArgumentTypes();
		String[] args = new String[rest.length + 2];
		args[0] = "ogc:GeomLiteral";
		args[1] = "ogc:GeomLiteral";
		int i = 2;
		for (String s : rest) {
			args[i++] = s;
		}
		return args;
	}
}
