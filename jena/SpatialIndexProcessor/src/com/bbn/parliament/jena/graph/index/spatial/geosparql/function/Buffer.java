package com.bbn.parliament.jena.graph.index.spatial.geosparql.function;

import java.util.List;

import org.apache.jena.graph.Node;
import org.apache.jena.query.QueryExecException;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionEnv;
import org.apache.jena.vocabulary.XSD;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.Geometry;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.bbn.parliament.jena.graph.index.spatial.geosparql.datatypes.GeoSPARQLLiteral;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.vocabulary.UOM;
import com.bbn.parliament.jena.graph.index.spatial.standard.SpatialGeometryFactory;

public class Buffer extends SingleGeometrySpatialFunction {
	/** {@inheritDoc} */
	@Override
	protected NodeValue exec(Geometry g, GeoSPARQLLiteral datatype,
		Binding binding, List<NodeValue> evalArgs, String uri, FunctionEnv env) {
		NodeValue radius = evalArgs.get(1);
		NodeValue units = evalArgs.get(2);

		checkUnits(units);
		Node rNode = radius.getNode();
		if (!rNode.isLiteral() || !(rNode.getLiteralValue() instanceof Number)) {
			throw new QueryExecException("%s is not a number".formatted(rNode));
		}
		double distance = ((Number) rNode.getLiteralValue()).doubleValue();
		Node unitsNode = units.getNode();

		Geometry buffered = g;
		if (UOM.Nodes.degree.equals(unitsNode)) {
			// UoM for WGS84 is degree
			buffered = g.buffer(distance);
		} else if (UOM.Nodes.radian.equals(unitsNode)) {
			buffered = g.buffer(Math.toDegrees(distance));
		} else if (UOM.Nodes.metre.equals(unitsNode)) {
			CoordinateReferenceSystem destination;
			MathTransform transform;
			MathTransform inverse;

			try {
				int srid = SpatialGeometryFactory.UTMZoneSRID(buffered.getEnvelope());
				destination = CRS.decode("EPSG:" + srid);
				boolean valid = false;
				if (null != destination) {
					transform = CACHE.getTransform(DefaultGeographicCRS.WGS84, destination);
					inverse = transform.inverse();

					buffered = JTS.transform(buffered, transform);
					buffered = buffered.buffer(distance);

					buffered = JTS.transform(buffered, inverse);
					valid = buffered.isValid();
				}
				if (!valid) {
					throw new GeoSPARQLFunctionException(
						"Invalid geometry when buffering: %1$s by %2$f"
						.formatted(g.toText(), distance));
				}
			} catch (MismatchedDimensionException | FactoryException | TransformException ex) {
				throw new GeoSPARQLFunctionException("Error while buffering: %1$s by %2$f"
					.formatted(g.toText(), distance), ex);
			}
		} else {
			throw new UnsupportedUnitsException(unitsNode);
		}

		buffered.setUserData(g.getUserData());
		return makeNodeValue(buffered, datatype);
	}

	/** {@inheritDoc} */
	@Override
	protected String[] getRestOfArgumentTypes() {
		return new String[] { XSD.xdouble.getURI(), XSD.anyURI.getURI() };
	}
}
