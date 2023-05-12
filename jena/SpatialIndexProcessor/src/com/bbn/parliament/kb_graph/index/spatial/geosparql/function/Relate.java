package com.bbn.parliament.kb_graph.index.spatial.geosparql.function;

import java.util.List;

import org.apache.jena.query.QueryExecException;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionEnv;
import org.locationtech.jts.geom.Geometry;

import com.bbn.parliament.kb_graph.index.spatial.geosparql.datatypes.GeoSPARQLLiteral;

/** @author rbattle */
public class Relate extends DoubleGeometrySpatialFunction {
	/** {@inheritDoc} */
	@Override
	protected NodeValue exec(Geometry g1, Geometry g2, GeoSPARQLLiteral datatype, Binding binding,
		List<NodeValue> evalArgs, String uri, FunctionEnv env) {

		String patternMatrix = evalArgs.get(2).getString();

		if (patternMatrix.length() != 9) {
			throw new QueryExecException(
				"'%1$s' is an invalid DE-9IM pattern matrix.It must be 9 characters long"
				.formatted(patternMatrix));
		}

		return relate(g1, g2, patternMatrix);
	}

	public static NodeValue relate(Geometry g1, Geometry g2, String patternMatrix) {
		if (g1.relate(g2, patternMatrix)) {
			return NodeValue.TRUE;
		}
		return NodeValue.FALSE;
	}

	/** {@inheritDoc} */
	@Override
	protected String[] getRestOfArgumentTypes() {
		return new String[] { "xsd:String" };
	}
}