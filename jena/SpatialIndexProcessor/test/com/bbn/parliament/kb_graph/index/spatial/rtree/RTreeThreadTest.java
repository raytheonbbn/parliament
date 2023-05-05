package com.bbn.parliament.kb_graph.index.spatial.rtree;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bbn.parliament.kb_graph.index.spatial.ThreadTestMethods;

/** @author rbattle */
public class RTreeThreadTest {
	private static ThreadTestMethods testMethods;

	@BeforeAll
	public static void beforeAll() {
		testMethods = new ThreadTestMethods(RTreePropertyFactory.create());
	}

	@AfterAll
	public static void afterAll() {
		testMethods.tearDownKb();
	}

	@SuppressWarnings("static-method")
	@BeforeEach
	public void beforeEach() {
		testMethods.setupIndex();
		testMethods.addData();
	}

	@SuppressWarnings("static-method")
	@AfterEach
	public void afterEach() {
		testMethods.removeIndex();
	}

	@SuppressWarnings("static-method")
	@Test
	public void testSimpleQuery() {
		testMethods.testSimpleQuery();
	}

	@SuppressWarnings("static-method")
	@Test
	public void testCircle3Extents() {
		testMethods.testCircle3Extents();
	}

	@SuppressWarnings("static-method")
	@Test
	public void testOptionalPart() {
		testMethods.testOptionalPart();
	}
}
