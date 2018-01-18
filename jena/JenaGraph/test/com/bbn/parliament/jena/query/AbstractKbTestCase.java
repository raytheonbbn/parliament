package com.bbn.parliament.jena.query;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import com.bbn.parliament.jena.Kb;
import com.bbn.parliament.jena.graph.KbGraph;
import com.bbn.parliament.jena.graph.KbGraphFactory;
import com.bbn.parliament.jena.graph.KbGraphStore;
import com.bbn.parliament.jena.joseki.client.RDFFormat;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.sparql.resultset.XMLInput;

public abstract class AbstractKbTestCase {
	protected static final Node NAMED_GRAPH_URI = Node.createURI("http://example.org/testgraph");

	protected static KbGraph defaultGraph;
	protected static KbGraph namedGraph;
	protected static KbGraphStore dataset;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Kb.init();
		defaultGraph = KbGraphFactory.createDefaultGraph();
		namedGraph = KbGraphFactory.createNamedGraph();
		dataset = new KbGraphStore(defaultGraph);
		dataset.initialize();
		dataset.addGraph(NAMED_GRAPH_URI, namedGraph);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		dataset.clear();
	}

	@Before
	public void setUp() {
	}

	@SuppressWarnings("static-method")
	@After
	public void tearDown() {
		clearKb();
	}

	protected static void clearKb() {
		defaultGraph.clear();
		namedGraph.clear();
		List<Node> graphs = new ArrayList<>();
		Iterator<Node> it = dataset.listGraphNodes();
		while (it.hasNext()) {
			Node n = it.next();
			if (!n.equals(NAMED_GRAPH_URI) && !n.getURI().equals(KbGraphStore.MASTER_GRAPH)) {
				graphs.add(n);
			}
		}

		for (Node n : graphs) {
			dataset.removeGraph(n);
		}
	}

	protected static KbGraph getGraph() {
		return defaultGraph;
	}

	protected static KbGraph getNamedGraph() {
		return namedGraph;
	}

	protected static KbGraph getNamedGraph(String graphUri) {
		Node n = Node.createURI(graphUri);
		if (dataset.containsGraph(n)) {
			return (KbGraph)dataset.getGraph(n);
		}
		KbGraph graph = KbGraphFactory.createNamedGraph();
		dataset.addGraph(n, graph);
		return graph;
	}

	protected static InputStream getResource(String resource) {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		InputStream strm = cl.getResourceAsStream(resource);
		if (strm == null) {
			fail(String.format("Could not load resource: '%1$s'", resource));
		}
		return strm;
	}

	protected static void loadResource(String resource, Graph graph) throws IOException {
		RDFFormat fmt = RDFFormat.parseFilename(resource);
		Model model = ModelFactory.createModelForGraph(graph);
		try (InputStream in = getResource(resource)) {
			model.read(in, null, fmt.toString());
		}
	}

	protected static Query loadQuery(String query) {
		return QueryFactory.read(query);
		//return QueryFactory.create(readResource(query));
	}

	protected static String readResource(String resource) {
		try (InputStream input = getResource(resource)) {
			StringBuilder s = new StringBuilder();
			for (int i = input.read(); i > -1; i = input.read()) {
				s.append((char)i);
			}
			return s.toString();
		} catch (IOException ex) {
			fail(String.format("Could not read resource: '%s'", resource));
		}
		return null;
	}

	protected static ResultSet loadResultSet(String resultSet) {
		if (resultSet.toLowerCase().endsWith("srx")) {
			return ResultSetFactory.fromXML(getResource(resultSet));
		}
		return ResultSetFactory.fromRDF(loadModel(resultSet, null));
	}

	protected static Model loadModel(String model, String base) {
		Model m = ModelFactory.createDefaultModel();
		final String lmodel = model.toLowerCase();
		String type = "RDF/XML";
		if (lmodel.endsWith("ttl") ||
			lmodel.endsWith("rq")) {
			type = "TTL";
		}
		m.read(getResource(model), base, type);
		return m;
	}

	protected static boolean loadAskResultSet(String resultSet) {
		return XMLInput.booleanFromXML(getResource(resultSet));
	}
}
