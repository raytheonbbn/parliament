// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.graph.index.spatial.standard.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.geotools.referencing.datum.DefaultEllipsoid;

import com.bbn.parliament.jena.graph.index.spatial.standard.SpatialGeometryFactory;
import com.vividsolutions.jts.algorithm.MinimumDiameter;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class FloatingCircle extends EphemeralGeometry {
	private static final long serialVersionUID = 1L;

	private double _radius;

	public FloatingCircle(GeometryFactory factory, double radius) {
		super(factory);
		_radius = radius;
	}

	public double getRadius() {
		return _radius;
	}

	@Override
	public String toString() {
		return "Floating Circle: " + _radius;
	}

	public Geometry computeFloatingRegion(List<Geometry> extents) {
		Set<Coordinate> pointSet = new HashSet<>();

		for (Geometry extent : extents) {
			if (extent instanceof Point) {
				Point point = (Point) extent;
				pointSet.add(point.getCoordinate());

			} else if (extent instanceof LineString) {
				LineString path = (LineString) extent;
				pointSet.addAll(Arrays.asList(path.getCoordinates()));
			} else if (extent instanceof Polygon) {
				pointSet.addAll(Arrays.asList(extent.getCoordinates()));
			}
		}

		List<Coordinate> points = new ArrayList<>(pointSet);

		if (points.size() == 1) {
			Point p = factory.createPoint(points.get(0));
			BufferedGeometry bg = SpatialGeometryFactory.createBufferedGeometry(p, _radius);
			return bg.getBufferedGeometry();
		} else if (points.size() == 2) {
			Coordinate p1 = points.get(0);
			Coordinate p2 = points.get(1);
			double distance = distanceMeters(p1, p2);
			Coordinate center = new Coordinate((p1.x + p2.x) / 2d, (p1.y + p2.y) / 2d);
			Point point = factory.createPoint(center);
			if (distance <= getMaximumLength()) {
				Geometry geometry = SpatialGeometryFactory.createBufferedGeometry(point, _radius).getBufferedGeometry();
				return geometry;
			}
			return null;
		}

		MultiPoint multiPoint = factory.createMultiPoint(points.toArray(new Coordinate[] { }));
		MinimumDiameter md = new MinimumDiameter(multiPoint);

		LineString ls = md.getDiameter();
		Point center = ls.getCentroid();
		Coordinate centerCoord = center.getCoordinate();
		for (Coordinate c : points) {
			if (distanceMeters(c, centerCoord) > _radius) {
				return null;
			}
		}
		BufferedGeometry bg = SpatialGeometryFactory.createBufferedGeometry(center, _radius);
		return bg.getBufferedGeometry();
	}

	private static double distanceMeters(Coordinate c1, Coordinate c2) {
		return DefaultEllipsoid.WGS84.orthodromicDistance(c1.x, c1.y, c2.x, c2.y);
	}

	public double getMaximumLength() {
		return 2 * _radius;
	}

	/** {@inheritDoc} */
	@Override
	public String getGeometryType() {
		return "Floating Circle";
	}

	/** {@inheritDoc} */
	@Override
	public int getNumPoints() {
		// TODO Auto-generated method stub
		return 0;
	}
}
