// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.graph.index.spatial.operands;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;

import com.bbn.parliament.jena.graph.index.spatial.GeometryConverter;
import com.bbn.parliament.jena.graph.index.spatial.Operation;

/** @author gjoiner */
@RunWith(JUnitPlatform.class)
public class OperandsTest {
	private static boolean performComparison(Geometry x, Geometry y, Operation operation) {
		return operation.relate(x, y);
	}

	@SuppressWarnings("static-method")
	@Test
	public void testConnected() {
		double[] xPoints = {30d, 30d, 30d, 40d, 40d, 40d, 40d, 30d};
		double[] yPoints = {30d, 30d, 30d, 40d, 40d, 40d, 40d, 30d};
		double[] xPointsFalse = {45d, 45d, 45d, 55, 55, 55, 55, 45d};

		LineString x = GeometryConverter.createLineString(xPoints);
		LineString y = GeometryConverter.createLineString(yPoints);
		LineString xFalse = GeometryConverter.createLineString(xPointsFalse);

		assertTrue(performComparison(x, y, Operation.RCC_EXT.CONNECTED));
		assertFalse(performComparison(xFalse, y, Operation.RCC_EXT.CONNECTED));
	}

	@SuppressWarnings("static-method")
	@Test
	public void testDisconnected() {
		double[] xPoints = {45d, 45d, 45d, 55, 55, 55, 55, 45d};
		double[] yPoints = {30d, 30d, 30d, 40d, 40d, 40d, 40d, 30d};
		double[] xPointsFalse = {30d, 30d, 30d, 40d, 40d, 40d, 40d, 30d};

		LineString x = GeometryConverter.createLineString(xPoints);
		LineString y = GeometryConverter.createLineString(yPoints);
		LineString xFalse = GeometryConverter.createLineString(xPointsFalse);

		assertTrue(performComparison(x, y, Operation.RCC8.DC));
		assertFalse(performComparison(xFalse, y, Operation.RCC8.DC));
	}

	@SuppressWarnings("static-method")
	@Test
	public void testIdentical() {
		double[] xPoints = {30d, 30d, 30d, 40d, 40d, 40d, 40d, 30d};
		double[] yPoints = {30d, 30d, 30d, 40d, 40d, 40d, 40d, 30d};
		double[] xPointsFalse = {45d, 45d, 45d, 55, 55, 55, 55, 45d};

		LineString x = GeometryConverter.createLineString(xPoints);
		LineString y = GeometryConverter.createLineString(yPoints);
		LineString xFalse = GeometryConverter.createLineString(xPointsFalse);

		assertTrue(performComparison(x, y, Operation.RCC8.EQ));
		assertFalse(performComparison(xFalse, y, Operation.RCC8.EQ));
	}

	@SuppressWarnings("static-method")
	@Test
	public void testExternallyConnected() {
		double[] xPoints = {30d, 30d, 27d, 34d, 26d, 34d, 26d, 33d};
		double[] yPoints = {30d, 30d, 30d, 40d, 40d, 40d, 40d, 30d};
		double[] xPointsFalse = {33d, 33d, 33d, 34d, 34d, 34d, 34d, 33d};

		LineString x = GeometryConverter.createLineString(xPoints);
		LineString y = GeometryConverter.createLineString(yPoints);
		LineString xFalse = GeometryConverter.createLineString(xPointsFalse);

		assertTrue(performComparison(x, y, Operation.RCC8.EC));
		assertFalse(performComparison(xFalse, y, Operation.RCC8.EC));
	}

	@SuppressWarnings("static-method")
	@Test
	public void testPartiallyOverlaps() {
		double[] xPoints = {35d, 35d, 35d, 45d, 45d, 45d, 45d, 35d, 35d, 35d};
		double[] yPoints = {30d, 30d, 30d, 40d, 40d, 40d, 40d, 30d, 30d, 30d};
		double[] xPointsFalse = {33d, 33d, 33d, 34d, 34d, 34d, 34d, 33d, 33d, 33d};

		Polygon x = GeometryConverter.createPolygon(xPoints);
		Polygon y = GeometryConverter.createPolygon(yPoints);
		Polygon xFalse = GeometryConverter.createPolygon(xPointsFalse);

		assertTrue(performComparison(x, y, Operation.RCC8.PO));
		assertFalse(performComparison(xFalse, y, Operation.RCC8.PO));
	}

	@SuppressWarnings("static-method")
	@Test
	public void testTangentialProperPart() {
		double[] xPoints = {30d, 30d, 33d, 34d, 34d, 34d, 34d, 33d, 30d, 30d};
		double[] yPoints = {30d, 30d, 30d, 40d, 40d, 40d, 40d, 30d, 30d, 30d};
		double[] xPointsFalse = {33d, 33d, 33d, 34d, 34d, 34d, 34d, 33d, 33d, 33d};

		Polygon x = GeometryConverter.createPolygon(xPoints);
		Polygon y = GeometryConverter.createPolygon(yPoints);
		Polygon xFalse = GeometryConverter.createPolygon(xPointsFalse);

		assertTrue(performComparison(x, y, Operation.RCC8.TPP));
		assertFalse(performComparison(xFalse, y, Operation.RCC8.TPP));
	}

	@SuppressWarnings("static-method")
	@Test
	public void testNonTangentialProperPart() {
		double[] xPoints = {33d, 33d, 33d, 34d, 34d, 34d, 34d, 33d, 33d, 33d};
		double[] yPoints = {30d, 30d, 30d, 40d, 40d, 40d, 40d, 30d, 30d, 30d};
		double[] xPointsFalse = {30d, 33d, 30d, 34d, 34d, 34d, 34d, 33d, 30d, 33d};

		Polygon x = GeometryConverter.createPolygon(xPoints);
		Polygon y = GeometryConverter.createPolygon(yPoints);
		Polygon xFalse = GeometryConverter.createPolygon(xPointsFalse);

		assertTrue(performComparison(x, y, Operation.RCC8.NTPP));
		assertFalse(performComparison(xFalse, y, Operation.RCC8.NTPP));
	}

	@SuppressWarnings("static-method")
	@Test
	public void testInverseTangentialProperPart() {
		double[] xPoints = {30d, 30d, 33d, 34d, 34d, 34d, 34d, 33d, 30d, 30d};
		double[] yPoints = {30d, 30d, 30d, 40d, 40d, 40d, 40d, 30d, 30d, 30d};
		double[] xPointsFalse = {33d, 33d, 33d, 34d, 34d, 34d, 34d, 33d, 33d, 33d};

		Polygon x = GeometryConverter.createPolygon(xPoints);
		Polygon y = GeometryConverter.createPolygon(yPoints);
		Polygon xFalse = GeometryConverter.createPolygon(xPointsFalse);

		assertTrue(performComparison(y, x, Operation.RCC8.TPPI));
		assertFalse(performComparison(y, xFalse, Operation.RCC8.TPPI));
	}

	@SuppressWarnings("static-method")
	@Test
	public void testInverseNonTangentialProperPart() {
		double[] xPoints = {33d, 33d, 33d, 34d, 34d, 34d, 34d, 33d, 33d, 33d};
		double[] yPoints = {30d, 30d, 30d, 40d, 40d, 40d, 40d, 30d, 30d, 30d};
		double[] xPointsFalse = {30d, 33d, 30d, 34d, 34d, 34d, 34d, 33d, 30d, 33d};

		Polygon x = GeometryConverter.createPolygon(xPoints);
		Polygon y = GeometryConverter.createPolygon(yPoints);
		Polygon xFalse = GeometryConverter.createPolygon(xPointsFalse);

		assertTrue(performComparison(y, x, Operation.RCC8.NTPPI));
		assertFalse(performComparison(y, xFalse, Operation.RCC8.NTPPI));
	}

	@SuppressWarnings("static-method")
	@Test
	public void testProperPart() {
		double[] xPoints = {30d, 30d, 33d, 34d, 34d, 34d, 34d, 33d, 30d, 30d};
		double[] x2Points = {33d, 33d, 33d, 34d, 34d, 34d, 34d, 33d, 33d, 33d};
		double[] yPoints = {30d, 30d, 30d, 40d, 40d, 40d, 40d, 30d, 30d, 30d};
		double[] xPointsFalse = {35d, 35d, 35d, 45d, 45d, 45d, 45d, 35d, 35d, 35d};

		Polygon x = GeometryConverter.createPolygon(xPoints);
		Polygon x2 = GeometryConverter.createPolygon(x2Points);
		Polygon y = GeometryConverter.createPolygon(yPoints);
		Polygon xFalse = GeometryConverter.createPolygon(xPointsFalse);

		assertTrue(performComparison(x, y, Operation.RCC_EXT.PROPER_PART));
		assertTrue(performComparison(x2, y, Operation.RCC_EXT.PROPER_PART));
		assertFalse(performComparison(xFalse, y, Operation.RCC_EXT.PROPER_PART));
	}

	@SuppressWarnings("static-method")
	@Test
	public void testInverseProperPart() {
		double[] xPoints = {30d, 30d, 33d, 34d, 34d, 34d, 34d, 33d, 30d, 30d};
		double[] x2Points = {33d, 33d, 33d, 34d, 34d, 34d, 34d, 33d, 33d, 33d};
		double[] yPoints = {30d, 30d, 30d, 40d, 40d, 40d, 40d, 30d, 30d, 30d};
		double[] xPointsFalse = {35d, 35d, 35d, 45d, 45d, 45d, 45d, 35d, 35d, 35d};

		Polygon x = GeometryConverter.createPolygon(xPoints);
		Polygon x2 = GeometryConverter.createPolygon(x2Points);
		Polygon y = GeometryConverter.createPolygon(yPoints);
		Polygon xFalse = GeometryConverter.createPolygon(xPointsFalse);

		assertTrue(performComparison(y, x, Operation.RCC_EXT.INV_PROPER_PART));
		assertTrue(performComparison(y, x2, Operation.RCC_EXT.INV_PROPER_PART));
		assertFalse(performComparison(y, xFalse, Operation.RCC_EXT.INV_PROPER_PART));
	}

	@SuppressWarnings("static-method")
	@Test
	public void testPart() {
		double[] xPoints = {30d, 30d, 33d, 34d, 34d, 34d, 34d, 33d, 30d, 30d};
		double[] x2Points = {33d, 33d, 33d, 34d, 34d, 34d, 34d, 33d, 33d, 33d};
		double[] x3Points = {35d, 35d, 35d, 45d, 45d, 45d, 45d, 35d, 35d, 35d};
		double[] yPoints = {30d, 30d, 30d, 40d, 40d, 40d, 40d, 30d, 30d, 30d};
		double[] xPointsFalse = {30d, 30d, 27d, 34d, 26d, 34d, 26d, 33d, 30d, 30d};

		Polygon x = GeometryConverter.createPolygon(xPoints);
		Polygon x2 = GeometryConverter.createPolygon(x2Points);
		Polygon x3 = GeometryConverter.createPolygon(x3Points);
		Polygon y = GeometryConverter.createPolygon(yPoints);
		Polygon xFalse = GeometryConverter.createPolygon(xPointsFalse);

		assertTrue(performComparison(x, y, Operation.RCC_EXT.PART));
		assertTrue(performComparison(x2, y, Operation.RCC_EXT.PART));
		assertTrue(performComparison(x3, y, Operation.RCC_EXT.PART));
		assertFalse(performComparison(xFalse, x2, Operation.RCC_EXT.PART));
	}

	@SuppressWarnings("static-method")
	@Test
	public void testInversePart() {
		double[] xPoints = {30d, 30d, 33d, 34d, 34d, 34d, 34d, 33d, 30d, 30d};
		double[] x2Points = {33d, 33d, 33d, 34d, 34d, 34d, 34d, 33d, 33d, 33d};
		double[] x3Points = {35d, 35d, 35d, 45d, 45d, 45d, 45d, 35d, 35d, 35d};
		double[] yPoints = {30d, 30d, 30d, 40d, 40d, 40d, 40d, 30d, 30d, 30d};
		double[] xPointsFalse = {29d, 29d, 27d, 34d, 26d, 34d, 26d, 33d, 29d, 29d};

		Polygon x = GeometryConverter.createPolygon(xPoints);
		Polygon x2 = GeometryConverter.createPolygon(x2Points);
		Polygon x3 = GeometryConverter.createPolygon(x3Points);
		Polygon y = GeometryConverter.createPolygon(yPoints);
		Polygon xFalse = GeometryConverter.createPolygon(xPointsFalse);

		assertTrue(performComparison(y, x, Operation.RCC_EXT.INV_PART));
		assertTrue(performComparison(y, x2, Operation.RCC_EXT.INV_PART));
		assertTrue(performComparison(y, x3, Operation.RCC_EXT.INV_PART));
		assertFalse(performComparison(y, xFalse, Operation.RCC_EXT.INV_PART));
	}

	@SuppressWarnings("static-method")
	@Test
	public void testIntersects() {
		Operation op = Operation.SimpleFeatures.INTERSECTS;
		double[] xPoints = {35d, 35d, 35d, 45d, 45d, 45d, 45d, 35d, 35d, 35d};
		double[] yPoints = {30d, 30d, 30d, 40d, 40d, 40d, 40d, 30d, 30d, 30d};
		double[] zPoints = {20d, 20d, 20d, 25d, 25d, 25d, 25d, 20d, 20d, 20d};

		Polygon x = GeometryConverter.createPolygon(xPoints);
		Polygon y = GeometryConverter.createPolygon(yPoints);
		Polygon z = GeometryConverter.createPolygon(zPoints);

		assertTrue(performComparison(x, y, op));
		assertFalse(performComparison(x, z, op));
	}

	@SuppressWarnings("static-method")
	@Test
	public void testEquals() {
		Operation op = Operation.SimpleFeatures.EQUALS;
		double[] xPoints = {35d, 35d, 35d, 45d, 45d, 45d, 45d, 35d, 35d, 35d};
		double[] x1Points = {35d, 35d, 45d, 35d, 45d, 45d, 35d, 45d, 35d, 35d};
		double[] yPoints = {30d, 30d, 30d, 40d, 40d, 40d, 40d, 30d, 30d, 30d};

		Polygon x = GeometryConverter.createPolygon(xPoints);
		Polygon y = GeometryConverter.createPolygon(yPoints);
		Polygon x1 = GeometryConverter.createPolygon(x1Points);

		assertTrue(performComparison(x, x, op));
		assertFalse(performComparison(x, y, op));
		assertTrue(performComparison(x, x1, op));

	}

	@SuppressWarnings("static-method")
	@Test
	public void testDisjoint() {
		Operation op = Operation.SimpleFeatures.DISJOINT;
		double[] xPoints = {35d, 35d, 35d, 45d, 45d, 45d, 45d, 35d, 35d, 35d};
		double[] yPoints = {30d, 30d, 30d, 40d, 40d, 40d, 40d, 30d, 30d, 30d};
		double[] zPoints = {20d, 20d, 20d, 25d, 25d, 25d, 25d, 20d, 20d, 20d};

		Polygon x = GeometryConverter.createPolygon(xPoints);
		Polygon y = GeometryConverter.createPolygon(yPoints);
		Polygon z = GeometryConverter.createPolygon(zPoints);

		assertFalse(performComparison(x, y, op));
		assertTrue(performComparison(x, z, op));
	}

	@SuppressWarnings("static-method")
	@Test
	public void testCovers() {
		Operation op = Operation.Egenhofer.COVERS;
		double[] xPoints = {35d, 35d, 35d, 45d, 45d, 45d, 45d, 35d, 35d, 35d};
		double[] yPoints = {30d, 30d, 30d, 40d, 40d, 40d, 40d, 30d, 30d, 30d};
		double[] zPoints = {20d, 20d, 20d, 50d, 50d, 50d, 50d, 20d, 20d, 20d};

		Polygon x = GeometryConverter.createPolygon(xPoints);
		Polygon y = GeometryConverter.createPolygon(yPoints);
		Polygon z = GeometryConverter.createPolygon(zPoints);

		assertFalse(performComparison(x, y, op));
		assertFalse(performComparison(x, z, op));
		assertTrue(performComparison(z, x, op));
	}

	@SuppressWarnings("static-method")
	@Test
	public void testCoveredBy() {
		Operation op = Operation.Egenhofer.COVEREDBY;
		double[] xPoints = {35d, 35d, 35d, 45d, 45d, 45d, 45d, 35d, 35d, 35d};
		double[] yPoints = {30d, 30d, 30d, 40d, 40d, 40d, 40d, 30d, 30d, 30d};
		double[] zPoints = {20d, 20d, 20d, 50d, 50d, 50d, 50d, 20d, 20d, 20d};

		Polygon x = GeometryConverter.createPolygon(xPoints);
		Polygon y = GeometryConverter.createPolygon(yPoints);
		Polygon z = GeometryConverter.createPolygon(zPoints);

		assertFalse(performComparison(x, y, op));
		assertTrue(performComparison(x, z, op));
		assertFalse(performComparison(z, x, op));
	}

	@SuppressWarnings("static-method")
	@Test
	public void testCrosses() {
		Operation op = Operation.SimpleFeatures.CROSSES;
		double[] xPoints = {35d, 35d, 35d, 45d, 45d, 45d, 45d, 35d, 35d, 35d};
		double[] yPoints = {30d, 30d, 30d, 40d, 40d, 40d, 40d, 30d, 30d, 30d};
		double[] zPoints = {20d, 20d, 50, 50};

		Polygon x = GeometryConverter.createPolygon(xPoints);
		Polygon y = GeometryConverter.createPolygon(yPoints);

		LineString z = GeometryConverter.createLineString(zPoints);
		assertFalse(performComparison(x, y, op));
		assertFalse(performComparison(y, x, op));
		assertTrue(performComparison(z, x, op));
		assertTrue(performComparison(x, z, op));
	}

	@SuppressWarnings("static-method")
	@Test
	public void testOverlaps() {
		Operation op = Operation.SimpleFeatures.OVERLAPS;

		double[] xPoints = {35d, 35d, 35d, 45d, 45d, 45d, 45d, 35d, 35d, 35d};
		double[] x1Points = {38d, 38d, 38d, 46d, 46d, 46d, 46d, 38d, 38d, 38d};
		double[] yPoints = {30d, 30d, 30d, 40d, 40d, 40d, 40d, 30d, 30d, 30d};
		double[] zPoints = {20d, 20d, 20d, 50d, 50d, 50d, 50d, 20d, 20d, 20d};

		Polygon x = GeometryConverter.createPolygon(xPoints);
		Polygon x1 = GeometryConverter.createPolygon(x1Points);
		Polygon y = GeometryConverter.createPolygon(yPoints);
		Polygon z = GeometryConverter.createPolygon(zPoints);

		assertTrue(performComparison(x, y, op));
		assertFalse(performComparison(x, z, op));
		assertFalse(performComparison(z, x, op));

		assertTrue(performComparison(x1, x, op));
		assertFalse(performComparison(x, x, op));
	}

	@SuppressWarnings("static-method")
	@Test
	public void testTouches() {
		Operation op = Operation.SimpleFeatures.TOUCHES;

		double[] xPoints = {35d, 35d, 35d, 45d, 45d, 45d, 45d, 35d, 35d, 35d};
		double[] x1Points = {35d, 35d, 35d, 30d, 30d, 30d, 30d, 35d, 35d, 35d};
		double[] yPoints = {30d, 30d, 30d, 40d, 40d, 40d, 40d, 30d, 30d, 30d};
		double[] zPoints = {20d, 20d, 20d, 50d, 50d, 50d, 50d, 20d, 20d, 20d};

		Polygon x = GeometryConverter.createPolygon(xPoints);
		Polygon x1 = GeometryConverter.createPolygon(x1Points);
		Polygon y = GeometryConverter.createPolygon(yPoints);
		Polygon z = GeometryConverter.createPolygon(zPoints);

		assertFalse(performComparison(x, y, op));
		assertFalse(performComparison(x, z, op));
		assertFalse(performComparison(z, x, op));

		assertTrue(performComparison(x1, x, op));
		assertFalse(performComparison(x, x, op));
	}

	@SuppressWarnings("static-method")
	@Test
	public void testWithin() {
		Operation op = Operation.SimpleFeatures.WITHIN;
		double[] xPoints = {35d, 35d, 35d, 45d, 45d, 45d, 45d, 35d, 35d, 35d};
		double[] x1Points = {38d, 38d, 38d, 46d, 46d, 46d, 46d, 38d, 38d, 38d};
		double[] yPoints = {30d, 30d, 30d, 40d, 40d, 40d, 40d, 30d, 30d, 30d};
		double[] zPoints = {20d, 20d, 20d, 50d, 50d, 50d, 50d, 20d, 20d, 20d};

		Polygon x = GeometryConverter.createPolygon(xPoints);
		Polygon x1 = GeometryConverter.createPolygon(x1Points);
		Polygon y = GeometryConverter.createPolygon(yPoints);
		Polygon z = GeometryConverter.createPolygon(zPoints);

		assertFalse(performComparison(x, y, op));
		assertFalse(performComparison(y, x, op));
		assertTrue(performComparison(x, z, op));
		assertFalse(performComparison(z, x, op));

		assertFalse(performComparison(x1, x, op));
		assertTrue(performComparison(x, x, op));
	}
}
