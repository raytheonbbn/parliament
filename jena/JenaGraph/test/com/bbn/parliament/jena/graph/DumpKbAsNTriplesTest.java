package com.bbn.parliament.jena.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.TreeSet;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.bbn.parliament.client.jena.RDFFormat;
import com.bbn.parliament.jni.KbConfig;
import com.bbn.parliament.jni.KbInstance;

public class DumpKbAsNTriplesTest {
	private static final File TEST_DATA_DIR = new File("data");
	private static final File TEST_INPUT = new File(TEST_DATA_DIR, "DumpTestData.ttl");
	private static final String OUTPUT_DELIMITER = "########## Actual dump output ##########";

	private KbConfig config;
	private KbGraph graph;
	private Model model;

	@BeforeEach
	public void beforeEach() {
		afterEach();
		config = new KbConfig();
		config.readFromFile();
		KbInstance.deleteKb(config, null);
		graph = KbGraphFactory.createDefaultGraph();
		model = ModelFactory.createModelForGraph(graph);
	}

	@AfterEach
	public void afterEach() {
		if (model != null) {
			model.close();
		}
		model = null;
		if (graph != null) {
			graph.close();
		}
		graph = null;
		if (config != null) {
			KbInstance.deleteKb(config, null);
		}
		config = null;
	}

	@ParameterizedTest
	@CsvSource({
		"false, DumpTestExpectedResult-utf8.nt",
		"true, DumpTestExpectedResult-ascii.nt",
	})
	public void dumpKB(boolean useAsciiEncoding, String expectedOutputFileName) throws IOException {
		// Set up the KB:
		try (InputStream in = getRsrcAsStream(TEST_INPUT)) {
			model.read(in, null, RDFFormat.parseFilename(TEST_INPUT).toString());
		}

		// Test that inferred statements are present.  The comparison with the
		// expected result will implicitly test that the inferred statements
		// are not exported.
		assertEquals(2L, countNamedEntities(), "No inferred statements are present");

		Set<String> expectedOutput;
		File expectedTestOutput = new File(TEST_DATA_DIR, expectedOutputFileName);
		try (InputStream in = getRsrcAsStream(expectedTestOutput)) {
			expectedOutput = getLinesFromFile(in);
		}
		String actualOutputStr;
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			graph.dumpAsNTriples(out, false, false, useAsciiEncoding);
			actualOutputStr = out.toString(StandardCharsets.UTF_8.name());
		}
		System.out.format("%n%1$s%n%2$s%1$s%n%n", OUTPUT_DELIMITER,
			actualOutputStr);	// for ease of debugging -- look in junit result file
		Set<String> actualOutput;
		try (ByteArrayInputStream in = new ByteArrayInputStream(
			actualOutputStr.getBytes(StandardCharsets.UTF_8))) {
			actualOutput = getLinesFromFile(in);
		}
		assertTrue(setsAreIdentical(expectedOutput, actualOutput));
	}

	private static InputStream getRsrcAsStream(File rsrcPath) throws FileNotFoundException {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		InputStream result = cl.getResourceAsStream(rsrcPath.getPath());
		if (result == null) {
			throw new FileNotFoundException("Could not load resource: '%1$s'"
				.formatted(rsrcPath.getPath()));
		}
		return result;
	}

	private long countNamedEntities() {
		final String query = """
			select (count(distinct *) as ?count) where {
			?x a <http://example.org/#NamedEntity> .
		}
		""";
		try (QueryExecution qe = QueryExecutionFactory.create(query, model)) {
			ResultSet rs = qe.execSelect();
			if (rs.hasNext()) {
				QuerySolution qs = rs.next();
				return qs.getLiteral("count").getLong();
			} else {
				throw new IllegalStateException("This should never happen");
			}
		}
	}

	private static Set<String> getLinesFromFile(InputStream in) throws IOException {
		Set<String> result = new TreeSet<>();
		try (
			Reader rdr = new InputStreamReader(in, StandardCharsets.UTF_8);
			BufferedReader brdr = new BufferedReader(rdr);
		) {
			brdr.lines().forEach(line -> result.add(line));
		}
		return result;
	}

	private static boolean setsAreIdentical(Set<String> expected, Set<String> actual) {
		Set<String> eMinusA = new TreeSet<>(expected);
		eMinusA.removeAll(actual);
		Set<String> aMinusE = new TreeSet<>(actual);
		aMinusE.removeAll(expected);
		if (eMinusA.isEmpty() && aMinusE.isEmpty()) {
			return true;
		} else {
			System.out.format("Sets are not identical.%n");
			System.out.format("Expected strings not found in actual result:%n");
			eMinusA.forEach(str -> System.out.format("   %1$s%n", str));
			System.out.format("Actual strings not found in expected result:%n");
			aMinusE.forEach(str -> System.out.format("   %1$s%n", str));
			return false;
		}
	}
}
