package com.bbn.parliament.spring_boot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.bbn.parliament.jena.bridge.tracker.Tracker;
import com.bbn.parliament.jena.joseki.client.QuerySolutionStream;
import com.bbn.parliament.jena.joseki.client.RDFFormat;
import com.bbn.parliament.test_util.GraphUtils;
import com.bbn.parliament.test_util.RdfResourceLoader;

//@Disabled
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(Lifecycle.PER_CLASS)
public class GraphStoreTests {
	private static final String HOST = "localhost";
	private static final String[] RSRCS_TO_LOAD = { "univ-bench.owl", "University15_20.owl.zip" };
	private static final String TTL_TEST_INPUT = "geo-example.ttl";
	private static final String NT_TEST_INPUT = "test.nt";
	private static final String JSONLD_TEST_INPUT = "test.jsonld";
	private static final Logger LOG = LoggerFactory.getLogger(GraphStoreTests.class);
	private static final String TEST_SUBJECT = "http://example.org/#TestItem";
	private static final String TEST_CLASS = "http://example.org/#TestClass";
	private static final String TEST_LITERAL = "TestLiteral";

	private static final String EVERYTHING_QUERY = """
		select distinct ?s ?o ?p ?g where {
			{ ?s ?p ?o }
			union
			{ graph ?g { ?s ?p ?o } }
		}
		""";
	private static final String OWL_CLASS_QUERY = """
		prefix owl: <http://www.w3.org/2002/07/owl#>
		select distinct ?class where {
			?class a owl:Class .
			filter (!isblank(?class))
		}
		""";

	private static final String TTL_TEST_QUERY = """
			prefix sf: <http://www.opengis.net/ont/sf#>
			select distinct ?s ?p ?o where {
				bind( sf:Point as ?o )
				?s ?p ?o .
			} order by ?s
			""";

	private static final String NT_TEST_QUERY = """
			select distinct ?s ?p ?o where {
				bind( <http://example.org/property> as ?p )
				?s ?p ?o .
			} order by ?s
			""";

	private static final String JSONLD_TEST_QUERY = """
			select distinct ?s ?p ?o where {
				bind( <http://dbpedia.org/resource/Bob_Marley> as ?s )
				?s ?p ?o .
			} order by ?o
			""";


	@LocalServerPort
	private int serverPort;

	private String sparqlUrl;
	private String graphStoreUrl;

	@BeforeEach
	public void beforeEach() {
		sparqlUrl = "http://%1$s:%2$s/parliament/sparql".formatted(HOST, serverPort);
		graphStoreUrl = "http://%1$s:%2$s/parliament/graphstore".formatted(HOST, serverPort);
	}

	@AfterEach
	public void afterEach() {
		GraphUtils.clearAll(graphStoreUrl, sparqlUrl);
	}

	@Test
//	@Disabled
	public void insertSampleDataTest() throws IOException {
		// owl
		try (QuerySolutionStream stream = GraphUtils.doSelectQuery(sparqlUrl, EVERYTHING_QUERY)) {
			assertEquals(0, stream.count(), "Invalid precondition -- triple store is not empty.");
		}

		loadSampleData();

		try (QuerySolutionStream stream = GraphUtils.doSelectQuery(sparqlUrl, OWL_CLASS_QUERY)) {
			assertEquals(43, stream.count());
			assertEquals(0, Tracker.getInstance().getTrackableIDs().size());
		}

		GraphUtils.deleteGraph(graphStoreUrl, null);
		try (QuerySolutionStream stream = GraphUtils.doSelectQuery(sparqlUrl, OWL_CLASS_QUERY)) {
			assertEquals(0, stream.count(), "Invalid postcondition -- triple store is not empty.");
		}


		// ttl
		try (InputStream is = getClass().getClassLoader().getResourceAsStream(TTL_TEST_INPUT)) {
			if (is == null) {
				fail("Unable to find resource '%1$s'".formatted(TTL_TEST_INPUT));
			}
			LOG.debug("rdf format: "+RDFFormat.parseFilename(TTL_TEST_INPUT));
			GraphUtils.insertStatements(graphStoreUrl, is, RDFFormat.parseFilename(TTL_TEST_INPUT), null);
		}

		try (QuerySolutionStream stream = GraphUtils.doSelectQuery(sparqlUrl, TTL_TEST_QUERY)) {
			assertEquals(3, stream.count());
			assertEquals(0, Tracker.getInstance().getTrackableIDs().size());
		}

		GraphUtils.deleteGraph(graphStoreUrl, null);
		try (QuerySolutionStream stream = GraphUtils.doSelectQuery(sparqlUrl, TTL_TEST_QUERY)) {
			assertEquals(0, stream.count(), "Invalid postcondition -- triple store is not empty.");
		}

		// nt
		try (InputStream is = getClass().getClassLoader().getResourceAsStream(NT_TEST_INPUT)) {
			if (is == null) {
				fail("Unable to find resource '%1$s'".formatted(NT_TEST_INPUT));
			}
			LOG.debug("rdf format: "+RDFFormat.parseFilename(NT_TEST_INPUT));
			GraphUtils.insertStatements(graphStoreUrl, is, RDFFormat.parseFilename(NT_TEST_INPUT), null);
		}

		try (QuerySolutionStream stream = GraphUtils.doSelectQuery(sparqlUrl, NT_TEST_QUERY)) {
			assertEquals(30, stream.count());
			assertEquals(0, Tracker.getInstance().getTrackableIDs().size());
		}

		GraphUtils.deleteGraph(graphStoreUrl, null);
		try (QuerySolutionStream stream = GraphUtils.doSelectQuery(sparqlUrl, NT_TEST_QUERY)) {
			assertEquals(0, stream.count(), "Invalid postcondition -- triple store is not empty.");
		}

		// json-ld
		try (InputStream is = getClass().getClassLoader().getResourceAsStream(JSONLD_TEST_INPUT)) {
			if (is == null) {
				fail("Unable to find resource '%1$s'".formatted(JSONLD_TEST_INPUT));
			}
			LOG.debug("rdf format: "+RDFFormat.parseFilename(JSONLD_TEST_INPUT));
			GraphUtils.insertStatements(graphStoreUrl, is, RDFFormat.parseFilename(JSONLD_TEST_INPUT), null);
		}

		try (QuerySolutionStream stream = GraphUtils.doSelectQuery(sparqlUrl, JSONLD_TEST_QUERY)) {
			assertEquals(5, stream.count());
			assertEquals(0, Tracker.getInstance().getTrackableIDs().size());
		}

		GraphUtils.deleteGraph(graphStoreUrl, null);
		try (QuerySolutionStream stream = GraphUtils.doSelectQuery(sparqlUrl, JSONLD_TEST_QUERY)) {
			assertEquals(0, stream.count(), "Invalid postcondition -- triple store is not empty.");
		}

	}


	/*
	 * Non-empty inserts into non-existing graphs will, however, implicitly create
	 * those graphs, i.e., an implementation fulfilling an update request should
	 * silently an automatically create graphs that do not exist before triples are
	 * inserted into them, and must return with failure if it fails to do so for any
	 * reason.
	 */
	@Test
//	@Disabled
	public void insertIntoNamedGraphAndQueryTest() {
		try (QuerySolutionStream stream = GraphUtils.doSelectQuery(sparqlUrl, EVERYTHING_QUERY)) {
			assertEquals(0, stream.count(), "Invalid precondition -- triple store is not empty.");
		}

		String graph1Uri = "http://example.org/foo/#Graph1";
		String triple1 = "<%1$s> <%2$s> <%3$s1> .".formatted(TEST_SUBJECT, RDF.type, TEST_CLASS);
		String query = "select * where { graph <%1$s> { ?x a <%2$s1>. } }";

		GraphUtils.insertStatements(graphStoreUrl, triple1, RDFFormat.NTRIPLES, URLEncoder.encode(graph1Uri, StandardCharsets.UTF_8));

		boolean foundIt = false;
		try (QuerySolutionStream stream = GraphUtils.doSelectQuery(sparqlUrl, query, graph1Uri, TEST_CLASS)) {
			foundIt = stream
					.map(qs -> qs.getResource("x"))
					.map(Resource::getURI)
					.filter(uri -> TEST_SUBJECT.equals(uri))
					.count() == 1;
			assertEquals(0, Tracker.getInstance().getTrackableIDs().size());
			assertTrue(foundIt);
		}

		GraphUtils.clearAll(graphStoreUrl, sparqlUrl);

		try (QuerySolutionStream stream = GraphUtils.doSelectQuery(sparqlUrl, query, graph1Uri, TEST_CLASS)) {
			assertEquals(0, stream.count(), "Invalid postcondition -- triple store is not empty.");
		}
	}

	@Test
//	@Disabled
	public void insertIntoDefaultGraphAndQueryTest() {
		try (QuerySolutionStream stream = GraphUtils.doSelectQuery(sparqlUrl, EVERYTHING_QUERY)) {
			assertEquals(0, stream.count(), "Invalid precondition -- triple store is not empty.");
		}

		String triple1 = "<%1$s> <%2$s> <%3$s1> .".formatted(TEST_SUBJECT, RDF.type, TEST_CLASS);
		String query = "select * where { ?x a <%1$s1>. } ";

		GraphUtils.insertStatements(graphStoreUrl, triple1, RDFFormat.NTRIPLES, null);

		boolean foundIt = false;
		try (QuerySolutionStream stream = GraphUtils.doSelectQuery(sparqlUrl, query, TEST_CLASS)) {
			foundIt = stream
					.map(qs -> qs.getResource("x"))
					.map(Resource::getURI)
					.filter(uri -> TEST_SUBJECT.equals(uri))
					.count() == 1;
			assertEquals(0, Tracker.getInstance().getTrackableIDs().size());
			assertTrue(foundIt);
		}

		GraphUtils.clearAll(graphStoreUrl, sparqlUrl);

		try (QuerySolutionStream stream = GraphUtils.doSelectQuery(sparqlUrl, query, TEST_CLASS)) {
			assertEquals(0, stream.count(), "Invalid postcondition -- triple store is not empty.");
		}
	}

	@Test
//	@Disabled
	public void deleteNamedGraphTest() {
		try (QuerySolutionStream stream = GraphUtils.doSelectQuery(sparqlUrl, EVERYTHING_QUERY)) {
			assertEquals(0, stream.count(), "Invalid precondition -- triple store is not empty.");
		}
		String graph1Uri = "http://example.org/foo/bar/#Graph1";
		String triple1 = "<%1$s> <%2$s> <%3$s1> .".formatted(TEST_SUBJECT, RDF.type, TEST_CLASS);
		String query = "select * where { graph ?g { <%1$s> a <%2$s1>. } }";
		GraphUtils.insertStatements(graphStoreUrl, triple1, RDFFormat.NTRIPLES, URLEncoder.encode(graph1Uri, StandardCharsets.UTF_8));

		boolean foundIt = false;
		try (QuerySolutionStream stream = GraphUtils.doSelectQuery(sparqlUrl, query, TEST_SUBJECT, TEST_CLASS)) {
			foundIt = stream
					.map(qs -> qs.getResource("g"))
					.map(Resource::getURI)
					.filter(uri -> graph1Uri.equals(uri))
					.count() == 1;
			assertEquals(0, Tracker.getInstance().getTrackableIDs().size());
			assertTrue(foundIt);
		}

		GraphUtils.deleteGraph(graphStoreUrl, graph1Uri);

		try (QuerySolutionStream stream = GraphUtils.doSelectQuery(sparqlUrl, query, TEST_SUBJECT, TEST_CLASS)) {
			assertEquals(0, stream.count(), "Invalid postcondition -- triple store is not empty.");
		}
	}

	@Test
//	@Disabled
	public void createNamedGraphTest() {
		try (QuerySolutionStream stream = GraphUtils.doSelectQuery(sparqlUrl, EVERYTHING_QUERY)) {
			assertEquals(0, stream.count(), "Invalid precondition -- triple store is not empty.");
		}

		String graph1Uri = "http://example.org/foo/bar/#NewNamedGraph";
		String triple1 = "<%1$s> <%2$s> <%3$s1> .".formatted(TEST_SUBJECT, RDF.type, TEST_CLASS);
		String query = "select * where { graph ?g { <%1$s> a <%2$s1>. } }";

		GraphUtils.createGraph(graphStoreUrl, RDFFormat.NTRIPLES.getMediaType(), URLEncoder.encode(graph1Uri, StandardCharsets.UTF_8));

		boolean foundIt;
		foundIt = GraphUtils.getAvailableNamedGraphs(sparqlUrl).stream().filter(uri -> graph1Uri.equals(uri)).count() == 1;
		assertTrue(foundIt);

		GraphUtils.insertStatements(graphStoreUrl, triple1, RDFFormat.NTRIPLES, URLEncoder.encode(graph1Uri, StandardCharsets.UTF_8));

		try (QuerySolutionStream stream = GraphUtils.doSelectQuery(sparqlUrl, query, TEST_SUBJECT, TEST_CLASS)) {
			foundIt = stream
					.map(qs -> qs.getResource("g"))
					.map(Resource::getURI)
					.filter(uri -> graph1Uri.equals(uri))
					.count() == 1;
			assertEquals(0, Tracker.getInstance().getTrackableIDs().size());
			assertTrue(foundIt);
		}
	}

	@Test
//	@Disabled
	public void deleteNgErrorTest() {
		String graph1Uri = "http://example.org/foo/bar/#Graph1";
		Set<String> allGraphs = GraphUtils.getAvailableNamedGraphs(sparqlUrl);
		int responseCode = GraphUtils.deleteGraph(graphStoreUrl, graph1Uri);
		boolean caughtException = responseCode == 404;
		if (caughtException)
			LOG.info("Missing named graph error (delete)");
		assertTrue(caughtException);
	}
//TODO: create test for:
/*
 * Note that the deletion of non-existing triples has no effect,
 * i.e., triples in the QuadData that did not exist in the Graph Store are ignored.
 * Blank nodes are not permitted in the QuadData, as these do not match any existing data.
 */

	private void loadSampleData() {
		Stream.of(RSRCS_TO_LOAD).forEach(
			rsrcName -> RdfResourceLoader.load(rsrcName, this::loadRdf));
	}

	private void loadRdf(String rsrcName, RDFFormat rdfFormat, InputStream input) {
		int status = loadRdf(input, rdfFormat.getMediaType());
		if (status != HttpStatus.OK.value()) {
			throw new HttpException(status, "",
				"Failure inserting RDF from %1$s".formatted(rsrcName));
		}
	}

	private int loadRdf(InputStream in, String mediaType) {
		var request = HttpRequest.newBuilder()
			.uri(URI.create(graphStoreUrl + "?default"))
			.POST(HttpRequest.BodyPublishers.ofInputStream(() -> in))
			.header(HttpHeaders.CONTENT_TYPE, mediaType)
			.header(HttpHeaders.ACCEPT, MediaType.ALL_VALUE)
			.header(HttpHeaders.ACCEPT_CHARSET, StandardCharsets.UTF_8.name())
			.build();
		return GraphUtils.sendRequest(request).statusCode();
	}

}
