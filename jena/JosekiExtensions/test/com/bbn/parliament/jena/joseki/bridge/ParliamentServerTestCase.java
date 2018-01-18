package com.bbn.parliament.jena.joseki.bridge;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jena.joseki.bridge.tracker.Tracker;
import com.bbn.parliament.jena.joseki.client.RDFFormat;
import com.bbn.parliament.jena.joseki.client.RemoteModel;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.sparql.modify.request.QuadDataAcc;
import com.hp.hpl.jena.sparql.modify.request.UpdateDataInsert;
import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateProcessor;
import com.hp.hpl.jena.update.UpdateRequest;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

import test_util.RdfResourceLoader;

public class ParliamentServerTestCase extends ParliamentServerBase {
	private static class CloseableQueryExec implements Closeable {
		private QueryExecution qe;

		public CloseableQueryExec(String sparqlService, String queryFmt, Object... args) {
			String query = String.format(queryFmt, args);
			qe = QueryExecutionFactory.sparqlService(sparqlService, query);
		}

		public ResultSet execSelect() {
			return qe.execSelect();
		}

		@Override
		public void close() {
			if (qe != null) {
				qe.close();
				qe = null;
			}
		}
	}

	private static final String HOST = "localhost";
	private static final String PORT = System.getProperty("jetty.port", "8586");
	private static final String SPARQL_URL = String.format(RemoteModel.DEFAULT_SPARQL_ENDPOINT_URL, HOST, PORT);
	private static final String BULK_URL = String.format(RemoteModel.DEFAULT_BULK_ENDPOINT_URL, HOST, PORT);
	private static final String[] RSRCS_TO_LOAD = { "univ-bench.owl", "University15_20.owl.zip" };
	private static final String TEST_SUBJECT = "http://example.org/#Test";
	private static final String TEST_CLASS = "http://example.org/#TestClass";
	private static final String TEST_LITERAL = "Test";
	private static final String EVERYTHING_QUERY = ""
		+ "select ?s ?o ?p ?g where {%n"
		+ "	{ ?s ?p ?o }%n"
		+ "	union%n"
		+ "	{ graph ?g { ?s ?p ?o } }%n"
		+ "}";
	private static final String CLASS_QUERY = ""
		+ "prefix owl: <http://www.w3.org/2002/07/owl#>%n"
		+ "%n"
		+ "select distinct ?class where {%n"
		+ "	?class a owl:Class .%n"
		+ "	filter (!isblank(?class))%n"
		+ "}";
	private static final String THING_QUERY = ""
		+ "prefix owl:  <http://www.w3.org/2002/07/owl#>%n"
		+ "prefix ex:   <http://www.example.org/>%n"
		+ "%n"
		+ "select ?a where {%n"
		+ "	bind ( ex:Test as ?a )%n"
		+ "	?a a owl:Thing .%n"
		+ "}";
	private static final String THING_INSERT = ""
		+ "prefix owl:  <http://www.w3.org/2002/07/owl#>%n"
		+ "prefix ex:   <http://www.example.org/>%n"
		+ "%n"
		+ "insert data {%n"
		+ "	ex:Test a owl:Thing .%n"
		+ "}";
	private static final String THING_DELETE = ""
		+ "prefix owl:  <http://www.w3.org/2002/07/owl#>%n"
		+ "prefix ex:   <http://www.example.org/>%n"
		+ "%n"
		+ "delete data {%n"
		+ "	ex:Test a owl:Thing .%n"
		+ "}";
	private static final RemoteModel rm = new RemoteModel(SPARQL_URL, BULK_URL);
	private static final Logger log = LoggerFactory.getLogger(ParliamentServerTestCase.class);

	@SuppressWarnings("static-method")
	@Test
	public void generalKBFunctionalityTest() throws IOException {
		rm.clearAll();
		ResultSet rs = doQuery(EVERYTHING_QUERY);
		int count = countResults(rs);
		assertEquals("Invalid precondition -- triple store is not empty.", 0, count);

		loadSampleData();

		rs = doQuery(CLASS_QUERY);
		count = countResults(rs);
		assertEquals(43, count);
		assertEquals(0, Tracker.getInstance().getTrackableIDs().size());

		rs = doQuery(THING_QUERY);
		count = countResults(rs);
		assertEquals("Invalid precondition -- triple store already contains data.", 0, count);

		doUpdate(THING_INSERT);

		rs = doQuery(THING_QUERY);
		count = countResults(rs);
		assertEquals("Data insert failed.", 1, count);

		doUpdate(THING_DELETE);

		rs = doQuery(THING_QUERY);
		count = countResults(rs);
		assertEquals("Data delete failed.", 0, count);
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

		String triples = String.format("<%1$s> <%2$s> \"%3$s\" .",
			TEST_SUBJECT, RDFS.label, TEST_LITERAL);
		rm.insertStatements(triples, RDFFormat.NTRIPLES, null, true);

		String query = String.format("select * where { ?thing a <%1$s> ; <%2$s> ?label . }",
			TEST_CLASS, RDFS.label);
		ResultSet rs = rm.selectQuery(query);
		boolean foundIt = false;
		while (rs.hasNext()) {
			QuerySolution qs = rs.nextSolution();
			RDFNode t = qs.get("thing");
			RDFNode l = qs.get("label");
			assertTrue(t != null);
			assertTrue(l != null);
			if (t.isURIResource() && TEST_SUBJECT.equals(t.asResource().getURI())
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
		String stmt = String.format("<http://example.org/foo> <%1$s> \"foo\" .", RDFS.label);
		String query = String.format("ask where { %1$s }", stmt);

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
		String update = String.format("%%1$s data { <%1$s> a <%2$s> . }", d, y);
		String query = String.format("select * where { ?thing a <%1$s> }", y);

		rm.updateQuery(String.format(update, "insert"));

		ResultSet rs = rm.selectQuery(query);
		boolean foundIt = false;
		while (rs.hasNext()) {
			QuerySolution qs = rs.nextSolution();
			if (d.equals(qs.getResource("thing").getURI())) {
				foundIt = true;
			}
		}
		assertTrue(foundIt);

		rm.updateQuery(String.format(update, "delete"));

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
		String triples = String.format("<%1$s> <%2$s> <%3$s> .",
			TEST_SUBJECT, RDF.type, TEST_CLASS);
		String query = String.format(
			"select * where { ?x a <%1$s> . graph <%2$s> { ?x a <%1$s> } }",
			TEST_CLASS, graphUri);

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

	private static ResultSet doQuery(String queryFmt, Object... args) {
		try (CloseableQueryExec qe = new CloseableQueryExec(SPARQL_URL, queryFmt, args)) {
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
			ex.printStackTrace();
			fail();
		}
	}

	private static void doUpdate(String queryFmt, Object... args) {
		UpdateRequest ur = UpdateFactory.create(String.format(queryFmt, args));
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
