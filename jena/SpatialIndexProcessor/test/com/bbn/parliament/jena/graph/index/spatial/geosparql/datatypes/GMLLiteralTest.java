package com.bbn.parliament.jena.graph.index.spatial.geosparql.datatypes;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.hp.hpl.jena.datatypes.DatatypeFormatException;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class GMLLiteralTest implements LiteralTestCase {

	private GMLLiteral literal;

	@Before
	public void setup() {
		literal = new GMLLiteral();
	}

	@Test
	@Override
	public void testValidCRS() {
		String gml; // WKT to parse
		Geometry g; // parsed location
		Geometry location; // location to check
		double distance; // distance between converted point and known location
		double threshold = 1E-5; // threshold for distance difference

		// CN Tower
		// location = literal.parse("POINT(-79.387139 43.642567)");
		gml = "<gml:Point xmlns:gml=\"http://www.opengis.net/gml\"><gml:pos>-79.387139 43.642567</gml:pos></gml:Point>";
		location = literal.parse(gml);
		// UTM Zone 17
		gml = "<gml:Point srsName=\"urn:x-ogc:def:crs:EPSG:32617\" xmlns:gml=\"http://www.opengis.net/gml\"><gml:pos >630084 4833438</gml:pos></gml:Point>";
		try {
			g = literal.parse(gml);
			Assert.assertTrue(g instanceof Point);
		} catch (DatatypeFormatException e) {
			Assert.fail(String.format("%s is valid GML", gml));
			e.printStackTrace();
			return;
		}

		distance = location.distance(g);
		Assert.assertTrue(String.format("%f >= %f", distance, threshold),
				distance < threshold);

		// WGS84
		gml = "<gml:Point srsName=\"urn:x-ogc:def:crs:EPSG:4326\" xmlns:gml=\"http://www.opengis.net/gml\"><gml:pos >43.642567 -79.387139</gml:pos></gml:Point>";
		try {
			g = literal.parse(gml);
			Assert.assertTrue(g instanceof Point);
		} catch (DatatypeFormatException e) {
			Assert.fail(String.format("%s is valid WKT", gml));
			e.printStackTrace();
			return;
		}

		distance = location.distance(g);
		Assert.assertTrue(String.format("%f >= %f", distance, threshold),
				distance < threshold);
	}

	@Test
	@Override
	public void testInvalidCRS() {
		String gml = "<gml:Point srsName=\"urn:x-ogc:def:crs:EPSG:-1\" xmlns:gml=\"http://www.opengis.net/gml\"><gml:pos >-77.036667 38.895111</gml:pos></gml:Point>";
		boolean failed = false;
		try {
			literal.parse(gml);
		} catch (DatatypeFormatException e) {
			failed = true;
		}
		Assert.assertTrue("Invalid CRS should through a DatatypeFormatException", failed);
	}

	@Test
	@Override
	public void testInvalidFormat() {
		String gml = "<gml:Point xmlns:gml=\"http://www.opengis.net/gml\">-77.036667 38.895111</gml:Point>";
		boolean failed = false;
		try {
			literal.parse(gml);
		} catch (DatatypeFormatException e) {
			failed = true;
		}
		Assert.assertTrue("Invalid GML should through a DatatypeFormatException", failed);

	}

	@Test
	@Override
	public void testDefaultCRS() {
		// default CRS is <http://www.opengis.net/def/crs/OGC/1.3/CRS84> which
		// specifies lon, lat ordering (X, Y)
		String gml = "<gml:Point xmlns:gml=\"http://www.opengis.net/gml\"><gml:pos >-77.036667 38.895111</gml:pos></gml:Point>";

		Geometry g;
		try {
			g = literal.parse(gml);
		} catch (DatatypeFormatException e) {
			Assert.fail(String.format("%s is valid GML", gml));
			e.printStackTrace();
			return;
		}

		Assert.assertTrue(g instanceof Point);

		Point p = (Point) g;
		Assert.assertEquals(38.895111, p.getY(), 0.0000001);
		Assert.assertEquals(-77.036667, p.getX(), 0.0000001);

		//      assertEquals(gml, literal.unparse(g));
	}

	@Override
	public void testParseUnparse() {
		String gml = "<gml:Point xmlns:gml=\"http://www.opengis.net/gml\"><gml:pos >-77.036667 38.895111</gml:pos></gml:Point>";

		Geometry g;
		try {
			g = literal.parse(gml);
		} catch (DatatypeFormatException e) {
			Assert.fail(String.format("%s is valid GML", gml));
			e.printStackTrace();
			return;
		}

		Assert.assertTrue(g instanceof Point);

		Point p = (Point) g;
		Assert.assertEquals(38.895111, p.getY(), 0.0000001);
		Assert.assertEquals(-77.036667, p.getX(), 0.0000001);

		Geometry g1;
		String unparse = literal.unparse(g);
		try {
			g1 = literal.parse(unparse);
		} catch (DatatypeFormatException e) {
			Assert.fail(String.format("%s is valid GML", unparse));
			e.printStackTrace();
			return;
		}
		Assert.assertTrue(g.equals(g1));

	}
}
