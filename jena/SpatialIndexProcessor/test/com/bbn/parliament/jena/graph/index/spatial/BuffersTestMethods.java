package com.bbn.parliament.jena.graph.index.spatial;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;

import com.bbn.parliament.jena.joseki.client.CloseableQueryExec;

public class BuffersTestMethods extends SpatialTestDataset {
	public BuffersTestMethods(Properties factoryProperties) {
		super(factoryProperties);
	}

	@SuppressWarnings("unused")
	private static final String OLD_POINT_QUERY = ""
		+ "SELECT ?a ?where WHERE {\n"
		+ "?a a example:Building .\n"
		+ "OPTIONAL {\n"
		+ "?a georss:where ?where .\n"
		+ "?buffer a spatial:Buffer .\n"
		+ "?buffer spatial:distance \"278\"^^xsd:double .\n"
		+ "?buffer spatial:extent [\n"
		+ "   a gml:Point ;\n"
		+ "   gml:pos \"2.5 0\"\n"
		+ "] .\n"
		+ "?where <http://example.org/covers> ?buffer .\n"
		+ "} }";
	private static final String POINT_QUERY = ""
		+ "SELECT DISTINCT ?a WHERE {\n"
		+ "?a a example:Building ;\n"
		+ "georss:where ?where .\n"
		//+ "   <http://example.org/covers> [\n"
		//+ " ogc:coveredBy [\n"
		+ "?buffer  a spatial:Buffer ;\n"
		+ "  spatial:distance \"280\"^^xsd:double;\n"
		+ "  spatial:extent [\n"
		+ "    a gml:Point ;\n"
		+ "    gml:pos \"2.5 0\"\n"
		+ "  ] .\n"
		+ "?buffer ogc:covers ?where .\n"
		//+ "]\n"
		//+ "].\n"
		+ "}";

	public void testBufferPoint() {
		loadData("queries/BuildingExample1.ttl");
		loadData("queries/BuildingExample2.ttl");

		assertTrue(getIndex().size() > 0);
		//try (CloseableQueryExec qexec = performQuery(OLD_POINT_QUERY)) {
		try (CloseableQueryExec qexec = performQuery(POINT_QUERY)) {
			checkResults(qexec, "example1:building1", "example1:building2");
		}
	}

	private static final String POINT_REVERSE_QUERY = ""
		+ "SELECT DISTINCT ?a WHERE {\n"
		+ "?a a example:Building ;\n"
		+ "  georss:where ?where .\n"
		+ "?buffer  a spatial:Buffer ;\n"
		+ "  spatial:distance \"280\"^^xsd:double;\n"
		+ "  spatial:extent [\n"
		+ "    a gml:Point ;\n"
		+ "    gml:pos \"2.5 0\";\n"
		+ " ] .\n"
		+ "?buffer ogc:covers ?where .\n"
		+ "}";

	public void testBufferPointReverse() {
		loadData("queries/BuildingExample1.ttl");
		loadData("queries/BuildingExample2.ttl");

		try (CloseableQueryExec qexec = performQuery(POINT_REVERSE_QUERY)) {
			checkResults(qexec, "example1:building1", "example1:building2");
		}
	}

	private static final String REGION_QUERY = ""
		+ "SELECT DISTINCT ?a WHERE {\n"
		+ "?a a example:SpatialThing ;\n"
		+ "georss:where ?where .\n"
		+ "	?buffer	a spatial:Buffer ;\n"
		+ "		spatial:distance \"0.09\"^^xsd:double;\n"
		+ "		spatial:extent [\n"
		+ "			a gml:Polygon ;\n"
		+ "			gml:exterior [\n"
		+ "				a gml:LinearRing ;\n"
		+ "				gml:posList \"34.90 36.0 34.845 36.0 34.845 35.8 34.9 35.8 34.9 36.0\"\n"
		+ "			]\n"
		+ "		] .\n"
		+ "  ?where rcc:part ?buffer .\n"
		+ "}";

	public void testBufferRegion() {
		loadData("queries/BuildingExample1.ttl");
		loadData("queries/BuildingExample2.ttl");
		loadData("queries/BuildingExample3.ttl");

		try (CloseableQueryExec qexec = performQuery(REGION_QUERY)) {
			checkResults(qexec, "example2:building3", "example2:building4", "example2:building5", "example3:campus2");
		}
	}

	private static final String VARIABLE_DISTANCE_QUERY = ""
		+ "SELECT DISTINCT ?a WHERE {\n"
		+ "  ?x a example:Car ;\n"
		+ "    georss:where ?carloc ;\n"
		+ "    example:range ?distance .\n"
		+ "  ?buffer a spatial:Buffer ;\n"
		+ "    spatial:distance ?distance ;\n"
		+ "    spatial:extent ?carloc .\n"
		+ "  ?a a example:Building ;\n"
		+ "    georss:where [\n"
		+ "      rcc:part ?buffer\n"
		+ "    ] .\n"
		+ "}";

	public void testBufferVariableDistance() {
		loadData("queries/BuildingExample1.ttl");
		loadData("queries/BuildingExample2.ttl");
		loadData("queries/CarExample.ttl");

		try (CloseableQueryExec qexec = performQuery(VARIABLE_DISTANCE_QUERY)) {
			checkResults(qexec, "example2:building3", "example2:building4", "example2:building5");
		}
	}

	private static final String BOUND_VARIABLE_EXTENT_QUERY = ""
		+ "SELECT DISTINCT ?a WHERE {\n"
		+ "  ?extent a gml:Point ;\n"
		+ "    gml:pos \"34 36\" .\n"
		+ "  ?buffer a spatial:Buffer ;\n"
		+ "    spatial:distance 500 ;\n"
		+ "    spatial:extent ?extent .\n"
		+ "  ?a a example:Building ;\n"
		+ "    georss:where ?point ;\n"
		+ "    georss:where [\n"
		+ "      rcc:part ?buffer\n"
		+ "    ] .\n"
		+ "}";

	public void testBufferBoundVariableExtent() {
		loadData("queries/BuildingExample1.ttl");
		loadData("queries/BuildingExample2.ttl");

		try (CloseableQueryExec qexec = performQuery(BOUND_VARIABLE_EXTENT_QUERY)) {
			checkResults(qexec, "example2:building3", "example2:building4", "example2:building5");
		}
	}

	private static final String ZERO_DISTANCE_QUERY = ""
		+ "SELECT ?a WHERE {\n"
		+ "  ?buffer a spatial:Buffer ;\n"
		+ "    spatial:distance \"0.0\"^^xsd:double;\n"
		+ "    spatial:extent [\n"
		+ "      a gml:Polygon ;\n"
		+ "      gml:exterior [\n"
		+ "        a gml:LinearRing ;\n"
		+ "        gml:posList \"39.0 125.0 39.0 126.0 40.0 126.0 40.0 125.0\"\n"
		+ "      ]\n"
		+ "    ] .\n"
		+ "  ?a a example:SpatialThing ;\n"
		+ "    georss:where ?point ;\n"
		+ "    georss:where [\n"
		+ "      rcc:part ?buffer\n"
		+ "    ] .\n"
		+ "}";

	public void testBufferZeroDistance() {
		loadData("queries/BuildingExample2.ttl");

		try (CloseableQueryExec qexec = performQuery(ZERO_DISTANCE_QUERY)) {
			assertFalse(qexec.execSelect().hasNext());
		}
	}

	private static final String THOUSAND_DISTANCE_QUERY = ""
		+ "SELECT DISTINCT ?a WHERE {\n"
		+ "  ?buffer a spatial:Buffer ;\n"
		+ "    spatial:distance \"%1$d.0\"^^xsd:double;\n"
		+ "    spatial:extent cities:%2$s .\n"
		+ "  ?a a example:SpatialThing ;\n"
		+ "    georss:where ?point ;\n"
		+ "    georss:where [\n"
		+ "      rcc:part ?buffer\n"
		+ "    ] .\n"
		+ "}";

	public void testBufferThousandDistance1() {
		thousandDistanceTestHelper(5914, "pointLondon", "cities:london", "cities:paris",
			"cities:ottawa", "cities:newyork", "cities:greaterlondon", "cities:washdc");
	}

	public void testBufferThousandDistance2() {
		thousandDistanceTestHelper(5585, "polyLondon", "cities:london", "cities:paris",
			"cities:ottawa", "cities:newyork", "cities:greaterlondon");
	}

	public void testBufferThousandDistance3() {
		thousandDistanceTestHelper(1004, "pointWashDC", "cities:ottawa", "cities:newyork",
			"cities:washdc");
	}

	private void thousandDistanceTestHelper(int distance, String city, String... expectedResults) {
		loadData("queries/Cities.ttl");
		String query = String.format(THOUSAND_DISTANCE_QUERY, distance, city);
		try (CloseableQueryExec qexec = performQuery(query)) {
			checkResults(qexec, expectedResults);
		}
	}
}
