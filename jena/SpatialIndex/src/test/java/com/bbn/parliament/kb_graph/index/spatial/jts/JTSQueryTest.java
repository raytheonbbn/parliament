package com.bbn.parliament.kb_graph.index.spatial.jts;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.bbn.parliament.kb_graph.index.spatial.QueryTestMethods;

/** @author rbattle */
public class JTSQueryTest {
	private static QueryTestMethods testMethods;

	@BeforeAll
	public static void beforeAll() {
		testMethods = new QueryTestMethods(JTSPropertyFactory.create());
	}

	@AfterAll
	public static void afterAll() {
		testMethods.tearDownKb();
	}

	@SuppressWarnings("static-method")
	@BeforeEach
	public void beforeEach() {
		testMethods.setupIndex();
	}

	@SuppressWarnings("static-method")
	@AfterEach
	public void afterEach() {
		testMethods.removeIndex();
	}

	@SuppressWarnings("static-method")
	@Test
	public void testTangentialProperPartPoints() {
		testMethods.testTangentialProperPartPoints();
	}

	@SuppressWarnings("static-method")
	@Test
	public void testNonTangentialProperPartPoints() {
		testMethods.testNonTangentialProperPartPoints();
	}

	@SuppressWarnings("static-method")
	@Test
	public void testNonTangentialProperPartMultiple() {
		testMethods.testNonTangentialProperPartMultiple();
	}

	@SuppressWarnings("static-method")
	@Test
	public void testProperPartMultipleResults() {
		testMethods.testProperPartMultipleResults();
	}

	@SuppressWarnings("static-method")
	@Test
	public void testTangentialProperPartRegion() {
		testMethods.testTangentialProperPartRegion();
	}

	@SuppressWarnings("static-method")
	@Test
	public void testQuery() {
		testMethods.testQuery();
	}

	@SuppressWarnings("static-method")
	@Test
	public void testBuildingQuery() {
		testMethods.testBuildingQuery();
	}

	@SuppressWarnings("static-method")
	@Test
	public void testAQuery() {
		testMethods.testAQuery();
	}

	@SuppressWarnings("static-method")
	@Test
	public void testQueryCircle() {
		testMethods.testQueryCircle();
	}

	@SuppressWarnings("static-method")
	@Test
	public void testQueryCoveredCampus() {
		testMethods.testQueryCoveredCampus();
	}

	@SuppressWarnings("static-method")
	@Test
	public void testOnlyPropertyFunctionQuery() {
		testMethods.testOnlyPropertyFunctionQuery();
	}

	@SuppressWarnings("static-method")
	@Test
	public void testOnlyPropertyFunctionQueryUnbound() {
		testMethods.testOnlyPropertyFunctionQueryUnbound();
	}

	@SuppressWarnings("static-method")
	@Test
	public void testOnlyPropertyFunctionQueryNonIndexedURI() {
		testMethods.testOnlyPropertyFunctionQueryNonIndexedURI();
	}

	@SuppressWarnings("static-method")
	@Test
	public void testSharedContext() {
		testMethods.testSharedContext();
	}

	@SuppressWarnings("static-method")
	@Disabled
	@Test
	public void testFirstVarBound() {
		testMethods.testFirstVarBound();
	}
}
