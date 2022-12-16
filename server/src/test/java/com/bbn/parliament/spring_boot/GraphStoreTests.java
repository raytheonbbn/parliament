package com.bbn.parliament.spring_boot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
	private static final String CLASS_QUERY = """
		prefix owl: <http://www.w3.org/2002/07/owl#>
		select distinct ?class where {
			?class a owl:Class .
			filter (!isblank(?class))
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
	}

	@AfterEach
	public void afterEach() {
		GraphUtils.clearAll(graphStoreUrl, sparqlUrl);
	}

	@Test
	@Disabled
	public void insertSampleDataTest() {
		try (QuerySolutionStream stream = GraphUtils.doSelectQuery(sparqlUrl, EVERYTHING_QUERY)) {
			assertEquals(0, stream.count(), "Invalid precondition -- triple store is not empty.");
		}

		loadSampleData();

		try (QuerySolutionStream stream = GraphUtils.doSelectQuery(sparqlUrl, CLASS_QUERY)) {
			assertEquals(43, stream.count());
			assertEquals(0, Tracker.getInstance().getTrackableIDs().size());
		}

		GraphUtils.deleteGraph(graphStoreUrl, null);

		try (QuerySolutionStream stream = GraphUtils.doSelectQuery(sparqlUrl, CLASS_QUERY)) {
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
	@Disabled
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
			LOG.debug("insertIntoNamedGraphAndQueryTest: Tracker.getInstance().getTrackableIDs():"+Tracker.getInstance().getTrackableIDs());
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
	@Disabled
	public void insertIntoDefaultGraphAndQueryTest() {
		try (QuerySolutionStream stream = GraphUtils.doSelectQuery(sparqlUrl, EVERYTHING_QUERY)) {
			assertEquals(0, stream.count(), "Invalid precondition -- triple store is not empty.");
		}

		String triple1 = "<%1$s> <%2$s> <%3$s1> .".formatted(TEST_SUBJECT, RDF.type, TEST_CLASS);
		String query = "select * where { ?x a <%1$s1>. } ";

		GraphUtils.insertStatements(graphStoreUrl, triple1, RDFFormat.NTRIPLES, null);

		boolean foundIt = false;
		try (QuerySolutionStream stream = GraphUtils.doSelectQuery(sparqlUrl, query, TEST_CLASS)) {
			LOG.debug("insertIntoDefaultGraphAndQueryTest: Tracker.getInstance().getTrackableIDs():"+Tracker.getInstance().getTrackableIDs());
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
	@Disabled
	public void deleteNamedGraphTest() {
		try (QuerySolutionStream stream = GraphUtils.doSelectQuery(sparqlUrl, EVERYTHING_QUERY)) {
			assertEquals(0, stream.count(), "Invalid precondition -- triple store is not empty.");
		}
		String graph1Uri = "http://example.org/foo/bar/#Graph1";
//		String graph2Uri = "http://example.org/foo/bar/#Graph2";
		String triple1 = "<%1$s> <%2$s> <%3$s1> .".formatted(TEST_SUBJECT, RDF.type, TEST_CLASS);
		String query = "select * where { graph ?g { <%1$s> a <%2$s1>. } }";
//		GraphUtils.createGraph(graphStoreUrl, RDFFormat.NTRIPLES.getMediaType(), URLEncoder.encode(graph1Uri, StandardCharsets.UTF_8));
		GraphUtils.insertStatements(graphStoreUrl, triple1, RDFFormat.NTRIPLES, URLEncoder.encode(graph1Uri, StandardCharsets.UTF_8));

		boolean foundIt = false;
		try (QuerySolutionStream stream = GraphUtils.doSelectQuery(sparqlUrl, query, TEST_SUBJECT, TEST_CLASS)) {
			LOG.debug("deleteNamedGraphTest: Tracker.getInstance().getTrackableIDs():"+Tracker.getInstance().getTrackableIDs());
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

//TODO: create negative tests:
//
//	@Test
//	public void deleteErrorTest() {
//		boolean caughtException = false;
//		try {
//			// Invalid n-triples:
//			GraphUtils.delete(updateUrl, "oogetyboogetyboo!", null, null, null);
//		} catch (Exception ex) {
//			caughtException = true;
//			LOG.info("N-triples parse error (delete)", ex);
//		}
//		assertTrue(caughtException);
//	}

	@Test
	@Disabled
	public void deleteNgErrorTest() {
		String graph1Uri = "http://example.org/foo/bar/#Graph1";
		String query = "select * where { graph ?g { <%1$s> a <%2$s1>. } }";
		GraphUtils.deleteGraph(graphStoreUrl, graph1Uri);

//		try (QuerySolutionStream stream = GraphUtils.doSelectQuery(sparqlUrl, query, TEST_SUBJECT, TEST_CLASS)) {
//			assertEquals(0, stream.count(), "Invalid postcondition -- triple store is not empty.");
//		}
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
LOG.debug("after loadRdf()");
		return GraphUtils.sendRequest(request).statusCode();
	}

}
