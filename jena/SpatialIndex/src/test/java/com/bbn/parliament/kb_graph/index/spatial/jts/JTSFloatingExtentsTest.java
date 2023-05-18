package com.bbn.parliament.kb_graph.index.spatial.jts;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.bbn.parliament.kb_graph.index.spatial.FloatingExtentsTestMethods;

public class JTSFloatingExtentsTest {
	private static FloatingExtentsTestMethods testMethods;

	@BeforeAll
	public static void beforeAll() {
		testMethods = new FloatingExtentsTestMethods(JTSPropertyFactory.create());
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
	@Disabled
	@Test
	public void testNot() {
		testMethods.testNot();
	}

	@SuppressWarnings("static-method")
	@Test
	public void testThreeExtentsInCircle() {
		testMethods.testThreeExtentsInCircle();
	}

	@SuppressWarnings("static-method")
	@Test
	public void testSinglePoint() {
		testMethods.testSinglePoint();
	}

	@SuppressWarnings("static-method")
	@Test
	public void testCircleExtentsKnown() {
		testMethods.testCircleExtentsKnown();
	}

	@SuppressWarnings("static-method")
	@Test
	public void testCircleReturnFloater() {
		testMethods.testCircleReturnFloater();
	}

	@SuppressWarnings("static-method")
	@Test
	public void testCircleExtentsUnknown() {
		testMethods.testCircleExtentsUnknown();
	}

	@SuppressWarnings("static-method")
	@Test
	public void testExtentsUnknownReorderedQuery() {
		testMethods.testExtentsUnknownReorderedQuery();
	}

	@SuppressWarnings("static-method")
	@Test
	public void testExtentsUnknownSize0Circle() {
		testMethods.testExtentsUnknownSize0Circle();
	}

	@SuppressWarnings("static-method")
	@Test
	public void testExtentsSmallCircle() {
		testMethods.testExtentsSmallCircle();
	}

	@SuppressWarnings("static-method")
	@Test
	public void testExtentsUnknownMultipleResults() {
		testMethods.testExtentsUnknownMultipleResults();
	}
}
