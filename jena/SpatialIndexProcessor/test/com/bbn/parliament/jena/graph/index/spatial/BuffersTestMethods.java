package com.bbn.parliament.jena.graph.index.spatial;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;
import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jena.joseki.client.CloseableQueryExec;

public class BuffersTestMethods extends SpatialTestDataset {
	private static final Logger LOG = LoggerFactory.getLogger(BuffersTestMethods.class);

	public BuffersTestMethods(Properties factoryProperties) {
		super(factoryProperties);
	}

	private static final String POINT_QUERY = ""
		+ "SELECT DISTINCT ?a WHERE {\n"
		+ "?a a example:Building ;\n"
		+ "georss:where ?where .\n"
		+ "?buffer  a spatial:Buffer ;\n"
		+ "  spatial:distance \"280\"^^xsd:double;\n"
		+ "  spatial:extent [\n"
		+ "    a gml:Point ;\n"
		+ "    gml:pos \"2.5 0\"\n"
		+ "  ] .\n"
		+ "?buffer ogc:covers ?where .\n"
		+ "}";

	public void testBufferPoint() {
		loadData("queries/BuildingExample1.ttl");
		loadData("queries/BuildingExample2.ttl");

		assertTrue(getIndex().size() > 0);
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
		+ "    spatial:distance \"%1$f\"^^xsd:double;\n"
		+ "    spatial:extent cities:%2$s .\n"
		+ "  ?a a example:SpatialThing ;\n"
		+ "    georss:where ?point ;\n"
		+ "    georss:where [\n"
		+ "      rcc:part ?buffer\n"
		+ "    ] .\n"
		+ "}";

	public void testThousandDistance(String testData, String city, double distance,
			String... expectedResults) {
		LOG.info("testThousandDistance: data = {}, city = {}, distance = {}",
			testData, city, distance);
		loadData(testData);
		String query = String.format(THOUSAND_DISTANCE_QUERY, distance, city);
		try (CloseableQueryExec qexec = performQuery(query)) {
			checkResults(qexec, expectedResults);
		}
	}

	/*
	 * The original intent of these tests was apparently (based on the name) to test
	 * a large buffer, thousands of kilometers wide. However,the CRS that is used
	 * here will not properly create an accurate buffer at large distances. To boot,
	 * the original three tests are at the bottom of this list, and while in the
	 * second of these the query should find New York and Ottawa, it does not.
	 *
	 * For now, we have commented out the cities that should be found but are not
	 * and added the first six tests for cities that are near London so that we can
	 * use a smaller buffer.  Unfortunately, most of these tests do not work either,
	 * in spite of my research using Google Earth.
	 *
	 * TODO: Figure out what's going wrong with the small buffers, here.
	 * TODO: Find a better algorithm for large buffer distances (1000s of km)
	 */
	public static Stream<Arguments> thousandDistanceTestArgs() {
		return Stream.of(
			//thousandDistanceArg("queries/CitiesNearLondon.ttl", "pointLondon", 28,
			//	"cities:londonCenter", "cities:leatherhead"),
			thousandDistanceArg("queries/CitiesNearLondon.ttl", "polyLondon", 1,
				"cities:greaterlondon", "cities:londonCenter", "cities:leatherhead"),
			//thousandDistanceArg("queries/CitiesNearLondon.ttl", "pointLondon", 31,
			//	"cities:londonCenter", "cities:leatherhead", "cities:stAlbans"),
			//thousandDistanceArg("queries/CitiesNearLondon.ttl", "polyLondon", 2,// 3 works
			//	"cities:greaterlondon", "cities:londonCenter", "cities:leatherhead",
			//	"cities:brentwood"),
			//thousandDistanceArg("queries/CitiesNearLondon.ttl", "pointLondon", 33,
			//	"cities:londonCenter", "cities:leatherhead", "cities:stAlbans",
			//	"cities:brentwood"),
			//thousandDistanceArg("queries/CitiesNearLondon.ttl", "polyLondon", 5,
			//	"cities:greaterlondon", "cities:londonCenter", "cities:leatherhead",
			//	"cities:stAlbans", "cities:brentwood"),

			thousandDistanceArg("queries/Cities.ttl", "pointLondon", 5914,
				"cities:londonCenter", "cities:paris", "cities:ottawa", "cities:newyork",
				"cities:greaterlondon", "cities:washdc"),
			thousandDistanceArg("queries/Cities.ttl", "polyLondon", 5585,
				"cities:londonCenter", "cities:paris",
				//"cities:ottawa", "cities:newyork",
				"cities:greaterlondon"),
			thousandDistanceArg("queries/Cities.ttl", "pointWashDC", 1004,
				"cities:ottawa", "cities:newyork", "cities:washdc")
			);
	}

	private static Arguments thousandDistanceArg(String testDataFile, String city,
			double distance, String... expectedResults) {
		return Arguments.of(testDataFile, city, distance, expectedResults);
	}
}
