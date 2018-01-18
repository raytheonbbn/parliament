// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
package com.bbn.parliament.jena.graph.index.spatial;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.hp.hpl.jena.query.ResultSet;

public abstract class AbstractBuffersTest extends AbstractSpatialTest {
	@Test
	public void testBufferPoint() {
		loadData("queries/BuildingExample1.ttl");
		loadData("queries/BuildingExample2.ttl");

		assertTrue(index.size() > 0);
		//int counter = DebugConnection.COUNTER.get();
		//ResultSet rs = performQuery(""
		//	+ PREFIXES
		//	+ "SELECT ?a ?where WHERE { "
		//	+ "?a a example:Building . "
		//	+ "OPTIONAL {"
		//	+ "?a georss:where ?where . "
		//	+ "?buffer a spatial:Buffer . "
		//	+ "?buffer spatial:distance \"278\"^^xsd:double . "
		//	+ "?buffer spatial:extent ["
		//	+ "   a gml:Point ; "
		//	+ "   gml:pos \"2.5 0\" "
		//	+ "] . "
		//	+ "?where <http://example.org/covers> ?buffer ."
		//	+ "} }");
		ResultSet rs = performQuery(""
			+ PREFIXES
			+ "SELECT DISTINCT ?a WHERE { "
			+ "?a a example:Building ; "
			+ "georss:where ?where ."
			//	+ "   <http://example.org/covers> ["
			//	+ " ogc:coveredBy [ "
			+ "?buffer  a spatial:Buffer ;"
			+ "  spatial:distance \"280\"^^xsd:double; "
			+ "  spatial:extent ["
			+ "    a gml:Point ; "
			+ "    gml:pos \"2.5 0\" "
			+ "  ] . "
			+ "?buffer ogc:covers ?where ."
			//	+ "]"
			//	+ "]."
			+ "}");

		checkResults(rs, new String[] { EXAMPLE1_NS + "building1", EXAMPLE1_NS + "building2" });
		//int newCounter = DebugConnection.COUNTER.get();
		//LOG.info(String.format("Used: %d connections", (newCounter - counter)));
	}

	@Test
	public void testBufferPointReverse() {
		loadData("queries/BuildingExample1.ttl");
		loadData("queries/BuildingExample2.ttl");
		//int counter = DebugConnection.COUNTER.get();
		ResultSet rs = performQuery(""
			+ PREFIXES
			+ "SELECT DISTINCT ?a WHERE { "
			+ "?a a example:Building ; "
			+ "  georss:where ?where . "

				+ "?buffer  a spatial:Buffer ;"
				+ "  spatial:distance \"280\"^^xsd:double; "
				+ "  spatial:extent ["
				+ "    a gml:Point ; "
				+ "    gml:pos \"2.5 0\"; "
				+ " ] . "

				+ "?buffer ogc:covers ?where ."
				+ "}");

		checkResults(rs, EXAMPLE1_NS + "building1", EXAMPLE1_NS + "building2");
		//int newCounter = DebugConnection.COUNTER.get();
		//LOG.info(String.format("Used: %d connections", (newCounter - counter)));
	}

	@Test
	public void testBufferRegion() {
		loadData("queries/BuildingExample1.ttl");
		loadData("queries/BuildingExample2.ttl");
		loadData("queries/BuildingExample3.ttl");
		//int counter = DebugConnection.COUNTER.get();
		ResultSet rs = performQuery(""
			+ PREFIXES
			+ "SELECT DISTINCT ?a WHERE { "
			+ "?a a example:SpatialThing ; "
			+ "georss:where ?where . "

				+ "	?buffer	a spatial:Buffer ;"
				+ "		spatial:distance \"0.09\"^^xsd:double; "
				+ "		spatial:extent ["
				+ "			a gml:Polygon ;"
				+ "			gml:exterior ["
				+ "				a gml:LinearRing ;"
				+ "				gml:posList \"34.90 36.0 34.845 36.0 34.845 35.8 34.9 35.8 34.9 36.0\""
				+ "			]"
				+ "		] ."
				+ "  ?where rcc:part ?buffer . "
				+ "}");

		checkResults(rs, EXAMPLE2_NS + "building3", EXAMPLE2_NS + "building4", EXAMPLE2_NS + "building5", EXAMPLE3_NS + "campus2");
		//int newCounter = DebugConnection.COUNTER.get();
		//LOG.info(String.format("Used: %d connections", (newCounter - counter)));
	}

	@Test
	public void testBufferVariableDistance() {
		loadData("queries/BuildingExample1.ttl");
		loadData("queries/BuildingExample2.ttl");
		loadData("queries/CarExample.ttl");

		//int counter = DebugConnection.COUNTER.get();
		ResultSet rs = performQuery(""
			+ PREFIXES
			+ "SELECT DISTINCT ?a "
			+ "WHERE { "
			+ "  ?x a example:Car ; "
			+ "    georss:where ?carloc ; "
			+ "    example:range ?distance . "

			+ "  ?buffer a spatial:Buffer ;"
			+ "    spatial:distance ?distance ;"
			+ "    spatial:extent ?carloc . "

			+ "  ?a a example:Building ; "
			+ "    georss:where ["
			+ "      rcc:part ?buffer "
			+ "    ] . "
			+ "}");

		checkResults(rs, EXAMPLE2_NS + "building3", EXAMPLE2_NS + "building4", EXAMPLE2_NS + "building5");
		//int newCounter = DebugConnection.COUNTER.get();
		//LOG.info(String.format("Used: %d connections", (newCounter - counter)));
	}

	@Test
	public void testBufferBoundVariableExtent() {
		loadData("queries/BuildingExample1.ttl");
		loadData("queries/BuildingExample2.ttl");

		//int counter = DebugConnection.COUNTER.get();
		ResultSet rs = performQuery(""
			+ PREFIXES
			+ "SELECT DISTINCT ?a "
			+ "WHERE {"
			+ "  ?extent a gml:Point ; "
			+ "    gml:pos \"34 36\" . "

			+ "  ?buffer a spatial:Buffer ;"
			+ "    spatial:distance 500 ;"
			+ "    spatial:extent ?extent ."

			+ "  ?a a example:Building ;"
			+ "    georss:where ?point ;"
			+ "    georss:where ["
			+ "      rcc:part ?buffer "
			+ "    ] ."

			+ "}");

		checkResults(rs, EXAMPLE2_NS + "building3", EXAMPLE2_NS + "building4", EXAMPLE2_NS + "building5");
		//int newCounter = DebugConnection.COUNTER.get();
		//LOG.info(String.format("Used: %d connections", (newCounter - counter)));
	}

	@Test
	public void testBufferZeroDistance() {
		loadData("queries/BuildingExample2.ttl");
		//int counter = DebugConnection.COUNTER.get();
		ResultSet rs = performQuery(""
			+ PREFIXES
			+ "SELECT ?a "
			+ "WHERE { "
			+ "  ?buffer a spatial:Buffer ;"
			+ "    spatial:distance \"0.0\"^^xsd:double;"
			+ "    spatial:extent ["
			+ "      a gml:Polygon ;"
			+ "      gml:exterior ["
			+ "        a gml:LinearRing ;"
			+ "        gml:posList \"39.0 125.0 39.0 126.0 40.0 126.0 40.0 125.0\""
			+ "      ]"
			+ "    ] ."
			+ "  ?a a example:SpatialThing ; "
			+ "    georss:where ?point ; "
			+ "    georss:where ["
			+ "      rcc:part ?buffer "
			+ "    ] ."
			+ "}");
		assertFalse(rs.hasNext());
		//int newCounter = DebugConnection.COUNTER.get();
		//LOG.info(String.format("Used: %d connections", (newCounter - counter)));
	}

	@Test
	public void testBufferThousandDistance() {
		loadData("queries/cities.ttl");

		ResultSet rs = performQuery(""
			+ PREFIXES
			+ "SELECT DISTINCT ?a "
			+ "WHERE { "
			+ "  ?buffer a spatial:Buffer ;"
			+ "    spatial:distance \"5914.0\"^^xsd:double;"
			+ "    spatial:extent cities:pointLondon ."
			+ "  ?a a example:SpatialThing ; "
			+ "    georss:where ?point ; "
			+ "    georss:where ["
			+ "      rcc:part ?buffer"
			+ "    ] ."
			+ "}");
		checkResults(rs, EXAMPLE_CITIES_NS + "london", EXAMPLE_CITIES_NS + "paris", EXAMPLE_CITIES_NS + "ottowa", EXAMPLE_CITIES_NS + "newyork", EXAMPLE_CITIES_NS + "greaterlondon", EXAMPLE_CITIES_NS + "washdc");

		rs = performQuery(""
			+ PREFIXES
			+ "SELECT DISTINCT ?a "
			+ "WHERE { "
			+ "  ?buffer a spatial:Buffer ;"
			+ "    spatial:distance \"5585.0\"^^xsd:double;"
			+ "    spatial:extent cities:polyLondon . "
			+ "  ?a a example:SpatialThing ; "
			+ "    georss:where ?point ; "
			+ "    georss:where ["
			+ "      rcc:part ?buffer"
			+ "    ] ."
			+ "}");
		checkResults(rs, EXAMPLE_CITIES_NS + "london", EXAMPLE_CITIES_NS + "paris", EXAMPLE_CITIES_NS + "ottowa", EXAMPLE_CITIES_NS + "newyork", EXAMPLE_CITIES_NS + "greaterlondon");

		rs = performQuery(""
			+ PREFIXES
			+ "SELECT DISTINCT ?a "
			+ "WHERE { "
			+ "  ?buffer a spatial:Buffer ;"
			+ "    spatial:distance \"1004.0\"^^xsd:double;"
			+ "    spatial:extent cities:pointWashDC ."
			+ "  ?a a example:SpatialThing ; "
			+ "    georss:where ?point ; "
			+ "    georss:where ["
			+ "      rcc:part ?buffer "
			+ "    ] ."
			+ "}");
		checkResults(rs, EXAMPLE_CITIES_NS + "ottowa", EXAMPLE_CITIES_NS + "newyork", EXAMPLE_CITIES_NS + "washdc");
	}
}
