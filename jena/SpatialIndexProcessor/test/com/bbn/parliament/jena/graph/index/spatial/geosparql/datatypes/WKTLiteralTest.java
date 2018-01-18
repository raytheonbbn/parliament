package com.bbn.parliament.jena.graph.index.spatial.geosparql.datatypes;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.hp.hpl.jena.datatypes.DatatypeFormatException;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class WKTLiteralTest implements LiteralTestCase {

	private WKTLiteral literal;

	@Before
	public void setup() {
		literal = new WKTLiteral();
	}

	@Test
	@Override
	public void testValidCRS() {
		String wkt; // WKT to parse
		Geometry g; // parsed location
		Geometry location; // location to check
		double distance; // distance between converted point and known location
		double threshold = 1E-5; // threshold for distance difference

		// CN Tower
		location = literal.parse("POINT(-79.387139 43.642567)");

		// UTM Zone 17
		wkt = "<http://www.opengis.net/def/crs/EPSG/0/32617> POINT (630084 4833438)";
		try {
			g = literal.parse(wkt);
			Assert.assertTrue(g instanceof Point);
		} catch (DatatypeFormatException e) {
			Assert.fail(String.format("%s is valid WKT", wkt));
			e.printStackTrace();
			return;
		}

		distance = location.distance(g);
		Assert.assertTrue(String.format("%f >= %f", distance, threshold),
				distance < threshold);

		// WGS84
		wkt = "<http://www.opengis.net/def/crs/EPSG/0/4326> POINT (43.642567 -79.387139)";
		try {
			g = literal.parse(wkt);
			Assert.assertTrue(g instanceof Point);
		} catch (DatatypeFormatException e) {
			Assert.fail(String.format("%s is valid WKT", wkt));
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
		String wkt = "<http://example.org/CRS> POINT(1 1)";
		boolean failed = false;
		try {
			literal.parse(wkt);
		} catch (DatatypeFormatException e) {
			failed = true;
		}
		Assert.assertTrue("Invalid CRS should through a DatatypeFormatException", failed);
	}

	@Test
	@Override
	public void testInvalidFormat() {
		String wkt = "PONT(-77.036667 38.895111)";
		boolean failed = false;
		try {
			literal.parse(wkt);
		} catch (DatatypeFormatException e) {
			failed = true;
		}
		Assert.assertTrue("Invalid WKT should through a DatatypeFormatException", failed);

	}

	@Test
	@Override
	public void testDefaultCRS() {
		// default CRS is <http://www.opengis.net/def/crs/OGC/1.3/CRS84> which
		// specifies lon, lat ordering (X, Y)
		String wkt = "POINT(-77.036667 38.895111)";
		Geometry g;
		try {
			g = literal.parse(wkt);
		} catch (DatatypeFormatException e) {
			Assert.fail(String.format("%s is valid WKT", wkt));
			e.printStackTrace();
			return;
		}

		Assert.assertTrue(g instanceof Point);

		Point p = (Point) g;
		Assert.assertEquals(38.895111, p.getY(), 0.0000001);
		Assert.assertEquals(-77.036667, p.getX(), 0.0000001);

		Assert.assertEquals("<http://www.opengis.net/def/crs/OGC/1.3/CRS84> POINT (-77.036667 38.895111)",
				literal.unparse(g));
	}

	@Test
	@Override
	public void testParseUnparse() {
		// default CRS is <http://www.opengis.net/def/crs/OGC/1.3/CRS84> which
		// specifies lon, lat ordering (X, Y)
		String wkt = "POINT(-77.036667 38.895111)";
		Geometry g;
		try {
			g = literal.parse(wkt);
		} catch (DatatypeFormatException e) {
			Assert.fail(String.format("%s is valid WKT", wkt));
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
			Assert.fail(String.format("%s is valid WKT", wkt));
			e.printStackTrace();
			return;
		}
		Assert.assertTrue(g.equals(g1));

	}
}
