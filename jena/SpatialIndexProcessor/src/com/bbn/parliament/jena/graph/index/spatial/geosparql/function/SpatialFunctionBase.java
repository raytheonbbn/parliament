package com.bbn.parliament.jena.graph.index.spatial.geosparql.function;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.jena.graph.Node;
import org.apache.jena.query.QueryBuildException;
import org.apache.jena.query.QueryExecException;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.Function;
import org.apache.jena.sparql.function.FunctionEnv;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.bbn.parliament.jena.graph.index.spatial.geosparql.TransformCache;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.datatypes.GeoSPARQLLiteral;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.vocabulary.GML;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.vocabulary.UOM;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.vocabulary.WKT;

public abstract class SpatialFunctionBase implements Function {
	protected static final TransformCache CACHE = new TransformCache();

	protected static void checkGeometryLiteral(NodeValue nv)
		throws QueryExecException {
		Node node = nv.asNode();
		if (!node.isLiteral() || !(node.getLiteralDatatype() instanceof GeoSPARQLLiteral)) {
			throw new QueryExecException("%s is not an %s or %s".formatted(
				node.toString(), WKT.WKTLiteral.getURI(), GML.GMLLiteral.getURI()));
		}
	}

	protected static void checkUnits(NodeValue nv) throws QueryExecException {
		Node node = nv.asNode();

		if (UOM.Nodes.metre.equals(node)) {
			return;
		}
		if (UOM.Nodes.degree.equals(node)) {
			return;
		}
		if (UOM.Nodes.GridSpacing.equals(node)) {
			return;
		}
		if (UOM.Nodes.radian.equals(node)) {
			return;
		}
		if (UOM.Nodes.unity.equals(node)) {
			return;
		}

		throw new UnsupportedUnitsException(node);
	}

	/** {@inheritDoc} */
	@Override
	public final NodeValue exec(Binding binding, ExprList args, String uri,
		FunctionEnv env) {
		List<NodeValue> evalArgs = new ArrayList<>();
		for (Iterator<Expr> iter = args.iterator(); iter.hasNext();) {
			Expr e = iter.next();
			NodeValue x = e.eval(binding, env);
			evalArgs.add(x);
		}
		return exec(binding, evalArgs, uri, env);
	}

	protected abstract NodeValue exec(Binding binding, List<NodeValue> evalArgs,
		String uri, FunctionEnv env);

	protected abstract String[] getArgumentTypes();

	/** {@inheritDoc} */
	@Override
	public final void build(String uri, ExprList args) {
		String[] argTypes = getArgumentTypes();
		int numArgs = argTypes.length;
		if ((null == args && numArgs != 0)
			|| (null != args && numArgs != args.size())) {
			StringBuilder message = new StringBuilder();
			for (int i = 0; i < argTypes.length; i++) {
				String arg = argTypes[i];
				message.append(arg);
				if (i < argTypes.length - 1) {
					message.append(", ");
				}
			}
			throw new QueryBuildException("%s requires %d arguments: %s".formatted(
				uri, argTypes.length, message));
		}
	}

	protected static Geometry project(Geometry geometry, String srsCode)
		throws QueryBuildException {
		if (srsCode.equals(geometry.getUserData())) {
			return geometry;
		}

		Geometry projected;
		try {
			// convert to different spatial reference
			CoordinateReferenceSystem source = CRS.decode(geometry.getUserData().toString());
			CoordinateReferenceSystem destination = CRS.decode(srsCode);

			MathTransform transform = CACHE.getTransform(source, destination);
			projected = JTS.transform(geometry, transform);
		} catch (MismatchedDimensionException | FactoryException | TransformException ex) {
			throw new QueryBuildException("Error while project geometry from %1$s to %2$s"
				.formatted(geometry.getUserData(), srsCode), ex);
		}
		return projected;
	}

	protected static NodeValue makeNodeValue(Geometry geom,
		GeoSPARQLLiteral datatype) {
		String lex = datatype.unparse(geom);
		return NodeValue.makeNode(lex, null, datatype.getURI());
	}
}
