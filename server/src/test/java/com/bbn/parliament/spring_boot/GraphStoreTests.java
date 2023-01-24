package com.bbn.parliament.spring_boot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.stream.Stream;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.bbn.parliament.client.jena.QuerySolutionStream;
import com.bbn.parliament.client.jena.RDFFormat;
import com.bbn.parliament.test_util.GraphUtils;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(Lifecycle.PER_CLASS)
public class GraphStoreTests {
	private static final String HOST = "localhost";
	private static final String TEST_SUBJECT = "http://example.org/#TestItem";
	private static final String TEST_CLASS = "http://example.org/#TestClass";
	private static final String TEST_NG_URI = "http://example.org/#TestGraph";
	private static final File DATA_DIR = new File(System.getProperty("test.data.path"));
	private static final Logger LOG = LoggerFactory.getLogger(GraphStoreTests.class);

	private static final String EVERYTHING_QUERY = """
		select distinct ?s ?o ?p ?g where {
			{ ?s ?p ?o }
			union
			{ graph ?g { ?s ?p ?o } }
		}
		""";
	private static final String DEFAULT_ALL_QUERY = """
		select distinct ?s ?o ?p where {
			?s ?p ?o .
		}
		""";
	private static final String NG_ALL_QUERY = """
		select distinct ?s ?o ?p where {
			graph ?_ng {
				?s ?p ?o .
			}
		}
		""";
	private static final String DEFAULT_LABEL_QUERY = """
		prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>
		select distinct ?s ?o where {
			?s rdfs:label ?o .
		}
		""";
	private static final String NG_LABEL_QUERY = """
		prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>
		select distinct ?s ?o where {
			graph ?_ng {
				?s rdfs:label ?o .
			}
		}
		""";

	@LocalServerPort
	private int serverPort;

	private String sparqlUrl;
	private String graphStoreUrl;

	@BeforeEach
	public void beforeEach() {
		sparqlUrl = "http://%1$s:%2$s/parliament/sparql".formatted(HOST, serverPort);
		graphStoreUrl = "http://%1$s:%2$s/parliament/graphstore".formatted(HOST, serverPort);
		assertAllClear(graphStoreUrl, sparqlUrl);
	}

	@AfterEach
	public void afterEach() {
		assertAllClear(graphStoreUrl, sparqlUrl);
	}

	private static void assertAllClear(String graphStoreUrl, String sparqlUrl) {
		GraphUtils.clearAll(graphStoreUrl, sparqlUrl);
		try (var stream = new QuerySolutionStream(EVERYTHING_QUERY, sparqlUrl)) {
			assertEquals(0, stream.count(), "Invalid postcondition -- triple store is not empty.");
		}
	}

	private static ParameterizedSparqlString prepareInsertTestQuery(String defaultQuery, String ngQuery, String graphName) {
		if (graphName == null) {
			return new ParameterizedSparqlString(defaultQuery);
		} else {
			var pss = new ParameterizedSparqlString(ngQuery);
			pss.setIri("_ng", graphName);
			return pss;
		}
	}

	private static Stream<Arguments> insertSampleDataTestArgs() {
		return Stream.of(
			Arguments.of("univ-bench.owl",		null,				393, 76),
			Arguments.of("univ-bench.owl",		TEST_NG_URI,	393, 76),
			Arguments.of("geo-example.ttl",		null,				14, 0),
			Arguments.of("geo-example.ttl",		TEST_NG_URI,	14, 0),
			Arguments.of("deft-data-load.nt",	null,				97094, 2),
			Arguments.of("deft-data-load.nt",	TEST_NG_URI,	97094, 2),
			Arguments.of("marley.jsonld",			null,				11, 4),
			Arguments.of("marley.jsonld",			TEST_NG_URI,	11, 4)
			);
	}

	@ParameterizedTest
	@MethodSource("insertSampleDataTestArgs")
	public void insertSampleDataTest(String fileName, String graphName,
			long expectedAllQueryCount, long expectedLabelQueryCount) throws IOException {
		var fileFmt = RDFFormat.parseFilename(fileName);
		LOG.info("Loading file '{}' as {} via graph store protocol", fileName, fileFmt);
		GraphUtils.insertStatements(graphStoreUrl, graphName, new File(DATA_DIR, fileName));

		var allQuery = prepareInsertTestQuery(DEFAULT_ALL_QUERY, NG_ALL_QUERY, graphName);
		try (var stream = new QuerySolutionStream(allQuery, sparqlUrl)) {
			assertEquals(expectedAllQueryCount, stream.count());
		}

		var labelQuery = prepareInsertTestQuery(DEFAULT_LABEL_QUERY, NG_LABEL_QUERY, graphName);
		try (var stream = new QuerySolutionStream(labelQuery, sparqlUrl)) {
			assertEquals(expectedLabelQueryCount, stream.count());
		}
	}

	@Test
	public void multiPostTest() {
		var file1 = new File(DATA_DIR, "univ-bench.owl");
		var file2 = new File(DATA_DIR, "geo-example.ttl");
		LOG.info("Loading files '{}' and '{}' via graph store protocol multi-part request",
			file1.getName(), file2.getName());
		var returnCode = GraphUtils.insertStatements(graphStoreUrl, null, file1, file2);
		assertEquals(200, returnCode);
	}

	@Test
	public void deleteNamedGraphTest() throws IOException {
		try (var stream = new QuerySolutionStream(EVERYTHING_QUERY, sparqlUrl)) {
			assertEquals(0, stream.count(), "Invalid precondition -- triple store is not empty.");
		}
		String triple1 = "<%1$s> <%2$s> <%3$s1> .".formatted(TEST_SUBJECT, RDF.type, TEST_CLASS);
		String query = "select * where { graph ?g { <%1$s> a <%2$s1>. } }";
		GraphUtils.insertStatements(graphStoreUrl, triple1, RDFFormat.NTRIPLES, TEST_NG_URI);

		boolean foundIt = false;
		try (var stream = GraphUtils.doSelectQuery(sparqlUrl, query, TEST_SUBJECT, TEST_CLASS)) {
			foundIt = stream
					.map(qs -> qs.getResource("g"))
					.map(Resource::getURI)
					.filter(uri -> TEST_NG_URI.equals(uri))
					.count() == 1;
			assertTrue(foundIt);
		}

		GraphUtils.deleteGraph(graphStoreUrl, TEST_NG_URI);

		try (var stream = GraphUtils.doSelectQuery(sparqlUrl, query, TEST_SUBJECT, TEST_CLASS)) {
			assertEquals(0, stream.count(), "Invalid postcondition -- triple store is not empty.");
		}
	}

	@Test
	public void createNamedGraphTest() throws IOException {
		try (var stream = new QuerySolutionStream(EVERYTHING_QUERY, sparqlUrl)) {
			assertEquals(0, stream.count(), "Invalid precondition -- triple store is not empty.");
		}

		String graph1Uri = "http://example.org/foo/bar/#NewNamedGraph";
		String triple1 = "<%1$s> <%2$s> <%3$s1> .".formatted(TEST_SUBJECT, RDF.type, TEST_CLASS);
		String query = "select * where { graph ?g { <%1$s> a <%2$s1>. } }";

		GraphUtils.createGraph(graphStoreUrl, RDFFormat.NTRIPLES.getMediaType(), graph1Uri);

		boolean foundIt;
		foundIt = GraphUtils.getAvailableNamedGraphs(sparqlUrl).stream().filter(uri -> graph1Uri.equals(uri)).count() == 1;
		assertTrue(foundIt);

		GraphUtils.insertStatements(graphStoreUrl, triple1, RDFFormat.NTRIPLES, graph1Uri);

		try (var stream = GraphUtils.doSelectQuery(sparqlUrl, query, TEST_SUBJECT, TEST_CLASS)) {
			foundIt = stream
					.map(qs -> qs.getResource("g"))
					.map(Resource::getURI)
					.filter(uri -> graph1Uri.equals(uri))
					.count() == 1;
			assertTrue(foundIt);
		}
	}

	@Test
	public void deleteNgErrorTest() {
		int responseCode = GraphUtils.deleteGraph(graphStoreUrl, TEST_NG_URI);
		boolean caughtException = responseCode == 404;
		if (caughtException) {
			LOG.info("Missing named graph error (delete)");
		}
		assertTrue(caughtException);
	}
}
