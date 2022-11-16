package com.bbn.parliament.spring_boot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.modify.request.QuadDataAcc;
import org.apache.jena.sparql.modify.request.UpdateDataDelete;
import org.apache.jena.sparql.modify.request.UpdateDataInsert;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
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
import org.springframework.web.reactive.function.client.WebClient;

import com.bbn.parliament.jena.bridge.tracker.Tracker;
import com.bbn.parliament.jena.joseki.client.QuerySolutionStream;
import com.bbn.parliament.jena.joseki.client.RDFFormat;
import com.bbn.parliament.spring_boot.controller.QueryController;
import com.bbn.parliament.test_util.RdfResourceLoader;

import reactor.core.publisher.Mono;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(Lifecycle.PER_CLASS)
public class ParliamentServerTests {
	private static final String HOST = "localhost";
	private static final String[] RSRCS_TO_LOAD = { "univ-bench.owl", "University15_20.owl.zip" };
	private static final String CSV_QUOTE_TEST_INPUT = "csv-quote-test-input.ttl";
	private static final String CSV_QUOTE_TEST_EXPECTED_RESULT = "csv-quote-test-expected-result.csv";
	private static final String TEST_SUBJECT = "http://example.org/#TestItem";
	private static final String TEST_CLASS = "http://example.org/#TestClass";
	private static final String TEST_LITERAL = "TestLiteral";
	private static final Logger LOG = LoggerFactory.getLogger(ParliamentServerTests.class);

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
	private static final String THING_QUERY = """
		prefix owl:  <http://www.w3.org/2002/07/owl#>
		prefix ex:   <http://www.example.org/>
		select distinct ?a where {
			bind ( ex:TestItem as ?a )
			?a a owl:Thing .
		}
		""";
	private static final String THING_INSERT = """
		prefix owl:  <http://www.w3.org/2002/07/owl#>
		prefix ex:   <http://www.example.org/>
		insert data {
			ex:TestItem a owl:Thing .
		}
		""";
	private static final String THING_DELETE = """
		prefix owl:  <http://www.w3.org/2002/07/owl#>
		prefix ex:   <http://www.example.org/>
		delete data {
			ex:TestItem a owl:Thing .
		}
		""";
	private static final String CSV_QUOTING_TEST_QUERY = """
		prefix ex: <http://example.org/#>
		select distinct ?s ?p ?o where {
			bind( ex:comment as ?p )
			?s ?p ?o .
		} order by ?o
		""";
	private static final String NG_QUERY = """
		select distinct ?g where {
			graph ?g {}
		}
		""";

	@LocalServerPort
	private int serverPort;

	private String sparqlUrl;
	private String updateUrl;
	private String graphStoreUrl;

	@BeforeEach
	public void beforeEach() {
		sparqlUrl = "http://%1$s:%2$s/parliament/sparql".formatted(HOST, serverPort);
		updateUrl = "http://%1$s:%2$s/parliament/update".formatted(HOST, serverPort);
		graphStoreUrl = "http://%1$s:%2$s/parliament/graphstore".formatted(HOST, serverPort);
	}

	@Test
	public void generalKBFunctionalityTest() throws IOException {
		clearAll();

		try (QuerySolutionStream stream = doSelectQuery(EVERYTHING_QUERY)) {
			long count = stream.count();
			assertEquals(0, count, "Invalid precondition -- triple store is not empty.");
		}

		loadSampleData();

		try (QuerySolutionStream stream = doSelectQuery(CLASS_QUERY)) {
			long count = stream.count();
			assertEquals(43, count);
			assertEquals(0, Tracker.getInstance().getTrackableIDs().size());
		}

		try (QuerySolutionStream stream = doSelectQuery(THING_QUERY)) {
			long count = stream.count();
			assertEquals(0, count, "Invalid precondition -- triple store already contains data.");
		}

		doUpdate(THING_INSERT);

		try (QuerySolutionStream stream = doSelectQuery(THING_QUERY)) {
			long count = stream.count();
			assertEquals(1, count, "Data insert failed.");
		}

		doUpdate(THING_DELETE);

		try (QuerySolutionStream stream = doSelectQuery(THING_QUERY)) {
			long count = stream.count();
			assertEquals(0, count, "Data delete failed.");
			assertEquals(0, Tracker.getInstance().getTrackableIDs().size());
		}

		clearAll();

		try (QuerySolutionStream stream = doSelectQuery(CLASS_QUERY)) {
			long count = stream.count();
			assertEquals(0, count, "Invalid postcondition -- triple store is not empty.");
		}
	}

	@Test
	public void namedGraphsTest() {
		String graphUri = "http://example.org/foo/bar/#Graph1";

		assertTrue(getAvailableNamedGraphs().isEmpty());

		doUpdate("create graph <%1$s>", graphUri);
		assertTrue(getAvailableNamedGraphs().equals(Collections.singleton(graphUri)));

		doUpdate("drop silent graph <%1$s>", graphUri);
		assertTrue(getAvailableNamedGraphs().isEmpty());
	}

	private Set<String> getAvailableNamedGraphs() {
		try (QuerySolutionStream stream = doSelectQuery(NG_QUERY)) {
			return stream
				.map(qs -> qs.getResource("g"))
				.map(Resource::getURI)
				.filter(uri -> !"http://parliament.semwebcentral.org/parliament#MasterGraph".equals(uri))
				.collect(Collectors.toSet());
		}
	}

	private String getAvailableNamedGraphsNoJena() {
		return WebClient.create(sparqlUrl)
			.post()
			.uri("")
			.bodyValue(NG_QUERY)
			.header(HttpHeaders.CONTENT_TYPE, QueryController.SPARQL_QUERY)
			.accept(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML)
			.acceptCharset(StandardCharsets.UTF_8)
			.exchangeToMono(response -> {
				System.out.format("HTTP response status code: %1$d%n", response.rawStatusCode());
				System.out.format("HTTP response headers: %1$s%n", response.headers().asHttpHeaders());
				if (response.statusCode().equals(HttpStatus.OK)) {
					return response.bodyToMono(String.class);
				} else if (response.statusCode().is4xxClientError()) {
					return Mono.just("Error response");
				} else {
					return response.createException().flatMap(Mono::error);
				}
			})
			.block()
			.toString();
	}

	@Test
	public void insertAndQueryTest() {
		insert(TEST_SUBJECT, RDF.type.getURI(), NodeFactory.createURI(TEST_CLASS), null);
		insert(TEST_SUBJECT, RDFS.label.getURI(), NodeFactory.createLiteral(TEST_LITERAL), null);

		String query = "select * where { ?thing a <%1$s> ; <%2$s> ?label . }";
		try (QuerySolutionStream stream = doSelectQuery(query, TEST_CLASS, RDFS.label)) {
			long count = stream
				.map(qs -> Pair.of(qs.getResource("thing"), qs.getLiteral("label")))
				.filter(pair -> TEST_SUBJECT.equals(pair.getLeft().getURI())
					&& TEST_LITERAL.equals(pair.getRight().getLexicalForm())
					&& isStringLiteral(pair.getRight().getDatatypeURI()))
				.count();
			assertTrue(count > 0);
		}
		// delete inserted statements bc deleteAndQueryTest checks
		delete(TEST_SUBJECT, RDF.type.getURI(), NodeFactory.createLiteral(TEST_CLASS), null);
		delete(TEST_SUBJECT, RDFS.label.getURI(), NodeFactory.createLiteral(TEST_LITERAL), null);
	}

	private static boolean isStringLiteral(String datatypeUri) {
		return datatypeUri == null
			|| datatypeUri.isEmpty()
			|| datatypeUri.equals(XSDDatatype.XSDstring.getURI());
	}

	@Test
	public void deleteAndQueryTest() {
		String queryFmt = "ask where { <%1$s> <%2$s> \"%3$s\" . }";

		assertFalse(doAskQuery(queryFmt, TEST_SUBJECT, RDFS.label, TEST_LITERAL));
		insert(TEST_SUBJECT, RDFS.label.getURI(), NodeFactory.createLiteral(TEST_LITERAL), null);
		assertTrue(doAskQuery(queryFmt, TEST_SUBJECT, RDFS.label, TEST_LITERAL));
		delete(TEST_SUBJECT, RDFS.label.getURI(), NodeFactory.createLiteral(TEST_LITERAL), null);
		assertFalse(doAskQuery(queryFmt, TEST_SUBJECT, RDFS.label, TEST_LITERAL));
	}

	@Test
	public void simpleSPARQLUpdateTest() {
		String d = "http://example.org/doughnut";
		String y = "http://example.org/yummy";
		String queryFmt = "ask where { <%1$s> a <%2$s> }";

		insert(d, RDF.type.getURI(), NodeFactory.createURI(y), null);
		assertTrue(doAskQuery(queryFmt, d, y));

		delete(d, RDF.type.getURI(), NodeFactory.createURI(y), null);
		assertFalse(doAskQuery(queryFmt, d, y));
	}

	@Test
	public void ngSparqlUpdateTest()
	{
		String graphUri = "http://example.org/foo/bar/#Graph2";
		String bs = "http://example.org/brusselsprouts";
		String y = "http://example.org/yucky";
		String query = "select * where { graph <%1$s> {?thing a <%2$s> } }";

		doUpdate("create graph <%1$s>", graphUri);
		insert(bs, RDF.type.getURI(), NodeFactory.createURI(y), graphUri);

		boolean foundIt = false;
		try (QuerySolutionStream stream = doSelectQuery(query, graphUri, y)) {
			foundIt = stream
					.map(qs -> qs.getResource("thing"))
					.map(Resource::getURI)
					.filter(uri -> bs.equals(uri))
					.count() == 1;
			assertTrue(foundIt);
		}

		delete(bs, RDF.type.getURI(), NodeFactory.createURI(y), graphUri);
		foundIt = false;
		try (QuerySolutionStream stream = doSelectQuery(query, graphUri, y)) {
			foundIt = stream
					.map(qs -> qs.getResource("thing"))
					.map(Resource::getURI)
					.filter(uri -> bs.equals(uri))
					.count() == 1;
			assertFalse(foundIt);
		}

		doUpdate("drop graph <%1$s>", graphUri);
	}

	@Test
	public void queryErrorTest() {
		String invalidQuery = "select * where { ?thing oogetyboogetyboo! }";
		boolean caughtException = false;
		try (QuerySolutionStream stream = doSelectQuery(invalidQuery)) {
			@SuppressWarnings("unused")
			long count = stream.count();
		} catch (Exception ex) {
			caughtException = true;
			LOG.info("Query parse error", ex);
		}
		assertTrue(caughtException);
	}

	@Test
	public void insertErrorTest() {
		boolean caughtException = false;
		try {
			// Invalid n-triples:
			insert("oogetyboogetyboo!", null, null, null);
		} catch (Exception ex) {
			caughtException = true;
			LOG.info("N-triples parse error (insert)", ex);
		}
		assertTrue(caughtException);
	}

	@Test
	public void deleteErrorTest() {
		boolean caughtException = false;
		try {
			// Invalid n-triples:
			delete("oogetyboogetyboo!", null, null, null);
		} catch (Exception ex) {
			caughtException = true;
			LOG.info("N-triples parse error (delete)", ex);
		}
		assertTrue(caughtException);
	}

	@Test
	public void insertQueryNamedGraphTest() {
		String graphUri = "http://example.org/foo/bar/#Graph3";
		String query = "select * where { ?x a <%1$s> . graph <%2$s> { ?x a <%1$s> } }";

		doUpdate("create graph <%1$s>", graphUri);

		insert(TEST_SUBJECT, RDF.type.getURI(), NodeFactory.createURI(TEST_CLASS), null);
		insert(TEST_SUBJECT, RDF.type.getURI(), NodeFactory.createURI(TEST_CLASS), graphUri);

		boolean foundIt = false;

		try (QuerySolutionStream stream = doSelectQuery(query, TEST_CLASS, graphUri)) {
			foundIt = stream
					.map(qs -> qs.getResource("x"))
					.map(Resource::getURI)
					.filter(uri -> TEST_SUBJECT.equals(uri))
					.count() == 1;
			assertTrue(foundIt);
		}

		doUpdate("drop graph <%1$s>", graphUri);
	}

	@Test
	public void namedGraphUnionTest() {
		String graph1Uri = "http://example.org/foo/bar/#Graph4";
		String graph2Uri = "http://example.org/foo/bar/#Graph5";
		String unionGraphUri = "http://example.org/foo/bar/#UnionGraph";
		String triple1 = "<%1$s> <%2$s> <%3$s1> .".formatted(TEST_SUBJECT, RDF.type, TEST_CLASS);
		String triple2 = "<%1$s> <%2$s> <%3$s2> .".formatted(TEST_SUBJECT, RDF.type, TEST_CLASS);
		String query = "select * where { graph <%1$s> { ?x a <%2$s1> , <%2$s2> . } }";

		doUpdate("create graph <%1$s>", graph1Uri);
		try {
			insertStatements(triple1, RDFFormat.NTRIPLES, URLEncoder.encode(graph1Uri, StandardCharsets.UTF_8.toString()));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		doUpdate("create graph <%1$s>", graph2Uri);
		try {
			insertStatements(triple2, RDFFormat.NTRIPLES, URLEncoder.encode(graph2Uri, StandardCharsets.UTF_8.toString()));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		doUpdate("""
			prefix parPF: <java:com.bbn.parliament.jena.pfunction.>
			prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
			prefix ex: <http://example.org/#>
			INSERT { } WHERE
			  { <%1$s>      parPF:createUnionGraph  ex:0 .
			    ex:0       rdf:first  <%2$s> ;
			              rdf:rest ex:1 .
			    ex:1       rdf:first  <%3$s> ;
			              rdf:rest  rdf:nil
			  }

			""", unionGraphUri, graph1Uri, graph2Uri);

		boolean foundIt = false;
		try (QuerySolutionStream stream = doSelectQuery(query, unionGraphUri, TEST_CLASS, TEST_CLASS)) {
			foundIt = stream
					.map(qs -> qs.getResource("x"))
					.map(Resource::getURI)
					.filter(uri -> TEST_SUBJECT.equals(uri))
					.count() == 1;
			assertTrue(foundIt);
		}

		doUpdate("drop graph <%1$s>", unionGraphUri);
		doUpdate("drop graph <%1$s>", graph1Uri);
		doUpdate("drop graph <%1$s>", graph2Uri);
	}

	@Test
	public void constructQueryTest() throws IOException {
		Resource testSubject = ResourceFactory.createResource(TEST_SUBJECT);
		Model testModel = ModelFactory.createDefaultModel();
		testModel.add(testSubject, RDFS.range, "sdfjklsdfj");
		testModel.add(testSubject, RDFS.seeAlso, "klsdfj");
		String query = "construct { ?s ?p ?o } where { ?s ?p ?o }";

		ByteArrayOutputStream os = new ByteArrayOutputStream();
		testModel.write(os, "N-TRIPLE", null);
		insertStatements(os.toString(), RDFFormat.NTRIPLES, null);

		Model resultModel = doConstructQuery(query);
		assertTrue(testModel.difference(resultModel).isEmpty());
	}

	@Test
	@Disabled
	public void csvQuotingTest() throws IOException {
		try (InputStream is = getClass().getClassLoader().getResourceAsStream(CSV_QUOTE_TEST_INPUT)) {
			if (is == null) {
				fail("Unable to find resource '%1$s'".formatted(CSV_QUOTE_TEST_INPUT));
			}
			LOG.debug("rdf format: "+RDFFormat.parseFilename(CSV_QUOTE_TEST_INPUT));
			insertStatements(is, RDFFormat.parseFilename(CSV_QUOTE_TEST_INPUT), null);
		}

		String actualResponse = doSelectToCsv(CSV_QUOTING_TEST_QUERY);
		Reader actualRdr = new StringReader(actualResponse);

		String expectedResponse;
		try (InputStream is = getClass().getClassLoader().getResourceAsStream(CSV_QUOTE_TEST_EXPECTED_RESULT)) {
			expectedResponse = readStreamToEnd(is, "Unable to find resource '%1$s'", CSV_QUOTE_TEST_EXPECTED_RESULT);
		}
		Reader expectedRdr = new StringReader(expectedResponse);
		List<CSVRecord> expectedRecords;
		try (CSVParser parser = CSVFormat.EXCEL.parse(expectedRdr)) {
			expectedRecords = parser.getRecords();
		}

		try (CSVParser parser = CSVFormat.EXCEL.parse(actualRdr)) {
			List<CSVRecord> actualRecords = parser.getRecords();
			for (int i=0; i<actualRecords.size(); i++) {
				String actual = actualRecords.get(i).get(2);
				String expected = expectedRecords.get(i).get(2);
				if (!actual.equals(expected)) {
					// TODO: double quote is not being escaped..
					LOG.debug("actual:\n"+actual);
					LOG.debug("expected:\n"+expected);
				}
				assertEquals(expected, actual);
			}
		}

		clearAll();
	}


	private static String readStreamToEnd(InputStream is, String errorMsg, Object... args) throws IOException {
		if (is == null) {
			fail(errorMsg.formatted(args));
		}
		try (
			Reader rdr = new InputStreamReader(is, StandardCharsets.UTF_8);
			BufferedReader brdr = new BufferedReader(rdr);
		) {
			return brdr.lines().collect(Collectors.joining(System.lineSeparator()));
		}
	}

	private QuerySolutionStream doSelectQuery(String queryFmt, Object... args) {
		return new QuerySolutionStream(queryFmt.formatted(args), sparqlUrl);
	}

	private boolean doAskQuery(String queryFmt, Object... args) {
		String query = queryFmt.formatted(args);
		LOG.debug("askquery: {}", query);
		try (var qe = QueryExecutionFactory.sparqlService(sparqlUrl, query)) {
			return qe.execAsk();
		}
	}

	public Model doConstructQuery(String queryFmt, Object... args) throws IOException {
		String query = queryFmt.formatted(args);
		try (var qe = QueryExecutionFactory.sparqlService(sparqlUrl, query)) {
			return qe.execConstruct();
		}
	}

	private void doUpdate(String queryFmt, Object... args) {
		UpdateRequest ur = UpdateFactory.create(queryFmt.formatted(args));
		UpdateExecutionFactory.createRemote(ur, updateUrl).execute();
	}

	private void insert(String sub, String pred, Node obj, String graphName) {
		QuadDataAcc qd = createQuadData(sub, pred, obj, graphName);
		UpdateDataInsert update = new UpdateDataInsert(qd);
		UpdateExecutionFactory.createRemote(update, updateUrl).execute();
	}

	private int insertStatements(String stmt, RDFFormat format, String graphUri) {
		ByteArrayInputStream bstrm = new ByteArrayInputStream(stmt.getBytes(StandardCharsets.UTF_8));
		return loadRdf(bstrm, format.getMediaType(), graphUri);
	}

	private int insertStatements(InputStream in, RDFFormat format, String graphUri) {
		return loadRdf(in, format.getMediaType(), graphUri);
	}

	private int loadRdf(InputStream in, String mediaType, String graphUri) {
		if (graphUri != null )
			graphUri = "?graph="+graphUri;
		else
			graphUri = "?default";
		var request = HttpRequest.newBuilder()
					.uri(URI.create(graphStoreUrl + graphUri))
					.POST(HttpRequest.BodyPublishers.ofInputStream(() -> in))
					.header(HttpHeaders.CONTENT_TYPE, mediaType)
					.header(HttpHeaders.ACCEPT, MediaType.ALL_VALUE)
					.header(HttpHeaders.ACCEPT_CHARSET, StandardCharsets.UTF_8.name())
					.build();
		return sendRequest(request).statusCode();
	}

	private String doSelectToCsv(String query) {
		var request = HttpRequest.newBuilder()
					.uri(URI.create(sparqlUrl))
					.POST(HttpRequest.BodyPublishers.ofString(query))
					.header(HttpHeaders.CONTENT_TYPE, "application/sparql-query")
					.header(HttpHeaders.ACCEPT, "text/csv")
					.header(HttpHeaders.ACCEPT_CHARSET, StandardCharsets.UTF_8.name())
					.build();
		return sendRequest(request).body();
	}

	private static HttpResponse<String> sendRequest(HttpRequest request) {
		HttpResponse<String> response = HttpClient
									.newHttpClient()
									.sendAsync(request, HttpResponse.BodyHandlers.ofString())
									.join();
		LOG.info("Response body:%n%1$s%n".formatted(response.body()));
		return response;
	}

	private void clearAll() {
		Set<String> allGraphs = getAvailableNamedGraphs();
		allGraphs.add("?default");
		for (String uri : allGraphs) {
			if (uri != "?default")
				uri = "?graph=" + uri;
			var request = HttpRequest.newBuilder()
					.uri(URI.create(graphStoreUrl + uri))
					.DELETE()
					.header(HttpHeaders.ACCEPT, MediaType.ALL_VALUE)
					.header(HttpHeaders.ACCEPT_CHARSET, StandardCharsets.UTF_8.name())
					.build();
			sendRequest(request);
		}
	}

	private void delete(String sub, String pred, Node obj, String graphName) {
		QuadDataAcc qd = createQuadData(sub, pred, obj, graphName);
		UpdateDataDelete update = new UpdateDataDelete(qd);
		UpdateExecutionFactory.createRemote(update, updateUrl).execute();
	}

	private static QuadDataAcc createQuadData(String sub, String pred, Node obj, String graphName) {
		Node s = NodeFactory.createURI(sub);
		Node p = NodeFactory.createURI(pred);
		QuadDataAcc qd = new QuadDataAcc();
		if (StringUtils.isBlank(graphName)) {
			qd.addTriple(new Triple(s, p, obj));
		} else {
			qd.addQuad(new Quad(NodeFactory.createURI(graphName), s, p, obj));
		}
		return qd;
	}

	private void loadSampleData() {
		try {
			for (String rsrcName : RSRCS_TO_LOAD) {
				Model clientSideModel = ModelFactory.createDefaultModel();
				RdfResourceLoader.load(rsrcName, clientSideModel);

				QuadDataAcc qd = new QuadDataAcc();
				StmtIterator it = clientSideModel.listStatements();
				while (it.hasNext()) {
					Statement stmt = it.next();
					qd.addTriple(stmt.asTriple());
				}
				UpdateDataInsert update = new UpdateDataInsert(qd);
				UpdateExecutionFactory.createRemote(update, updateUrl).execute();
			}
		} catch (Exception ex) {
			fail(ex.getMessage());
		}
	}

}
