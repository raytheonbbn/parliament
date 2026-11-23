package com.bbn.parliament.kb_graph.index.spatial.sql.postgres;

import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.bbn.parliament.kb_graph.index.spatial.BuffersTestMethods;

/** @author rbattle */
@Disabled
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
	@ParameterizedTest
	@MethodSource("thousandDistanceTestArgs")
	public void testBufferThousandDistance(String testData, String city, double distance,
		String[] expectedResults) {
		testMethods.testThousandDistance(testData, city, distance, expectedResults);
	}

	private static Stream<Arguments> thousandDistanceTestArgs() {
		return BuffersTestMethods.thousandDistanceTestArgs();
	}
}
