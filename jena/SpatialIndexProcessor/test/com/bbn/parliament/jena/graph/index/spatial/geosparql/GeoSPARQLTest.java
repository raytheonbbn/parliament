package com.bbn.parliament.jena.graph.index.spatial.geosparql;

import static org.junit.Assert.assertEquals;

import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import com.bbn.parliament.jena.graph.index.spatial.AbstractSpatialTest;
import com.bbn.parliament.jena.graph.index.spatial.Constants;

public class GeoSPARQLTest extends AbstractSpatialTest {
	private static final boolean USE_RTREE_INDEX = true;

	@Override
	protected Properties getProperties() {
		Properties properties = new Properties();
		if (USE_RTREE_INDEX) {
			properties.setProperty(Constants.GEOMETRY_INDEX_TYPE, Constants.GEOMETRY_INDEX_RTREE);
		} else {
			properties.setProperty(Constants.GEOMETRY_INDEX_TYPE, Constants.GEOMETRY_INDEX_POSTGRESQL);
			properties.setProperty(Constants.USERNAME, AbstractSpatialTest.USERNAME);
			properties.setProperty(Constants.PASSWORD, AbstractSpatialTest.PASSWORD);
			properties.setProperty(Constants.JDBC_URL, AbstractSpatialTest.JDBC_URL);
		}
		properties.setProperty(Constants.GEOSPARQL_ENABLED, Boolean.TRUE.toString());
		return properties;
	}

	@SuppressWarnings("static-method")
	@Before
	public void load() {
		clearKb();
		loadData("queries/geosparql/data.rdf");
	}

	@Test
	public void testAddItems() {
		assertEquals(10, index.size());
	}

	@Test
	public void testCorrectCRSOnReturn() {
		runTest("queries/geosparql/query-crs.rq", "queries/geosparql/result-crs.ttl");
	}

	// GeoSPARQL relation queries with objects
	@Test
	public void testLiteralObject() {
		runTest("queries/geosparql/query-literal-object.rq", "queries/geosparql/result-literal-object.ttl");
	}

	@Test
	public void testBoundVariableObject() {
		runTest("queries/geosparql/query-variable-bound-object.rq", "queries/geosparql/result-variable-bound-object.ttl");
	}

	@Test
	public void testUnboundVariableObject() {
		runTest("queries/geosparql/query-variable-unbound-object.rq", "queries/geosparql/result-variable-unbound-object.ttl");
	}

	@Test
	public void testURIObject() {
		runTest("queries/geosparql/query-uri-object.rq", "queries/geosparql/result-uri-object.ttl");
	}

	@Test
	public void testInvalidURIObject() {
		runTest("queries/geosparql/query-uri-invalid-object.rq", "queries/geosparql/result-uri-invalid-object.ttl");
	}

	// GeoSPARQL Spec Example Queries
	@Test
	public void testExample1() {
		runTest("queries/geosparql/query-1.rq", "queries/geosparql/result-1.ttl");
	}

	@Test
	public void testExample2() {
		runTest("queries/geosparql/query-2.rq", "queries/geosparql/result-2.ttl");
	}

	@Test
	public void testExample3() {
		runTest("queries/geosparql/query-3.rq", "queries/geosparql/result-3.ttl");
	}

	@Test
	public void testExample4() {
		runTest("queries/geosparql/query-4.rq", "queries/geosparql/result-4.srx");
	}
}
