package com.bbn.parliament.jena.graph.index.spatial.geosparql;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Properties;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bbn.parliament.jena.graph.index.spatial.Constants;
import com.bbn.parliament.jena.graph.index.spatial.SpatialTestDataset;

public class GeoSPARQLTest {
	private static final boolean USE_RTREE_INDEX = true;

	private static SpatialTestDataset testMethods;

	@BeforeAll
	public static void beforeAll() {
		Properties properties = new Properties();
		if (USE_RTREE_INDEX) {
			properties.setProperty(Constants.GEOMETRY_INDEX_TYPE, Constants.GEOMETRY_INDEX_RTREE);
		} else {
			properties.setProperty(Constants.GEOMETRY_INDEX_TYPE, Constants.GEOMETRY_INDEX_POSTGRESQL);
			properties.setProperty(Constants.USERNAME, SpatialTestDataset.USERNAME);
			properties.setProperty(Constants.PASSWORD, SpatialTestDataset.PASSWORD);
			properties.setProperty(Constants.JDBC_URL, SpatialTestDataset.JDBC_URL);
		}
		properties.setProperty(Constants.GEOSPARQL_ENABLED, Boolean.TRUE.toString());

		testMethods = new SpatialTestDataset(properties);
	}

	@AfterAll
	public static void afterAll() {
		testMethods.tearDownKb();
	}

	@SuppressWarnings("static-method")
	@BeforeEach
	public void beforeEach() {
		testMethods.setupIndex();
		testMethods.clearKb();
		testMethods.loadData("queries/geosparql/data.rdf");
	}

	@SuppressWarnings("static-method")
	@AfterEach
	public void afterEach() {
		testMethods.removeIndex();
	}

	@SuppressWarnings("static-method")
	@Test
	public void testAddItems() {
		assertEquals(10, testMethods.getIndex().size());
	}

	@SuppressWarnings("static-method")
	@Test
	public void testCorrectCRSOnReturn() {
		testMethods.runTest(
			"queries/geosparql/query-crs.rq",
			"queries/geosparql/result-crs.ttl");
	}

	// GeoSPARQL relation queries with objects
	@SuppressWarnings("static-method")
	@Test
	public void testLiteralObject() {
		testMethods.runTest(
			"queries/geosparql/query-literal-object.rq",
			"queries/geosparql/result-literal-object.ttl");
	}

	@SuppressWarnings("static-method")
	@Test
	public void testBoundVariableObject() {
		testMethods.runTest(
			"queries/geosparql/query-variable-bound-object.rq",
			"queries/geosparql/result-variable-bound-object.ttl");
	}

	@SuppressWarnings("static-method")
	@Test
	public void testUnboundVariableObject() {
		testMethods.runTest(
			"queries/geosparql/query-variable-unbound-object.rq",
			"queries/geosparql/result-variable-unbound-object.ttl");
	}

	@SuppressWarnings("static-method")
	@Test
	public void testURIObject() {
		testMethods.runTest(
			"queries/geosparql/query-uri-object.rq",
			"queries/geosparql/result-uri-object.ttl");
	}

	@SuppressWarnings("static-method")
	@Test
	public void testInvalidURIObject() {
		testMethods.runTest(
			"queries/geosparql/query-uri-invalid-object.rq",
			"queries/geosparql/result-uri-invalid-object.ttl");
	}

	// GeoSPARQL Spec Example Queries
	@SuppressWarnings("static-method")
	@Test
	public void testExample1() {
		testMethods.runTest(
			"queries/geosparql/query-1.rq",
			"queries/geosparql/result-1.ttl");
	}

	@SuppressWarnings("static-method")
	@Test
	public void testExample2() {
		testMethods.runTest(
			"queries/geosparql/query-2.rq",
			"queries/geosparql/result-2.ttl");
	}

	@SuppressWarnings("static-method")
	@Test
	public void testExample3() {
		testMethods.runTest(
			"queries/geosparql/query-3.rq",
			"queries/geosparql/result-3.ttl");
	}

	@SuppressWarnings("static-method")
	@Test
	public void testExample4() {
		testMethods.runTest(
			"queries/geosparql/query-4.rq",
			"queries/geosparql/result-4.srx");
	}
}
