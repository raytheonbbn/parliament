package com.bbn.parliament.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.bbn.parliament.client.QuerySolutionStream;
import com.bbn.parliament.server.test_util.GraphUtils;
import com.bbn.parliament.server.test_util.RdfFileLoader;

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
	private static final String SAMPLE_TRIPLES = """
		<%1$s>
			<%2$s> <%3$s> ;
			<%4$s> "Test Item" ;
			.
		""".formatted(TEST_SUBJECT, RDF.type, TEST_CLASS, RDFS.label);

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
		if (StringUtils.isBlank(graphName)) {
			return new ParameterizedSparqlString(defaultQuery);
		} else {
			var pss = new ParameterizedSparqlString(ngQuery);
			pss.setIri("_ng", graphName);
			return pss;
		}
	}

	private static Stream<Arguments> insertAndGetSampleDataTestArgs() {
		return Stream.of(
			Arguments.of("univ-bench.owl",		null,				393, 76, true),
			Arguments.of("univ-bench.owl",		TEST_NG_URI,	393, 76, true),
			Arguments.of("geo-example.ttl",		null,				14, 0, false),
			Arguments.of("geo-example.ttl",		TEST_NG_URI,	14, 0, false),
			Arguments.of("deft-data-load.nt",	null,				97094, 2, true),
			Arguments.of("deft-data-load.nt",	TEST_NG_URI,	97094, 2, true),
			Arguments.of("marley.jsonld",			null,				11, 4, false),
			Arguments.of("marley.jsonld",			TEST_NG_URI,	11, 4, false),
			Arguments.of("University15_20.owl",	null,				9067, 0, false),
			Arguments.of("University15_20.owl",	TEST_NG_URI,	9067, 0, false)
			);
	}

	@ParameterizedTest
	@MethodSource("insertAndGetSampleDataTestArgs")
	public void insertAndGetSampleDataTest(String fileName, String graphName,
			long expectedAllQueryCount, long expectedLabelQueryCount, boolean hasInferredOrBlankNodes) throws IOException {
		var lang = RDFLanguages.pathnameToLang(fileName);
		LOG.info("Loading file '{}' as {} via graph store protocol", fileName, lang.getName());
		var file = new File(DATA_DIR, fileName);
		var returnCode = GraphUtils.insertStatements(graphStoreUrl, graphName, file);
		assertEquals(HttpStatus.OK.value(), returnCode);

		var allQuery = prepareInsertTestQuery(DEFAULT_ALL_QUERY, NG_ALL_QUERY, graphName);
		try (var stream = new QuerySolutionStream(allQuery, sparqlUrl)) {
			assertEquals(expectedAllQueryCount, stream.count());
		}

		var labelQuery = prepareInsertTestQuery(DEFAULT_LABEL_QUERY, NG_LABEL_QUERY, graphName);
		try (var stream = new QuerySolutionStream(labelQuery, sparqlUrl)) {
			assertEquals(expectedLabelQueryCount, stream.count());
		}

		// GET test
		if (!hasInferredOrBlankNodes) {
			HttpResponse<InputStream> response = GraphUtils.getStatements(graphStoreUrl, graphName, lang);

			Model responseModel = ModelFactory.createDefaultModel();
			try (InputStream bstrm = response.body()) {
				responseModel.read(bstrm, null, lang.getName());
			}
			Model fileModel = ModelFactory.createDefaultModel();
			RdfFileLoader.load(file, fileModel);
			assertTrue(responseModel.isIsomorphicWith(fileModel));
		}
	}

	private static boolean multiPostInsertTestFileMatcher(Path path, BasicFileAttributes attrs) {
		var lang = RDFLanguages.pathnameToLang(path.toString());
		return attrs.isRegularFile()
			&& attrs.size() < 12 * 1024 * 1024
			&& (lang != null)
			&& !RDFLanguages.sameLang(lang, Lang.RDFNULL);
	}

	@ParameterizedTest
	@ValueSource(strings = { "", TEST_NG_URI })
	public void multiPostInsertTest(String graphName) throws IOException {
		List<File> files;
		try (
			var paths = Files.find(DATA_DIR.toPath(), Integer.MAX_VALUE,
				GraphStoreTests::multiPostInsertTestFileMatcher, FileVisitOption.FOLLOW_LINKS);
		) {
			files = paths.map(Path::toFile).collect(Collectors.toUnmodifiableList());
		}
		LOG.info("multiPostTest files:\n   {}",
			files.stream().map(File::getPath).collect(Collectors.joining("\n   ")));
		var returnCode = GraphUtils.insertStatements(graphStoreUrl, graphName, files);
		assertEquals(HttpStatus.OK.value(), returnCode);

		var allQuery = prepareInsertTestQuery(DEFAULT_ALL_QUERY, NG_ALL_QUERY, graphName);
		try (var stream = new QuerySolutionStream(allQuery, sparqlUrl)) {
			assertEquals(110133, stream.count());
		}

		var labelQuery = prepareInsertTestQuery(DEFAULT_LABEL_QUERY, NG_LABEL_QUERY, graphName);
		try (var stream = new QuerySolutionStream(labelQuery, sparqlUrl)) {
			assertEquals(82, stream.count());
		}
	}

	@Test
	public void insertErrorTest() throws IOException {
		// Insert invalid n-triples:
		var statusCode = GraphUtils.insertStatements(graphStoreUrl, "oogetyboogetyboo!",
			Lang.NTRIPLES, null);
		var status = Optional.ofNullable(HttpStatus.resolve(statusCode));
		LOG.info("insertErrorTest status code: {} ({})", statusCode,
			status.map(HttpStatus::getReasonPhrase).orElse("unrecognized status code"));
		assertFalse(status.map(HttpStatus::is2xxSuccessful).orElse(false));
	}

	@Test
	public void deleteNgErrorTest() {
		int responseCode = GraphUtils.deleteGraph(graphStoreUrl, TEST_NG_URI);
		LOG.info("Error code on delete of non-existant named graph: {}", responseCode);
		assertEquals(404, responseCode);
	}

	@Test
	public void createAndDeleteNamedGraphTest() throws IOException {
		final String ng1Uri = TEST_NG_URI + "1";
		final String ng2Uri = TEST_NG_URI + "2";

		assertEquals(HttpStatus.OK.value(), GraphUtils.insertStatements(
			graphStoreUrl, SAMPLE_TRIPLES, Lang.TURTLE, null));
		assertEquals(HttpStatus.NO_CONTENT.value(), GraphUtils.replaceGraph(
			graphStoreUrl, "", Lang.TURTLE, ng1Uri));
		assertEquals(HttpStatus.OK.value(), GraphUtils.insertStatements(
			graphStoreUrl, SAMPLE_TRIPLES, Lang.TURTLE, ng2Uri));

		var expectedCounts1 = Map.of("", 2L, ng1Uri, 0L, ng2Uri, 2L);
		var actualCountsMap1 = GraphUtils.getGraphCounts(sparqlUrl);
		assertEquals(expectedCounts1, actualCountsMap1);

		GraphUtils.deleteGraph(graphStoreUrl, ng1Uri);

		var expectedCounts2 = Map.of("", 2L, ng2Uri, 2L);
		var actualCountsMap2 = GraphUtils.getGraphCounts(sparqlUrl);
		assertEquals(expectedCounts2, actualCountsMap2);

		GraphUtils.deleteGraph(graphStoreUrl, ng2Uri);

		var expectedCounts3 = Map.of("", 2L);
		var actualCountsMap3 = GraphUtils.getGraphCounts(sparqlUrl);
		assertEquals(expectedCounts3, actualCountsMap3);

		assertEquals(0, GraphUtils.getAvailableNamedGraphs(sparqlUrl).size());
	}
}
