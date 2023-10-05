package org.semwebcentral.parliament.sparql_query_builder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;

import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.core.Prologue;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PathBuilderTest {
	private static final Prologue stdProlog = new Prologue(PrefixMapping.Standard);
	private static final Logger LOG = LoggerFactory.getLogger(PathBuilderTest.class);

	@BeforeAll
	public static void beforeAll() throws Exception {
	}

	@AfterAll
	public static void afterAll() throws Exception {
	}

	@BeforeEach
	public void beforeEach() throws Exception {
	}

	@AfterEach
	public void afterEach() throws Exception {
	}

	@SuppressWarnings("static-method")	// NOPMD - AvoidDuplicateLiterals
	@Test
	public void testTrivialPath() {
		String path = new PathBuilder()
			.pushIri(RDF.first)
			.build()
			.toString(stdProlog);
		assertEquals("rdf:first", path);
		LOG.info("Trivial path:  '{}'", path);
	}

	@SuppressWarnings("static-method")	// NOPMD - AvoidDuplicateLiterals
	@Test
	public void testSequence() {
		String path1 = new PathBuilder()
			.pushIri(RDF.rest)
			.pushIri(RDF.first)
			.sequence()
			.build()
			.toString(stdProlog);
		assertEquals("rdf:rest/rdf:first", path1);
		LOG.info("Simple sequence:  '{}'", path1);

		String path2 = new PathBuilder()
			.pushIri(RDF.rest)
			.pushIri(RDF.first)
			.sequence()
			.pushIri(RDF.type)
			.sequence()
			.build()
			.toString(stdProlog);
		assertEquals("(rdf:rest/rdf:first)/rdf:type", path2);
		LOG.info("Complex sequence 1:  '{}'", path2);

		String path3 = new PathBuilder()
			.pushIri(RDF.rest)
			.pushIri(RDF.first)
			.pushIri(RDF.type)
			.sequence()
			.sequence()
			.build()
			.toString(stdProlog);
		assertEquals("rdf:rest/(rdf:first/rdf:type)", path3);
		LOG.info("Complex sequence 2:  '{}'", path3);
	}

	@SuppressWarnings("static-method")	// NOPMD - AvoidDuplicateLiterals
	@Test
	public void testAlternative() {
		String path1 = new PathBuilder()
			.pushIri(RDF.rest)
			.pushIri(RDF.first)
			.alternative()
			.build()
			.toString(stdProlog);
		assertEquals("rdf:rest|rdf:first", path1);
		LOG.info("Simple alternative:  '{}'", path1);

		String path2 = new PathBuilder()
			.pushIri(RDF.rest)
			.pushIri(RDF.first)
			.alternative()
			.pushIri(RDF.type)
			.alternative()
			.build()
			.toString(stdProlog);
		assertEquals("(rdf:rest|rdf:first)|rdf:type", path2);
		LOG.info("Complex alternative 1:  '{}'", path2);

		String path3 = new PathBuilder()
			.pushIri(RDF.rest)
			.pushIri(RDF.first)
			.pushIri(RDF.type)
			.alternative()
			.alternative()
			.build()
			.toString(stdProlog);
		assertEquals("rdf:rest|(rdf:first|rdf:type)", path3);
		LOG.info("Complex alternative 2:  '{}'", path3);
	}

	@SuppressWarnings("static-method")	// NOPMD - AvoidDuplicateLiterals
	@Test
	public void testInvert() {
		String path = new PathBuilder()
			.pushIri(RDF.first)
			.invert()
			.build()
			.toString(stdProlog);
		assertEquals("^rdf:first", path);
		LOG.info("Inverted path:  '{}'", path);
	}

	private static Stream<Arguments> testLengthBetween() {
		return Stream.of(
			Arguments.of("((rdf:rest){2,4})/rdf:first", 2, 4),
			Arguments.of("((rdf:rest){3})/rdf:first", 3, 3),
			Arguments.of("((rdf:rest){,3})/rdf:first", PathBuilder.UNSPECIFIED, 3),
			Arguments.of("((rdf:rest){,3})/rdf:first", 0, 3),
			Arguments.of("((rdf:rest){3,})/rdf:first", 3, PathBuilder.UNSPECIFIED),
			Arguments.of("(rdf:rest)*/rdf:first", PathBuilder.UNSPECIFIED, PathBuilder.UNSPECIFIED));
	}

	@SuppressWarnings("static-method")	// NOPMD - AvoidDuplicateLiterals
	@ParameterizedTest
	@MethodSource
	public void testLengthBetween(String expectedPath, long min, long max) {
		String actualPath = new PathBuilder()
			.pushIri(RDF.rest)
			.lengthBetween(min, max)
			.pushIri(RDF.first)
			.sequence()
			.build()
			.toString(stdProlog);
		assertEquals(expectedPath, actualPath);
		String minStr = (min == PathBuilder.UNSPECIFIED) ? "UNSPECIFIED" : Long.toString(min);
		String maxStr = (max == PathBuilder.UNSPECIFIED) ? "UNSPECIFIED" : Long.toString(max);
		LOG.info("LengthBetween ({}, {}):  '{}'", minStr, maxStr, actualPath);
	}

	@SuppressWarnings("static-method")	// NOPMD - AvoidDuplicateLiterals
	@Test
	public void testNegate() {
		assertThrows(UnsupportedOperationException.class, () -> {
			new PathBuilder()
				.pushIri(RDF.first)
				.negate()
				.build();
		});
	}

	@SuppressWarnings("static-method")	// NOPMD - AvoidDuplicateLiterals
	@Test
	public void testOneOrMore() {
		String path = new PathBuilder()
			.pushIri(RDF.rest)
			.oneOrMore()
			.pushIri(RDF.first)
			.sequence()
			.build()
			.toString(stdProlog);
		assertEquals("(rdf:rest)+/rdf:first", path);
		LOG.info("One or more:  '{}'", path);
	}

	@SuppressWarnings("static-method")	// NOPMD - AvoidDuplicateLiterals
	@Test
	public void testZeroOrMore() {
		String path = new PathBuilder()
			.pushIri(RDF.rest)
			.zeroOrMore()
			.pushIri(RDF.first)
			.sequence()
			.build()
			.toString(stdProlog);
		assertEquals("(rdf:rest)*/rdf:first", path);
		LOG.info("Zero or more:  '{}'", path);
	}

	@SuppressWarnings("static-method")	// NOPMD - AvoidDuplicateLiterals
	@Test
	public void testZeroOrOne() {
		String path = new PathBuilder()
			.pushIri(RDF.rest)
			.zeroOrOne()
			.pushIri(RDF.first)
			.sequence()
			.build()
			.toString(stdProlog);
		assertEquals("(rdf:rest)?/rdf:first", path);
		LOG.info("Zero or one:  '{}'", path);
	}
}
