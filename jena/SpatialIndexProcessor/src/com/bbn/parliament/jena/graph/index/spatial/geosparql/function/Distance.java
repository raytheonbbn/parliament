package com.bbn.parliament.jena.graph.index.spatial.geosparql.function;

import java.util.List;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.operation.distance.DistanceOp;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.bbn.parliament.jena.graph.index.spatial.geosparql.datatypes.GeoSPARQLLiteral;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.vocabulary.UOM;
import com.bbn.parliament.jena.graph.index.spatial.standard.SpatialGeometryFactory;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.QueryExecException;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.expr.NodeValue;
import com.hp.hpl.jena.sparql.function.FunctionEnv;

public class Distance extends DoubleGeometrySpatialFunction {
	private final GeodeticCalculator calc;

	public Distance() {
		calc = new GeodeticCalculator(DefaultGeographicCRS.WGS84);
	}

	/** {@inheritDoc} */
	@Override
	protected NodeValue exec(Geometry g1, Geometry g2,
		GeoSPARQLLiteral datatype, Binding binding, List<NodeValue> evalArgs,
		String uri, FunctionEnv env) {
		NodeValue units = evalArgs.get(2);

		checkUnits(units);

		Geometry m1 = g1;
		Geometry m2 = g2;
		CoordinateReferenceSystem destination;
		MathTransform transform;
		double distance = Double.MAX_VALUE;
		Node unitsNode = units.getNode();
		if (UOM.Nodes.metre.equals(unitsNode)) {
			// transform to UTM zones
			int srid1 = SpatialGeometryFactory.UTMZoneSRID(m1.getEnvelope());
			int srid2 = SpatialGeometryFactory.UTMZoneSRID(m2.getEnvelope());
			try {
				if (srid1 == srid2) {
					destination = CRS.decode("EPSG:" + srid1);

					transform = CACHE.getTransform(DefaultGeographicCRS.WGS84,
						destination);

					m1 = JTS.transform(m1, transform);
					m2 = JTS.transform(m2, transform);
					distance = m1.distance(m2);
				} else {
					// use spheroid distance
					Coordinate[] points = DistanceOp.nearestPoints(m1, m2);

					calc.setStartingGeographicPoint(points[0].x, points[0].y);
					calc.setDestinationGeographicPoint(points[1].x, points[1].y);

					distance = calc.getOrthodromicDistance();
				}
			} catch (TransformException e) {
				throw new QueryExecException(
					"Could not transform to or from CRS EPSG:" + srid1);
			} catch (NoSuchAuthorityCodeException e) {
				throw new QueryExecException("Could not find CRS for EPSG:" + srid1);
			} catch (FactoryException e) {
				throw new QueryExecException(
					"Could not find transformation between WGS84 and EPSG:" + srid1);
			}
		} else if (UOM.Nodes.degree.equals(unitsNode)) {
			// WGS84 UoM is degrees so nothing needs to change
			distance = m1.distance(m2);
		} else if (UOM.Nodes.radian.equals(unitsNode)) {
			distance = Math.toRadians(m1.distance(m2));
		} else {
			throw new UnsupportedUnitsException(unitsNode);
		}

		return NodeValue.makeDouble(distance);
	}

	/** {@inheritDoc} */
	@Override
	protected String[] getRestOfArgumentTypes() {
		return new String[] { "xsd:anyURI" };
	}
}
