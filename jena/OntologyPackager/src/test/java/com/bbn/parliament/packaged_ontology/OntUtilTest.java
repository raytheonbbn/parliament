package com.bbn.parliament.packaged_ontology;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QuerySolution;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class OntUtilTest {
	@BeforeAll
	public static void beforeAll() {
	}

	@AfterAll
	public static void afterAll() {
	}

	@BeforeEach
	public void beforeEach() {
	}

	@AfterEach
	public void afterEach() {
	}

	@SuppressWarnings("static-method")
	@Test
	public void smokeTest() {
		System.out.format("Prefixes:%n%1$s%n", OntUtil.getSparqlPrefixes());

		AtomicLong countWithoutInf = new AtomicLong(0);
		ParameterizedSparqlString pss = OntUtil.getPssFromRsrc("classPropPairs.sparql");
		OntUtil.execOntSelect(pss, InferenceOption.WITHOUT, qs -> countWithoutInf.incrementAndGet());
		try (Stream<QuerySolution> strm = OntUtil.execOntSelect(pss, InferenceOption.WITH)) {
			long countWithInf = strm.count();
			System.out.format("Count without inference: %1$4d%nCount with inference:    %2$4d%n",
				countWithoutInf.get(), countWithInf);
			assertTrue(countWithInf > countWithoutInf.get());
			assertTrue(countWithoutInf.get() > 25);
		}
	}

	@SuppressWarnings("static-method")
	@ParameterizedTest
	@ValueSource(strings = { "Hello", "World" })
	public void testWithStringParameter(String argument) {
		assertNotNull(argument);
	}
}
