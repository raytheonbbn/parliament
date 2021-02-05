// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.graph.index.spatial;

import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jena.graph.KbGraph;
import com.bbn.parliament.jena.graph.index.spatial.standard.SpatialGeometryFactory;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.AnonId;

/** @author Robert Battle */
public class GeometryConverter {
	private static Logger LOG = LoggerFactory.getLogger(GeometryConverter.class);

	public static Point createPoint(double[] coord) {
		Coordinate c = new Coordinate(coord[1], coord[0]);
		Point p = SpatialGeometryFactory.GEOMETRY_FACTORY.createPoint(c);
		return p;
	}

	public static LineString createLineString(double[] points) {
		List<Coordinate> coords = new ArrayList<>();
		for (int i = 0; i < points.length; i += 2) {
			Coordinate c = new Coordinate(points[i + 1], points[i]);
			coords.add(c);
		}
		LineString string = SpatialGeometryFactory.GEOMETRY_FACTORY.createLineString(
			coords.toArray(new Coordinate[] {}));
		return string;
	}

	public static LinearRing createLinearRing(double[] ring) {
		List<Coordinate> coords = new ArrayList<>();
		for (int i = 0; i < ring.length; i += 2) {
			Coordinate c = new Coordinate(ring[i + 1], ring[i]);
			coords.add(c);
		}
		if (!(coords.get(0).equals(coords.get(coords.size() - 1)))) {
			coords.add(coords.get(0));
		}
		LinearRing shell = SpatialGeometryFactory.GEOMETRY_FACTORY.createLinearRing(
			coords.toArray(new Coordinate[] {}));
		return shell;
	}

	public static Polygon createPolygon(double[] ring) {
		LinearRing shell = createLinearRing(ring);
		Polygon p = SpatialGeometryFactory.GEOMETRY_FACTORY.createPolygon(shell, null);
		return p;
	}

	public static Geometry convertSQLGeometry(Object value)
		throws SpatialIndexException {
		Geometry extent = null;

		byte[] wkb = (byte[]) value;
		WKBReader reader = new WKBReader(SpatialGeometryFactory.GEOMETRY_FACTORY);
		try {
			extent = reader.read(wkb);
		} catch (ParseException e) {
			LOG.error("ParseException", e);
			return null;
		}

		extent.setSRID(Constants.WGS84_SRID);
		return extent;
	}

	public static Node getNodeRepresentation(String representation) {
		Node result = null;
		if (representation.startsWith(KbGraph.MAGICAL_BNODE_PREFIX)) {
			result = Node.createAnon(AnonId.create(representation.substring(
				KbGraph.MAGICAL_BNODE_PREFIX.length())));
		} else {
			result = Node.createURI(representation);
		}
		return result;
	}

	public static String getStringRepresentation(Node n) {
		String stringRep = n.toString();
		if (n.isBlank()) {
			stringRep = KbGraph.MAGICAL_BNODE_PREFIX + stringRep;
		}
		return stringRep;
	}

	public static byte[] convertGeometry(Geometry extent) {
		WKBWriter writer = new WKBWriter();
		return writer.write(extent);
	}

	public static String makeBoundingBox(Geometry boundExtent) {
		double xmin = Double.MAX_VALUE;
		double xmax = Double.MIN_VALUE;
		double ymin = Double.MAX_VALUE;
		double ymax = Double.MIN_VALUE;
		for (Coordinate c : boundExtent.getCoordinates()) {
			if (c.x < xmin) {
				xmin = c.x;
			}
			if (c.x > xmax) {
				xmax = c.x;
			}
			if (c.y < ymin) {
				ymin = c.y;
			}
			if (c.y > ymax) {
				ymax = c.y;
			}
		}
		return "BOX(" + xmin + " " + ymin + ", " + xmax + " " + ymax + ")";
	}
}
