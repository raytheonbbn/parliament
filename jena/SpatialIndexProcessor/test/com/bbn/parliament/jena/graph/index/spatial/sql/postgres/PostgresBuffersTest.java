package com.bbn.parliament.jena.graph.index.spatial.sql.postgres;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.bbn.parliament.jena.graph.index.spatial.BuffersTestMethods;

/** @author rbattle */
@Disabled
@RunWith(JUnitPlatform.class)
public class PostgresBuffersTest {
	private static BuffersTestMethods testMethods;

	@BeforeAll
	public static void beforeAll() {
		testMethods = new BuffersTestMethods(PostgresPropertyFactory.create());
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
	public void testBufferPoint() {
		testMethods.testBufferPoint();
	}

	@SuppressWarnings("static-method")
	@Test
	public void testBufferPointReverse() {
		testMethods.testBufferPointReverse();
	}

	@SuppressWarnings("static-method")
	@Test
	public void testBufferRegion() {
		testMethods.testBufferRegion();
	}

	@SuppressWarnings("static-method")
	@Test
	public void testBufferVariableDistance() {
		testMethods.testBufferVariableDistance();
	}

	@SuppressWarnings("static-method")
	@Test
	public void testBufferBoundVariableExtent() {
		testMethods.testBufferBoundVariableExtent();
	}

	@SuppressWarnings("static-method")
	@Test
	public void testBufferZeroDistance() {
		testMethods.testBufferZeroDistance();
	}

	@SuppressWarnings("static-method")
	@Test
	public void testBufferThousandDistance() {
		testMethods.testBufferThousandDistance();
	}
}
