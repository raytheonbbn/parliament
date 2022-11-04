package com.bbn.parliament.jena.joseki.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.sparql.modify.request.QuadDataAcc;
import org.apache.jena.sparql.modify.request.UpdateDataInsert;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jena.joseki.bridge.tracker.Tracker;
import com.bbn.parliament.jena.joseki.client.CloseableQueryExec;
import com.bbn.parliament.jena.joseki.client.HttpClientUtil;
import com.bbn.parliament.jena.joseki.client.RDFFormat;
import com.bbn.parliament.jena.joseki.client.RemoteModel;

import test_util.RdfResourceLoader;

public class ParliamentServerTestCase {
	private static final String HOST = "localhost";
	private static final String PORT = System.getProperty("jetty.port", "8586");
	private static final String SPARQL_URL = RemoteModel.DEFAULT_SPARQL_ENDPOINT_URL.formatted(HOST, PORT);
	private static final String BULK_URL = RemoteModel.DEFAULT_BULK_ENDPOINT_URL.formatted(HOST, PORT);
	private static final String[] RSRCS_TO_LOAD = { "univ-bench.owl", "University15_20.owl.zip" };
	private static final String CSV_QUOTE_TEST_INPUT = "csv-quote-test-input.ttl";
	private static final String CSV_QUOTE_TEST_EXPECTED_RESULT = "csv-quote-test-expected-result.csv";
	private static final String CSV_QUOTE_TEST_ACTUAL_CSV_RESULT = "../csv-quote-test-actual-result.csv";
	private static final String CSV_QUOTE_TEST_ACTUAL_XML_RESULT = "../csv-quote-test-actual-result.xml";
	private static final String TEST_SUBJECT = "http://example.org/#Test";
	private static final String TEST_CLASS = "http://example.org/#TestClass";
	private static final String TEST_LITERAL = "Test";
	private static final String PREFIXES = """
		prefix owl: <http://www.w3.org/2002/07/owl#>
		prefix ex:  <http://www.example.org/>
		""";
	private static final String EVERYTHING_QUERY = PREFIXES + """
		select ?s ?o ?p ?g where {
			{ ?s ?p ?o }
			union
			{ graph ?g { ?s ?p ?o } }
		}
		""";
	private static final String CLASS_QUERY = PREFIXES + """
		select distinct ?class where {
			?class a owl:Class .
			filter (!isblank(?class))
		}
		""";
	private static final String THING_QUERY = PREFIXES + """
		select ?a where {
			bind ( ex:Test as ?a )
			?a a owl:Thing .
		}
		""";
	private static final String THING_INSERT = PREFIXES + """
		insert data {
			ex:Test a owl:Thing .
		}
		""";
	private static final String THING_DELETE = PREFIXES + """
		delete data {
			ex:Test a owl:Thing .
		}
		""";
	private static final String CSV_QUOTING_TEST_QUERY = PREFIXES + """
		select ?s ?p ?o where {
			bind( ex:comment as ?p )
			?s ?p ?o .
		} order by ?o
		""";
	private static final RemoteModel rm = new RemoteModel(SPARQL_URL, BULK_URL);
	private static final Logger log = LoggerFactory.getLogger(ParliamentServerTestCase.class);

	@BeforeAll
	public static void beforeAll() {
		ParliamentTestServer.createServer();
	}

	@AfterAll
	public static void afterAll() {
		ParliamentTestServer.stopServer();
	}

	@SuppressWarnings("static-method")
	@Test
	public void generalKBFunctionalityTest() throws IOException {
		rm.clearAll();
		ResultSet rs = doQuery(EVERYTHING_QUERY);
		int count = countResults(rs);
		assertEquals(0, count, "Invalid precondition -- triple store is not empty.");

		loadSampleData();

		rs = doQuery(CLASS_QUERY);
		count = countResults(rs);
		assertEquals(43, count);
		assertEquals(0, Tracker.getInstance().getTrackableIDs().size());

		rs = doQuery(THING_QUERY);
		count = countResults(rs);
		assertEquals(0, count, "Invalid precondition -- triple store already contains data.");

		doUpdate(THING_INSERT);

		rs = doQuery(THING_QUERY);
		count = countResults(rs);
		assertEquals(1, count, "Data insert failed.");

		doUpdate(THING_DELETE);

		rs = doQuery(THING_QUERY);
		count = countResults(rs);
		assertEquals(0, count, "Data delete failed.");
		assertEquals(0, Tracker.getInstance().getTrackableIDs().size());

		rm.clearAll();
		rs = doQuery(CLASS_QUERY);
		count = countResults(rs);
		assertEquals(0, count);
	}

	@SuppressWarnings("static-method")
	@Test
	public void remoteModelGetNamedGraphsTest() throws IOException {
		String graphUri = "http://example.org/foo/bar/#Graph1";

		rm.createNamedGraph(graphUri);
		assertTrue(getAvailableNamedGraphs().contains(graphUri));

		rm.dropNamedGraph(graphUri);
		assertFalse(getAvailableNamedGraphs().contains(graphUri));
	}

	private static Set<String> getAvailableNamedGraphs() {
		Set<String> result = new HashSet<>();
		String q = "select distinct ?g where { graph ?g { } }";
		try (CloseableQueryExec qe = new CloseableQueryExec(SPARQL_URL, q)) {
			ResultSet rs = qe.execSelect();
			while (rs.hasNext()) {
				QuerySolution qs = rs.next();
				RDFNode node = qs.get("g");
				if (node != null && node.isURIResource()) {
					result.add(node.asResource().getURI());
				}
			}
		}
		return result;
	}

	@SuppressWarnings("static-method")
	@Test
	public void remoteModelInsertAndQueryTest() throws IOException {
		Resource testSubject = ResourceFactory.createResource(TEST_SUBJECT);
		Resource testClass = ResourceFactory.createResource(TEST_CLASS);
		Model testModel = ModelFactory.createDefaultModel();
		testModel.add(testSubject, RDF.type, testClass);
		rm.insertStatements(testModel);

		String triples = "<%1$s> <%2$s> \"%3$s\" ."
			.formatted(TEST_SUBJECT, RDFS.label, TEST_LITERAL);
		rm.insertStatements(triples, RDFFormat.NTRIPLES, null, true);

		String query = "select * where { ?thing a <%1$s> ; <%2$s> ?label . }"
			.formatted(TEST_CLASS, RDFS.label);
		ResultSet rs = rm.selectQuery(query);
		boolean foundIt = false;
		while (rs.hasNext()) {
			QuerySolution qs = rs.nextSolution();
			RDFNode t = qs.get("thing");
			RDFNode l = qs.get("label");
			if (t != null && l != null
				&& t.isURIResource() && TEST_SUBJECT.equals(t.asResource().getURI())
				&& l.isLiteral() && TEST_LITERAL.equals(l.asLiteral().getLexicalForm())
				&& isStringLiteral(l.asLiteral().getDatatypeURI())) {
				foundIt = true;
			}
		}
		assertTrue(foundIt);
	}

	private static boolean isStringLiteral(String datatypeUri) {
		return datatypeUri == null
			|| datatypeUri.isEmpty()
			|| datatypeUri.equals(XSDDatatype.XSDstring.getURI());
	}

	@SuppressWarnings("static-method")
	@Test
	public void remoteModelDeleteAndQueryTest() throws IOException {
		String stmt = "<http://example.org/foo> <%1$s> \"foo\" .".formatted(RDFS.label);
		String query = "ask where { %1$s }".formatted(stmt);

		assertFalse(rm.askQuery(query));
		rm.insertStatements(stmt, RDFFormat.NTRIPLES, null, true);
		assertTrue(rm.askQuery(query));
		rm.deleteStatements(stmt, RDFFormat.NTRIPLES);
		assertFalse(rm.askQuery(query));
	}

	@SuppressWarnings("static-method")
	@Test
	public void remoteModelSimpleSPARQLUpdateTest() throws IOException
	{
		String d = "http://example.org/doughnut";
		String y = "http://example.org/yummy";
		String update = "%%1$s data { <%1$s> a <%2$s> . }".formatted(d, y);
		String query = "select * where { ?thing a <%1$s> }".formatted(y);

		rm.updateQuery(update.formatted("insert"));

		ResultSet rs = rm.selectQuery(query);
		boolean foundIt = false;
		while (rs.hasNext()) {
			QuerySolution qs = rs.nextSolution();
			if (d.equals(qs.getResource("thing").getURI())) {
				foundIt = true;
			}
		}
		assertTrue(foundIt);

		rm.updateQuery(update.formatted("delete"));

		rs = rm.selectQuery(query);
		foundIt = false;
		while (rs.hasNext()) {
			QuerySolution qs = rs.nextSolution();
			if (d.equals(qs.getResource("thing").getURI())) {
				foundIt = true;
			}
		}
		assertTrue(!foundIt);
	}

	@SuppressWarnings("static-method")
	@Test
	public void remoteModelNGSPARQLUpdateTest() throws IOException
	{
		String graphUri = "http://example.org/foo/bar/#Graph2";
		String bs = "http://example.org/brusselsprouts";
		String y = "http://example.org/yucky";
		String updateQuery = "%%1$s <%1$s> { <%2$s> a <%3$s> . }"
			.formatted(graphUri, bs, y);
		String query = "select * where { graph <%1$s> {?thing a <%2$s> } }"
			.formatted(graphUri, y);

		rm.createNamedGraph(graphUri);

		rm.updateQuery(updateQuery.formatted("insert data into"));

		ResultSet rs = rm.selectQuery(query);
		boolean foundIt = false;
		while (rs.hasNext()) {
			QuerySolution qs = rs.nextSolution();
			if (bs.equals(qs.getResource("thing").getURI())) {
				foundIt = true;
			}
		}
		assertTrue(foundIt);

		rm.updateQuery(updateQuery.formatted("delete data from"));

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

	@SuppressWarnings("static-method")
	@Test
	public void remoteModelQueryErrorTest() {
		String query = "select * where { ?thing oogetyboogetyboo! }";	// invalid query
		boolean caughtException = false;
		try {
			rm.selectQuery(query);
		} catch (Exception ex) {
			caughtException = true;
			log.info("Exception type for query parse error:  {}", ex.getClass().getName());
		}
		assertTrue(caughtException);
	}

	@SuppressWarnings("static-method")
	@Test
	public void remoteModelInsertErrorTest() {
		boolean caughtException = false;
		try {
			// Invalid n-triples:
			rm.insertStatements("oogetyboogetyboo!", RDFFormat.NTRIPLES, null, true);
		} catch (Exception ex) {
			caughtException = true;
			log.info("Exception type for n-triples parse error (insert):  {}", ex.getClass().getName());
		}
		assertTrue(caughtException);
	}

	@SuppressWarnings("static-method")
	@Test
	public void remoteModelDeleteErrorTest() {
		boolean caughtException = false;
		try {
			// Invalid n-triples:
			rm.deleteStatements("oogetyboogetyboo!", RDFFormat.NTRIPLES);
		} catch (Exception ex) {
			caughtException = true;
			log.info("Exception type for n-triples parse error (delete):  {}", ex.getClass().getName());
		}
		assertTrue(caughtException);
	}

	@SuppressWarnings("static-method")
	@Test
	public void remoteModelInsertQueryNamedGraphTest() throws IOException {
		String graphUri = "http://example.org/foo/bar/#Graph3";
		String triples = "<%1$s> <%2$s> <%3$s> ."
			.formatted(TEST_SUBJECT, RDF.type, TEST_CLASS);
		String query = "select * where { ?x a <%1$s> . graph <%2$s> { ?x a <%1$s> } }"
			.formatted(TEST_CLASS, graphUri);

		rm.createNamedGraph(graphUri);

		rm.insertStatements(triples, RDFFormat.NTRIPLES, null, true);
		rm.insertStatements(triples, RDFFormat.NTRIPLES, null, graphUri, true);

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

	@SuppressWarnings("static-method")
	@Test
	public void remoteModelNamedGraphUnionTest() throws IOException {
		String graph1Uri = "http://example.org/foo/bar/#Graph4";
		String graph2Uri = "http://example.org/foo/bar/#Graph5";
		String unionGraphUri = "http://example.org/foo/bar/#UnionGraph";
		String triple1 = "<%1$s> <%2$s> <%3$s1> ."
			.formatted(TEST_SUBJECT, RDF.type, TEST_CLASS);
		String triple2 = "<%1$s> <%2$s> <%3$s2> ."
			.formatted(TEST_SUBJECT, RDF.type, TEST_CLASS);
		String query = "select * where { graph <%1$s> { ?x a <%2$s1> , <%2$s2> . } }"
			.formatted(unionGraphUri, TEST_CLASS);

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

	@SuppressWarnings("static-method")
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
		try (InputStream is = getResourceAsStream(CSV_QUOTE_TEST_INPUT)) {
			rm.insertStatements(is, RDFFormat.parseFilename(CSV_QUOTE_TEST_INPUT), null, true);
		}

		String actualResponse;
		Map<String, Object> params = new HashMap<>();
		params.put("query", CSV_QUOTING_TEST_QUERY);
		params.put("stylesheet", "/xml-to-csv.xsl");
		try (InputStream is = rm.sendRequest(params)) {
			actualResponse = readStreamToEnd(is);
		}

		String expectedResponse;
		try (InputStream is = getResourceAsStream(CSV_QUOTE_TEST_EXPECTED_RESULT)) {
			expectedResponse = readStreamToEnd(is);
		}

		// In case we are about to fail, record the XML result set to a file so we can diagnose:
		if (!Objects.equals(expectedResponse, actualResponse)) {
			Path actualCsvFile = new File(CSV_QUOTE_TEST_ACTUAL_CSV_RESULT).toPath();
			Files.writeString(actualCsvFile, actualResponse, StandardCharsets.UTF_8);

			params.remove("stylesheet");
			try (
				InputStream is = rm.sendRequest(params);
				OutputStream os = new FileOutputStream(CSV_QUOTE_TEST_ACTUAL_XML_RESULT);
			) {
				HttpClientUtil.transfer(is, os);
			}
			log.error("CSV quote actual result as XML written to {}", CSV_QUOTE_TEST_ACTUAL_XML_RESULT);
		}
		assertEquals(expectedResponse, actualResponse);
	}

	private InputStream getResourceAsStream(String resourceName) {
		InputStream result = getClass().getResourceAsStream(resourceName);
		if (result == null) {
			fail("Resource not found: '%1$s'".formatted(resourceName));
		}
		return result;
	}

	/**
	 * Reads stream to its end and returns the results as a string. All end-of-line
	 * situations are converted to the platform's native EOL format.
	 *
	 * @param is The stream to read
	 * @return The stream contents as a string
	 * @throws IOException on error
	 */
	private static String readStreamToEnd(InputStream is) throws IOException {
		try (
			Reader rdr = new InputStreamReader(is, StandardCharsets.UTF_8);
			BufferedReader brdr = new BufferedReader(rdr);
		) {
			return brdr.lines().collect(Collectors.joining(System.lineSeparator()));
		}
	}

	private static ResultSet doQuery(String queryFmt, Object... args) {
		try (CloseableQueryExec qe = new CloseableQueryExec(SPARQL_URL, queryFmt.formatted(args))) {
			return qe.execSelect();
		}
	}

	private static int countResults(ResultSet rs) {
		int count = 0;
		while (rs.hasNext()) {
			++count;
			rs.next();
		}
		return count;
	}

	private static void loadSampleData() {
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
				UpdateProcessor exec = UpdateExecutionFactory.createRemote(update, SPARQL_URL);
				executeUpdate(exec);
			}
		} catch (IOException ex) {
			fail(ex.getMessage());
		}
	}

	private static void doUpdate(String queryFmt, Object... args) {
		UpdateRequest ur = UpdateFactory.create(queryFmt.formatted(args));
		UpdateProcessor exec = UpdateExecutionFactory.createRemote(ur, SPARQL_URL);
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
				log.info("Encountered known bug in Jena 2.7.4/ARQ 2.9.4.  Ignoring NPE.");
			} else {
				throw new RuntimeException("Encountered NPE", ex);
			}
		}
	}
}
