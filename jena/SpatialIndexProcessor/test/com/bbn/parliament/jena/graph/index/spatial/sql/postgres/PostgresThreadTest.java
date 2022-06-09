package com.bbn.parliament.jena.graph.index.spatial.sql.postgres;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.bbn.parliament.jena.graph.index.spatial.ThreadTestMethods;
import com.bbn.parliament.jena.graph.index.spatial.sql.PersistentStore;

/** @author rbattle */
@Disabled
public class PostgresThreadTest {
	private static ThreadTestMethods testMethods;

	@BeforeAll
	public static void beforeAll() {
		testMethods = new ThreadTestMethods(PostgresPropertyFactory.create());
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
		PersistentStore ps = PersistentStore.getInstance();
		System.out.println(String.format("%d active, %d idle", ps.getNumActive(), ps.getNumIdle()));

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
