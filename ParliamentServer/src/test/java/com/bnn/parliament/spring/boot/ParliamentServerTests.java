package com.bnn.parliament.spring.boot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.bbn.parliament.jena.bridge.tracker.Tracker;
import com.bbn.parliament.jena.joseki.client.CloseableQueryExec;
import com.bbn.parliament.jena.joseki.client.QuerySolutionStream;
import com.bbn.parliament.jena.joseki.client.RDFFormat;
import com.bbn.parliament.jena.joseki.client.RemoteModel;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.sparql.modify.request.QuadDataAcc;
import com.hp.hpl.jena.sparql.modify.request.UpdateDataDelete;
import com.hp.hpl.jena.sparql.modify.request.UpdateDataInsert;
import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateProcessor;
import com.hp.hpl.jena.update.UpdateRequest;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

import test_util.RdfResourceLoader;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(Lifecycle.PER_CLASS)
@Disabled
public class ParliamentServerTests {
	private static final String HOST = "localhost";
	private static final String[] RSRCS_TO_LOAD = { "univ-bench.owl", "University15_20.owl.zip" };
	private static final String CSV_QUOTE_TEST_INPUT = "csv-quote-test-input.ttl";
	private static final String CSV_QUOTE_TEST_EXPECTED_RESULT = "csv-quote-test-expected-result.csv";
	private static final String TEST_SUBJECT = "http://example.org/#Test";
	private static final String TEST_CLASS = "http://example.org/#TestClass";
	private static final String TEST_LITERAL = "Test";
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
			bind ( ex:Test as ?a )
			?a a owl:Thing .
		}
		""";
	private static final String THING_INSERT = """
		prefix owl:  <http://www.w3.org/2002/07/owl#>
		prefix ex:   <http://www.example.org/>
		insert data {
			ex:Test a owl:Thing .
		}
		""";
	private static final String THING_DELETE = """
		prefix owl:  <http://www.w3.org/2002/07/owl#>
		prefix ex:   <http://www.example.org/>
		delete data {
			ex:Test a owl:Thing .
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
			graph ?g { }
		}
		""";

	@LocalServerPort
	private int serverPort;

	private String sparqlUrl;
	private String bulkUrl;
	private RemoteModel rm;

	@BeforeAll
	public static void beforeAll() {
		//ParliamentTestServer.createServer();
	}

	@AfterAll
	public static void afterAll() {
		//ParliamentTestServer.stopServer();
	}

	@BeforeEach
	public void beforeEach() {
		sparqlUrl = RemoteModel.DEFAULT_SPARQL_ENDPOINT_URL.formatted(HOST, serverPort);
		bulkUrl = RemoteModel.DEFAULT_BULK_ENDPOINT_URL.formatted(HOST, serverPort);
		rm = new RemoteModel(sparqlUrl, bulkUrl);
	}

	@AfterEach
	public void afterEach() {
	}

	@Test
	public void generalKBFunctionalityTest() throws IOException {
		rm.clearAll();

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

		rm.clearAll();

		try (QuerySolutionStream stream = doSelectQuery(CLASS_QUERY)) {
			long count = stream.count();
			assertEquals(0, count, "Invalid postcondition -- triple store is not empty.");
		}
	}

	@Test
	public void remoteModelGetNamedGraphsTest() throws IOException {
		String graphUri = "http://example.org/foo/bar/#Graph1";

		rm.createNamedGraph(graphUri);
		assertTrue(getAvailableNamedGraphs().equals(Collections.singleton(graphUri)));

		rm.dropNamedGraph(graphUri);
		assertTrue(getAvailableNamedGraphs().isEmpty());
	}

	private Set<String> getAvailableNamedGraphs() {
		try (QuerySolutionStream stream = doSelectQuery(NG_QUERY)) {
			return stream
				.map(qs -> qs.getResource("g"))
				.map(Resource::getURI)
				.collect(Collectors.toUnmodifiableSet());
		}
	}

	@Test
	public void insertAndQueryTest() {
		insert(TEST_SUBJECT, RDF.type.getURI(), Node.createURI(TEST_CLASS), null);
		insert(TEST_SUBJECT, RDFS.label.getURI(), Node.createLiteral(TEST_LITERAL), null);

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
		insert(TEST_SUBJECT, RDFS.label.getURI(), Node.createLiteral(TEST_LITERAL), null);
		assertTrue(doAskQuery(queryFmt, TEST_SUBJECT, RDFS.label, TEST_LITERAL));
		delete(TEST_SUBJECT, RDFS.label.getURI(), Node.createLiteral(TEST_LITERAL), null);
		assertFalse(doAskQuery(queryFmt, TEST_SUBJECT, RDFS.label, TEST_LITERAL));
	}

	@Test
	public void remoteModelSimpleSPARQLUpdateTest() {
		String d = "http://example.org/doughnut";
		String y = "http://example.org/yummy";
		String queryFmt = "ask where { <%1$s> a <%2$s> }";

		insert(d, RDF.type.getURI(), Node.createURI(y), null);
		assertTrue(doAskQuery(queryFmt, d, y));

		delete(d, RDF.type.getURI(), Node.createURI(y), null);
		assertFalse(doAskQuery(queryFmt, d, y));
	}

	@Test
	public void remoteModelNGSPARQLUpdateTest() throws IOException
	{
		String graphUri = "http://example.org/foo/bar/#Graph2";
		String bs = "http://example.org/brusselsprouts";
		String y = "http://example.org/yucky";
		String updateQuery = String.format("%%1$s <%1$s> { <%2$s> a <%3$s> . }",
			graphUri, bs, y);
		String query = String.format("select * where { graph <%1$s> {?thing a <%2$s> } }",
			graphUri, y);

		rm.createNamedGraph(graphUri);

		rm.updateQuery(String.format(updateQuery, "insert data into"));

		ResultSet rs = rm.selectQuery(query);
		boolean foundIt = false;
		while (rs.hasNext()) {
			QuerySolution qs = rs.nextSolution();
			if (bs.equals(qs.getResource("thing").getURI())) {
				foundIt = true;
			}
		}
		assertTrue(foundIt);

		rm.updateQuery(String.format(updateQuery, "delete data from"));

		rs = rm.selectQuery(query);
		foundIt = false;
		while (rs.hasNext()) {
			QuerySolution qs = rs.nextSolution();
			if (bs.equals(qs.getResource("thing").getURI())) {
				foundIt = true;
			}
		}
		assertFalse(foundIt);

		rm.dropNamedGraph(graphUri);
	}

	@Test
	public void remoteModelQueryErrorTest() {
		String invalidQuery = "select * where { ?thing oogetyboogetyboo! }";
		boolean caughtException = false;
		try {
			rm.selectQuery(invalidQuery);
		} catch (Exception ex) {
			caughtException = true;
			LOG.info("Query parse error", ex);
		}
		assertTrue(caughtException);
	}

	@Test
	public void remoteModelInsertErrorTest() {
		boolean caughtException = false;
		try {
			// Invalid n-triples:
			rm.insertStatements("oogetyboogetyboo!", RDFFormat.NTRIPLES, null, true);
		} catch (Exception ex) {
			caughtException = true;
			LOG.info("N-triples parse error (insert)", ex);
		}
		assertTrue(caughtException);
	}

	@Test
	public void remoteModelDeleteErrorTest() {
		boolean caughtException = false;
		try {
			// Invalid n-triples:
			rm.deleteStatements("oogetyboogetyboo!", RDFFormat.NTRIPLES);
		} catch (Exception ex) {
			caughtException = true;
			LOG.info("N-triples parse error (delete)", ex);
		}
		assertTrue(caughtException);
	}

	@Test
	public void remoteModelInsertQueryNamedGraphTest() throws IOException {
		String graphUri = "http://example.org/foo/bar/#Graph3";
		String query = String.format(
			"select * where { ?x a <%1$s> . graph <%2$s> { ?x a <%1$s> } }",
			TEST_CLASS, graphUri);

		rm.createNamedGraph(graphUri);

		insert(TEST_SUBJECT, RDF.type.getURI(), Node.createURI(TEST_CLASS), null);
		insert(TEST_SUBJECT, RDF.type.getURI(), Node.createURI(TEST_CLASS), graphUri);

		ResultSet rs = rm.selectQuery(query);
		boolean foundIt = false;
		while (rs.hasNext()) {
			QuerySolution qs = rs.nextSolution();
			Resource value = qs.getResource("x");
			if (TEST_SUBJECT.equals(value.getURI())) {
				foundIt = true;
			}
		}
		assertTrue(foundIt);
	}

	@Test
	public void remoteModelNamedGraphUnionTest() throws IOException {
		String graph1Uri = "http://example.org/foo/bar/#Graph4";
		String graph2Uri = "http://example.org/foo/bar/#Graph5";
		String unionGraphUri = "http://example.org/foo/bar/#UnionGraph";
		String triple1 = String.format("<%1$s> <%2$s> <%3$s1> .",
			TEST_SUBJECT, RDF.type, TEST_CLASS);
		String triple2 = String.format("<%1$s> <%2$s> <%3$s2> .",
			TEST_SUBJECT, RDF.type, TEST_CLASS);
		String query = String.format(
			"select * where { graph <%1$s> { ?x a <%2$s1> , <%2$s2> . } }",
			unionGraphUri, TEST_CLASS);

		rm.createNamedGraph(graph1Uri);
		rm.insertStatements(triple1, RDFFormat.NTRIPLES, null, graph1Uri, true);

		rm.createNamedGraph(graph2Uri);
		rm.insertStatements(triple2, RDFFormat.NTRIPLES, null, graph2Uri, true);

		rm.createNamedUnionGraph(unionGraphUri, graph1Uri, graph2Uri);

		ResultSet rs = rm.selectQuery(query);
		boolean foundIt = false;
		while (rs.hasNext()) {
			QuerySolution qs = rs.nextSolution();
			Resource value = qs.getResource("x");
			if (TEST_SUBJECT.equals(value.getURI())) {
				foundIt = true;
			}
		}
		assertTrue(foundIt);
	}

	@Test
	public void remoteModelConstructQueryTest() throws IOException {
		Resource testSubject = ResourceFactory.createResource(TEST_SUBJECT);
		Model testModel = ModelFactory.createDefaultModel();
		testModel.add(testSubject, RDFS.range, "sdfjklsdfj");
		testModel.add(testSubject, RDFS.seeAlso, "klsdfj");
		rm.insertStatements(testModel);

		Model resultModel = rm.constructQuery("construct { ?s ?p ?o } where { ?s ?p ?o }");
		assertTrue(testModel.difference(resultModel).isEmpty());
	}

	@Test
	public void csvQuotingTest() throws IOException {
		try (InputStream is = getClass().getClassLoader().getResourceAsStream(CSV_QUOTE_TEST_INPUT)) {
			if (is == null) {
				fail(String.format("Unable to find resource '%1$s'", CSV_QUOTE_TEST_INPUT));
			}
			rm.insertStatements(is, RDFFormat.parseFilename(CSV_QUOTE_TEST_INPUT), null, true);
		}

		LOG.info("CSV quote test results:");
		try (CloseableQueryExec qe = new CloseableQueryExec(sparqlUrl, String.format(CSV_QUOTING_TEST_QUERY))) {
			ResultSet rs = qe.execSelect();
			while (rs.hasNext()) {
				QuerySolution qs = rs.next();
				Resource s = qs.getResource("s");
				Resource p = qs.getResource("p");
				Literal o = qs.getLiteral("o");
				String sStr = s.isAnon()
					? s.getId().getLabelString()
					: s.getURI();
				LOG.info("   {} {} \"{}\"", sStr, p.getURI(), o.getLexicalForm());
			}
		}

		String actualResponse;
		Map<String, Object> params = new HashMap<>();
		params.put("query", String.format(CSV_QUOTING_TEST_QUERY));
		params.put("stylesheet", "/xml-to-csv.xsl");
		try (InputStream is = rm.sendRequest(params)) {
			actualResponse = readStreamToEnd(is, "RemoteModel.sendRequest() returned null");
		}
		LOG.info("CSV quote result as CSV:{}{}", System.lineSeparator(), actualResponse);

		String expectedResponse;
		try (InputStream is = getClass().getResourceAsStream(CSV_QUOTE_TEST_EXPECTED_RESULT)) {
			expectedResponse = readStreamToEnd(is, "Unable to find resource '%1$s'", CSV_QUOTE_TEST_EXPECTED_RESULT);
		}

		assertEquals(expectedResponse, actualResponse);
	}

	private static String readStreamToEnd(InputStream is, String errorMsg, Object... args) throws IOException {
		if (is == null) {
			fail(String.format(errorMsg, args));
		}
		try (
			Reader rdr = new InputStreamReader(is, StandardCharsets.UTF_8);
			BufferedReader brdr = new BufferedReader(rdr);
		) {
			return brdr.lines().collect(Collectors.joining(System.lineSeparator()));
		}
	}

	private QuerySolutionStream doSelectQuery(String queryFmt, Object... args) {
		return new QuerySolutionStream(String.format(queryFmt, args), sparqlUrl);
	}

	private boolean doAskQuery(String queryFmt, Object... args) {
		String query = String.format(queryFmt, args);
		try (CloseableQueryExec qe = new CloseableQueryExec(sparqlUrl, query)) {
			return qe.execAsk();
		}
	}

	private void insert(String sub, String pred, Node obj, String graphName) {
		QuadDataAcc qd = createQuadData(sub, pred, obj, graphName);
		UpdateDataInsert update = new UpdateDataInsert(qd);
		UpdateExecutionFactory.createRemote(update, sparqlUrl).execute();
	}

	private void delete(String sub, String pred, Node obj, String graphName) {
		QuadDataAcc qd = createQuadData(sub, pred, obj, graphName);
		UpdateDataDelete update = new UpdateDataDelete(qd);
		UpdateExecutionFactory.createRemote(update, sparqlUrl).execute();
	}

	private static QuadDataAcc createQuadData(String sub, String pred, Node obj, String graphName) {
		Node s = Node.createURI(sub);
		Node p = Node.createURI(pred);
		QuadDataAcc qd = new QuadDataAcc();
		if (graphName == null || graphName.isBlank()) {
			qd.addTriple(new Triple(s, p, obj));
		} else {
			qd.addQuad(new Quad(Node.createURI(graphName), s, p, obj));
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
				UpdateProcessor exec = UpdateExecutionFactory.createRemote(update, sparqlUrl);
				executeUpdate(exec);
			}
		} catch (Exception ex) {
			fail(ex.getMessage());
		}
	}

	private void doUpdate(String queryFmt, Object... args) {
		UpdateRequest ur = UpdateFactory.create(String.format(queryFmt, args));
		UpdateProcessor exec = UpdateExecutionFactory.createRemote(ur, sparqlUrl);
		executeUpdate(exec);
	}

	private static void executeUpdate(UpdateProcessor exec) {
		try {
			exec.execute();
		} catch (NullPointerException ex) {
			StackTraceElement ste = ex.getStackTrace()[0];
			if ("org.openjena.riot.web.HttpOp".equals(ste.getClassName())
				&& "httpResponse".equals(ste.getMethodName())
				&& "HttpOp.java".equals(ste.getFileName())
				&& 345 == ste.getLineNumber()) {
				LOG.info("Encountered known bug in Jena 2.7.4/ARQ 2.9.4.  Ignoring NPE.");
			} else {
				throw new RuntimeException("Encountered NPE", ex);
			}
		}
	}
}
