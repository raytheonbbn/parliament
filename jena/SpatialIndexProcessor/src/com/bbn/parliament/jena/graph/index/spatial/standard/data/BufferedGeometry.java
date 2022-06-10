// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.graph.index.spatial.standard.data;

import java.util.ArrayList;
import java.util.List;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.CRS.AxisOrder;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.operation.projection.PointOutsideEnvelopeException;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.buffer.BufferOp;
import org.locationtech.jts.operation.buffer.BufferParameters;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jena.graph.index.spatial.standard.SpatialGeometryFactory;
import com.bbn.parliament.jena.graph.index.spatial.standard.StdConstants;

/** @author Robert Battle */
public class BufferedGeometry extends EphemeralGeometry {
	private static final long serialVersionUID = 1L;
	protected static final Logger LOG = LoggerFactory.getLogger(BufferedGeometry.class);
	private Geometry extent;
	private Double distance;
	private Geometry buffer;
	private static CoordinateReferenceSystem WGS84_CRS;
	private static final BufferParameters BUFFER_PARAMS = new BufferParameters(
		StdConstants.SEGMENTS_PER_QUADRANT, BufferParameters.CAP_ROUND,
		BufferParameters.JOIN_ROUND, BufferParameters.DEFAULT_MITRE_LIMIT);

	static {
		try {
			WGS84_CRS = CRS.decode("EPSG:4326");
		} catch (FactoryException ex) {
			LOG.error("Exception while initializing WGS84 CRS:", ex);
		}
	}

	public BufferedGeometry(GeometryFactory factory, Geometry extent) {
		super(factory);
		this.extent = extent;
	}

	public BufferedGeometry(GeometryFactory factory, Geometry extent,
		double distance) {
		super(factory);
		this.extent = extent;
		this.distance = distance;
	}

	public Double getDistance() {
		return distance;
	}

	public void setDistance(Double distance) {
		this.distance = distance;
	}

	public Geometry getExtent() {
		return extent;
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return "BufferedGeometry: " + extent + ", " + distance;
	}

	/** {@inheritDoc} */
	@Override
	public String getGeometryType() {
		return "Buffer";
	}

	/** {@inheritDoc} */
	@Override
	public int getNumPoints() {
		return 0;
	}

	public Geometry getBufferedGeometry() {
		if (buffer != null) {
			return buffer;
		}
		if (distance == null) {
			return null;
		}
		CoordinateReferenceSystem destCRS = null;
		int srid = SpatialGeometryFactory.UTMZoneSRID(extent.getEnvelope());

		try {
			destCRS = CRS.decode("EPSG:" + srid, true);

			Geometry buffered = extent instanceof Polygon
				? projectAndBufferPoly(extent, distance, destCRS)
					: projectAndBuffer(extent, distance, destCRS);

			boolean canReproject = true;

			try {
				JTS.checkCoordinatesRange(buffered, DefaultGeographicCRS.WGS84);
				buffer = buffered;
			} catch (PointOutsideEnvelopeException e) {
				canReproject = false;
			}
			if (!canReproject) {
				if (extent instanceof Point) {
					Point point = (Point) extent;
					GeodeticCalculator calc = new GeodeticCalculator(WGS84_CRS);
					calc.setStartingGeographicPoint(point.getX(), point.getY());
					List<Coordinate> coords = new ArrayList<>();
					Coordinate first = null;
					for (int i = -180; i <= 180; i += 30) {
						calc.setDirection(i, distance);
						Coordinate coord = new Coordinate(
							calc.getDestinationGeographicPoint().getX(),
							calc.getDestinationGeographicPoint().getY());
						if (null == first) {
							first = coord;
						}
						coords.add(coord);
					}
					if (!coords.get(coords.size() - 1).equals(first)) {
						coords.add(first);
					}
					LinearRing shell = extent.getFactory().createLinearRing(
						coords.toArray(new Coordinate[] {}));
					buffer = extent.getFactory().createPolygon(shell, null);
				} else {
					Envelope e = extent.getEnvelopeInternal();
					GeodeticCalculator calc = new GeodeticCalculator();
					List<Coordinate> coords = new ArrayList<>();
					Coordinate first = null;

					for (int index = 0; index < 4; index++) {
						Coordinate point = null;
						int[] degrees;
						switch (index) {
						case 0:
							point = new Coordinate(e.getMaxX(), e.getMaxY());
							degrees = new int[] { 0, 30, 60, 90 };
							break;
						case 1:
							point = new Coordinate(e.getMaxX(), e.getMinY());
							degrees = new int[] { 90, 120, 150, 180 };
							break;
						case 2:
							point = new Coordinate(e.getMinX(), e.getMinY());
							degrees = new int[] { -180, -150, -120, -90 };
							break;
						case 3:
							point = new Coordinate(e.getMinX(), e.getMaxY());
							degrees = new int[] { -90, -60, -30, 0 };
							break;
						default:
							throw new RuntimeException("Invalid index");
						}

						if (CRS.getAxisOrder(DefaultGeographicCRS.WGS84).equals(AxisOrder.NORTH_EAST)) {
							calc.setStartingGeographicPoint(point.x, point.y);
						} else {
							calc.setStartingGeographicPoint(point.y, point.x);
						}

						for (int i : degrees) {
							Coordinate coord;
							calc.setDirection(i, distance);
							//convert back to the lat/lon
							if (CRS.getAxisOrder(DefaultGeographicCRS.WGS84).equals(AxisOrder.NORTH_EAST)) {
								coord = new Coordinate(
									calc.getDestinationGeographicPoint().getX(),
									calc.getDestinationGeographicPoint().getY());
							} else {
								coord = new Coordinate(
									calc.getDestinationGeographicPoint().getY(),
									calc.getDestinationGeographicPoint().getX());
							}

							if (null == first) {
								first = coord;
							}
							coords.add(coord);
						}
					}
					if (!coords.get(coords.size() - 1).equals(first)) {
						coords.add(first);
					}
					LinearRing shell = extent.getFactory()
						.createLinearRing(coords.toArray(new Coordinate[] {}));
					buffer = extent.getFactory().createPolygon(shell, null);
				}
			}
			return buffer;
		} catch (FactoryException | MismatchedDimensionException | TransformException ex) {
			LOG.error("Exception while getting buffered geometry:", ex);
		}
		return null;
	}

	private static Geometry projectAndBuffer(Geometry extent, double distance,
		CoordinateReferenceSystem destination)
			throws FactoryException, MismatchedDimensionException, TransformException {
		LOG.debug("Transforming: {} from {} to {}",
			new Object[] { extent.getEnvelope(),
				WGS84_CRS.getName().getCode(),
				destination.getName().getCode() });
		MathTransform transform = CRS.findMathTransform(WGS84_CRS, destination);
		MathTransform reverseTransform = transform.inverse();
		Geometry targetGeometry = JTS.transform(extent, transform);

		BufferOp op = new BufferOp(targetGeometry, BUFFER_PARAMS);

		Geometry buffered = op.getResultGeometry(distance);
		return JTS.transform(buffered, reverseTransform);
	}

	private static Geometry projectAndBufferPoly(Geometry geom, double distance, CoordinateReferenceSystem origCRS)
		throws FactoryException, MismatchedDimensionException, TransformException {
		Geometry pGeom = geom.getEnvelope();
		LOG.debug("Transforming: {} from {} to {}", pGeom,
			WGS84_CRS.getName().getCode(), origCRS.getName().getCode());
		double x = geom.getCoordinate().x;
		double y = geom.getCoordinate().y;
		String code = "AUTO:42001," + x + "," + y;
		@SuppressWarnings("unused")
		CoordinateReferenceSystem utm = CRS.decode(code, true);
		CoordinateReferenceSystem wgs84= CRS.decode("EPSG:4326", true);
		@SuppressWarnings("unused")
		CoordinateReferenceSystem wgs84inv= CRS.decode("EPSG:4326", false);

		CoordinateReferenceSystem google = CRS.decode("EPSG:3857", true);
		MathTransform WGS84toGoogle= CRS.findMathTransform(wgs84, google);
		MathTransform GoogletoWSG84= CRS.findMathTransform(google, wgs84);

		Geometry pOrigToGoogle = JTS.transform(geom, WGS84toGoogle);
		Geometry pOrigToGoogleBuffer = pOrigToGoogle.buffer(distance, 20);
		Geometry pGoogleToWSG84Buffer = JTS.transform(pOrigToGoogleBuffer, GoogletoWSG84);
		Geometry retGeom =  pGoogleToWSG84Buffer;
		try {
			JTS.checkCoordinatesRange(retGeom, DefaultGeographicCRS.WGS84);
		} catch (PointOutsideEnvelopeException e) {
			LOG.warn("Buffer could not be transformed correctly.");
		}
		return retGeom;
	}

	@Override
	protected int getTypeCode() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected Geometry reverseInternal() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Geometry copyInternal() {
		// TODO Auto-generated method stub
		return null;
	}
}
