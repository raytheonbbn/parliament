package com.bbn.parliament.spring_boot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.sparql.modify.request.QuadDataAcc;
import org.apache.jena.sparql.modify.request.UpdateDataInsert;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
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
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.bbn.parliament.client.jena.QuerySolutionStream;
import com.bbn.parliament.client.jena.RDFFormat;
import com.bbn.parliament.test_util.GraphUtils;
import com.bbn.parliament.test_util.RdfResourceLoader;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(Lifecycle.PER_CLASS)
public class ParliamentServerTests {
	private static final String HOST = "localhost";
	private static final String[] FILES_TO_LOAD = { "univ-bench.owl", "University15_20.owl" };
	private static final String TEST_SUBJECT = "http://example.org/#TestItem";
	private static final String TEST_CLASS = "http://example.org/#TestClass";
	private static final String TEST_LITERAL = "TestLiteral";
	private static final String TEST_NG_URI = "http://example.org/#TestGraph";
	private static final File DATA_DIR = new File(System.getProperty("test.data.path"));
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
		GraphUtils.clearAll(graphStoreUrl, sparqlUrl);
	}

	@AfterEach
	public void afterAll() {
		GraphUtils.clearAll(graphStoreUrl, sparqlUrl);
	}

	@Test
	public void generalKBFunctionalityTest() {
		GraphUtils.clearAll(graphStoreUrl, sparqlUrl);

		try (var stream = new QuerySolutionStream(EVERYTHING_QUERY, sparqlUrl)) {
			long count = stream.count();
			assertEquals(0, count, "Invalid precondition -- triple store is not empty.");
		}

		loadSampleData();

		try (var stream = new QuerySolutionStream(CLASS_QUERY, sparqlUrl)) {
			long count = stream.count();
			assertEquals(43, count);
		}

		try (var stream = new QuerySolutionStream(THING_QUERY, sparqlUrl)) {
			long count = stream.count();
			assertEquals(0, count, "Invalid precondition -- triple store already contains data.");
		}

		GraphUtils.doUpdate(updateUrl, THING_INSERT);

		try (var stream = new QuerySolutionStream(THING_QUERY, sparqlUrl)) {
			long count = stream.count();
			assertEquals(1, count, "Data insert failed.");
		}

		GraphUtils.doUpdate(updateUrl, THING_DELETE);

		try (var stream = new QuerySolutionStream(THING_QUERY, sparqlUrl)) {
			long count = stream.count();
			assertEquals(0, count, "Data delete failed.");
		}

		GraphUtils.clearAll(graphStoreUrl, sparqlUrl);

		try (var stream = new QuerySolutionStream(CLASS_QUERY, sparqlUrl)) {
			long count = stream.count();
			assertEquals(0, count, "Invalid postcondition -- triple store is not empty.");
		}
	}

	@Test
	public void namedGraphsTest() {
		assertTrue(GraphUtils.getAvailableNamedGraphs(sparqlUrl).isEmpty());

		GraphUtils.doUpdate(updateUrl, "create graph <%1$s>", TEST_NG_URI);
		assertTrue(GraphUtils.getAvailableNamedGraphs(sparqlUrl).equals(Collections.singleton(TEST_NG_URI)));

		GraphUtils.doUpdate(updateUrl, "drop silent graph <%1$s>", TEST_NG_URI);
		assertTrue(GraphUtils.getAvailableNamedGraphs(sparqlUrl).isEmpty());
	}

	@Test
	public void insertAndQueryTest() {
		GraphUtils.insert(updateUrl, TEST_SUBJECT, RDF.type.getURI(), NodeFactory.createURI(TEST_CLASS), null);
		GraphUtils.insert(updateUrl, TEST_SUBJECT, RDFS.label.getURI(), NodeFactory.createLiteral(TEST_LITERAL), null);

		String query = "select * where { ?thing a <%1$s> ; <%2$s> ?label . }";
		try (var stream = GraphUtils.doSelectQuery(sparqlUrl, query, TEST_CLASS, RDFS.label)) {
			long count = stream
				.map(qs -> Pair.of(qs.getResource("thing"), qs.getLiteral("label")))
				.filter(pair -> TEST_SUBJECT.equals(pair.getLeft().getURI())
					&& TEST_LITERAL.equals(pair.getRight().getLexicalForm())
					&& isStringLiteral(pair.getRight().getDatatypeURI()))
				.count();
			assertTrue(count > 0);
		}
		// delete inserted statements bc deleteAndQueryTest checks
		GraphUtils.delete(updateUrl, TEST_SUBJECT, RDF.type.getURI(), NodeFactory.createLiteral(TEST_CLASS), null);
		GraphUtils.delete(updateUrl, TEST_SUBJECT, RDFS.label.getURI(), NodeFactory.createLiteral(TEST_LITERAL), null);
	}

	private static boolean isStringLiteral(String datatypeUri) {
		return datatypeUri == null
			|| datatypeUri.isEmpty()
			|| datatypeUri.equals(XSDDatatype.XSDstring.getURI());
	}

	@Test
	public void deleteAndQueryTest() {
		String queryFmt = "ask where { <%1$s> <%2$s> \"%3$s\" . }";

		assertFalse(GraphUtils.doAskQuery(sparqlUrl, queryFmt, TEST_SUBJECT, RDFS.label, TEST_LITERAL));
		GraphUtils.insert(updateUrl, TEST_SUBJECT, RDFS.label.getURI(), NodeFactory.createLiteral(TEST_LITERAL), null);
		assertTrue(GraphUtils.doAskQuery(sparqlUrl, queryFmt, TEST_SUBJECT, RDFS.label, TEST_LITERAL));
		GraphUtils.delete(updateUrl, TEST_SUBJECT, RDFS.label.getURI(), NodeFactory.createLiteral(TEST_LITERAL), null);
		assertFalse(GraphUtils.doAskQuery(sparqlUrl, queryFmt, TEST_SUBJECT, RDFS.label, TEST_LITERAL));
	}

	@Test
	public void simpleSPARQLUpdateTest() {
		String d = "http://example.org/doughnut";
		String y = "http://example.org/yummy";
		String queryFmt = "ask where { <%1$s> a <%2$s> }";

		GraphUtils.insert(updateUrl, d, RDF.type.getURI(), NodeFactory.createURI(y), null);
		assertTrue(GraphUtils.doAskQuery(sparqlUrl, queryFmt, d, y));

		GraphUtils.delete(updateUrl, d, RDF.type.getURI(), NodeFactory.createURI(y), null);
		assertFalse(GraphUtils.doAskQuery(sparqlUrl, queryFmt, d, y));
	}

	@Test
	public void ngSparqlUpdateTest()
	{
		String graphUri = "http://example.org/foo/bar/#Graph2";
		String bs = "http://example.org/brusselsprouts";
		String y = "http://example.org/yucky";
		String query = "select * where { graph <%1$s> {?thing a <%2$s> } }";

		GraphUtils.doUpdate(updateUrl, "create graph <%1$s>", graphUri);
		GraphUtils.insert(updateUrl, bs, RDF.type.getURI(), NodeFactory.createURI(y), graphUri);

		boolean foundIt = false;
		try (var stream = GraphUtils.doSelectQuery(sparqlUrl, query, graphUri, y)) {
			foundIt = stream
					.map(qs -> qs.getResource("thing"))
					.map(Resource::getURI)
					.filter(uri -> bs.equals(uri))
					.count() == 1;
			assertTrue(foundIt);
		}

		GraphUtils.delete(updateUrl, bs, RDF.type.getURI(), NodeFactory.createURI(y), graphUri);
		foundIt = false;
		try (var stream = GraphUtils.doSelectQuery(sparqlUrl, query, graphUri, y)) {
			foundIt = stream
					.map(qs -> qs.getResource("thing"))
					.map(Resource::getURI)
					.filter(uri -> bs.equals(uri))
					.count() == 1;
			assertFalse(foundIt);
		}

		GraphUtils.doUpdate(updateUrl, "drop graph <%1$s>", graphUri);
	}

	@Test
	public void queryErrorTest() {
		String invalidQuery = "select * where { ?thing oogetyboogetyboo! }";
		boolean caughtException = false;
		try (var stream = new QuerySolutionStream(invalidQuery, sparqlUrl)) {
			@SuppressWarnings("unused")
			long count = stream.count();
		} catch (QueryParseException ex) {
			caughtException = true;
			LOG.info("Query parse error", ex);
		}
		assertTrue(caughtException);
	}

	@Test
	public void updateErrorTest() {
		boolean caughtException = false;
		try {
			// Delete invalid n-triples:
			GraphUtils.doUpdate(updateUrl, "delete data { oogetyboogetyboo! }");
		} catch (QueryParseException ex) {
			caughtException = true;
			LOG.info("Update parse error", ex);
		}
		assertTrue(caughtException);
	}

	@Test
	public void insertQueryNamedGraphTest() {
		String graphUri = "http://example.org/foo/bar/#Graph3";
		String query = "select * where { ?x a <%1$s> . graph <%2$s> { ?x a <%1$s> } }";

		GraphUtils.doUpdate(updateUrl, "create graph <%1$s>", graphUri);

		GraphUtils.insert(updateUrl, TEST_SUBJECT, RDF.type.getURI(), NodeFactory.createURI(TEST_CLASS), null);
		GraphUtils.insert(updateUrl, TEST_SUBJECT, RDF.type.getURI(), NodeFactory.createURI(TEST_CLASS), graphUri);

		boolean foundIt = false;

		try (var stream = GraphUtils.doSelectQuery(sparqlUrl, query, TEST_CLASS, graphUri)) {
			foundIt = stream
					.map(qs -> qs.getResource("x"))
					.map(Resource::getURI)
					.filter(uri -> TEST_SUBJECT.equals(uri))
					.count() == 1;
			assertTrue(foundIt);
		}

		GraphUtils.doUpdate(updateUrl, "drop graph <%1$s>", graphUri);
	}

	@Test
	public void namedGraphUnionTest() throws IOException {
		String graph1Uri = "http://example.org/foo/bar/#Graph4";
		String graph2Uri = "http://example.org/foo/bar/#Graph5";
		String unionGraphUri = "http://example.org/foo/bar/#UnionGraph";
		String triple1 = "<%1$s> <%2$s> <%3$s1> .".formatted(TEST_SUBJECT, RDF.type, TEST_CLASS);
		String triple2 = "<%1$s> <%2$s> <%3$s2> .".formatted(TEST_SUBJECT, RDF.type, TEST_CLASS);
		String query = "select * where { graph <%1$s> { ?x a <%2$s1> , <%2$s2> . } }";

		GraphUtils.doUpdate(updateUrl, "create graph <%1$s>", graph1Uri);
		try {
			GraphUtils.insertStatements(graphStoreUrl, triple1, RDFFormat.NTRIPLES, graph1Uri);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		GraphUtils.doUpdate(updateUrl, "create graph <%1$s>", graph2Uri);
		try {
			GraphUtils.insertStatements(graphStoreUrl, triple2, RDFFormat.NTRIPLES, graph2Uri);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		GraphUtils.doUpdate(updateUrl, """
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
		try (var stream = GraphUtils.doSelectQuery(sparqlUrl, query, unionGraphUri, TEST_CLASS, TEST_CLASS)) {
			foundIt = stream
					.map(qs -> qs.getResource("x"))
					.map(Resource::getURI)
					.filter(uri -> TEST_SUBJECT.equals(uri))
					.count() == 1;
			assertTrue(foundIt);
		}

		GraphUtils.doUpdate(updateUrl, "drop graph <%1$s>", unionGraphUri);
		GraphUtils.doUpdate(updateUrl, "drop graph <%1$s>", graph1Uri);
		GraphUtils.doUpdate(updateUrl, "drop graph <%1$s>", graph2Uri);
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
		GraphUtils.insertStatements(graphStoreUrl, os.toString(), RDFFormat.NTRIPLES, null);

		Model resultModel = GraphUtils.doConstructQuery(sparqlUrl, query);
		assertTrue(testModel.difference(resultModel).isEmpty());
	}

	private void loadSampleData() {
		try {
			Model clientSideModel = ModelFactory.createDefaultModel();
			for (String fileName : FILES_TO_LOAD) {
				var file = new File(DATA_DIR, fileName);
				RdfResourceLoader.load(file, clientSideModel);
			}

			QuadDataAcc qd = new QuadDataAcc();
			StmtIterator it = clientSideModel.listStatements();
			while (it.hasNext()) {
				Statement stmt = it.next();
				qd.addTriple(stmt.asTriple());
			}
			UpdateDataInsert update = new UpdateDataInsert(qd);
			UpdateExecutionFactory.createRemote(update, updateUrl).execute();
		} catch (Exception ex) {
			fail(ex.getMessage());
		}
	}
}
