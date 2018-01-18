// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.graph.index.spatial;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jena.graph.KbGraph;
import com.bbn.parliament.jena.graph.KbGraphFactory;
import com.bbn.parliament.jena.graph.KbGraphStore;
import com.bbn.parliament.jena.graph.index.IndexFactoryRegistry;
import com.bbn.parliament.jena.graph.index.IndexManager;
import com.bbn.parliament.jena.graph.index.RecordFactory;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.vocabulary.Geo;
import com.bbn.parliament.jena.graph.index.spatial.standard.Constants;
import com.bbn.parliament.jena.joseki.client.RDFFormat;
import com.bbn.parliament.jena.query.QueryTestUtil;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.query.ResultSetRewindable;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.FileManager;
import com.vividsolutions.jts.geom.Geometry;

/** @author Robert Battle */
public abstract class AbstractSpatialTest {
	protected static final Logger LOG = LoggerFactory.getLogger(AbstractSpatialTest.class);

	protected static final String EXAMPLE_NS = "http://parliament.semwebcentral.org/spatial/examples#";
	protected static final String EXAMPLE1_NS = "http://parliament.semwebcentral.org/spatial/examples/example1#";
	protected static final String EXAMPLE2_NS = "http://parliament.semwebcentral.org/spatial/examples/example2#";
	protected static final String EXAMPLE3_NS = "http://parliament.semwebcentral.org/spatial/examples/example3#";
	protected static final String EXAMPLE4_NS = "http://parliament.semwebcentral.org/spatial/examples/example4#";
	protected static final String EXAMPLE_CITIES_NS = "http://parliament.semwebcentral.org/spatial/examples/cities#";

	protected static final String COMMON_PREFIXES = "PREFIX rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
		+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
		+ "PREFIX owl:  <http://www.w3.org/2002/07/owl#> "
		+ "PREFIX xsd:  <http://www.w3.org/2001/XMLSchema#> "
		+ "PREFIX snap: <http://www.ifomis.org/bfo/1.0/snap#> "
		+ "PREFIX span: <http://www.ifomis.org/bfo/1.0/span#> "
		+ "PREFIX time: <http://www.w3.org/2006/time#> "
		+ "PREFIX georss: <http://www.georss.org/georss/> "
		+ "PREFIX gml: <"
		+ Constants.GML_NS
		+ "> ";

	protected static final String PREFIXES = COMMON_PREFIXES
		+ "PREFIX rcc: <" + Constants.RCC_NS + "> "
		+ "PREFIX ogc: <" + Constants.OGC_NS + "> "
		+ "PREFIX spatial: <" + Constants.BUFFER_NS + "> "
		+ "PREFIX example: <" + EXAMPLE_NS + "> "
		+ "PREFIX example1: <" + EXAMPLE1_NS + "> "
		+ "PREFIX cities: <" + EXAMPLE_CITIES_NS + "> ";

	protected static final String GEO_SPARQL_PREFIXES = COMMON_PREFIXES
		+ "PREFIX geo: <" + Geo.uri + "> \n"
		+ "PREFIX geof: <" + com.bbn.parliament.jena.graph.index.spatial.geosparql.Constants.OGC_FUNCTION_NS + "> \n";

	public static final String JDBC_URL = "jdbc:postgresql://localhost/spatial_index_test";
	public static final String USERNAME = "spatial";
	public static final String PASSWORD = "data";

	protected static Model model;
	protected static KbGraph graph;
	protected static KbGraphStore graphStore;

	@BeforeClass
	public static void setUpKb() {
		graph = KbGraphFactory.createDefaultGraph();
		graphStore = new KbGraphStore(graph);
		graphStore.initialize();

		model = ModelFactory.createModelForGraph(graph);
		clearKb();
	}

	@SuppressWarnings("resource")
	public static Model addNamedGraph(String uri) {
		Node graphName = Node.createURI(uri);
		KbGraph g = KbGraphFactory.createNamedGraph();
		Model m = ModelFactory.createModelForGraph(g);
		graphStore.addGraph(graphName, g);
		return m;
	}

	@AfterClass
	public static void tearDownKb() {
		clearKb();
		model.close();
		graphStore.clear();
	}

	public static void clearKb() {
		model.removeAll();
	}

	protected static void loadData(String fileName) {
		loadData(fileName, model);
	}

	protected static void loadData(String fileName, Model m) {
		RDFFormat dataFormat = RDFFormat.parseFilename(fileName);
		FileManager.get().readModel(m, fileName, dataFormat.toString());
	}

	protected static void printQuerySolution(QuerySolution querySolution) {
		if (LOG.isInfoEnabled()) {
			LOG.info("QuerySolution:");
			for (Iterator<String> it = querySolution.varNames(); it.hasNext();) {
				String var = it.next();
				LOG.info("{} -> {}", var, querySolution.get(var));
			}
		}
	}

	protected static void checkResults(ResultSet rs, String... results) {
		SortedSet<String> expectedResultSet = new TreeSet<>(Arrays.asList(results));
		SortedSet<String> actualResultSet = new TreeSet<>();
		while (rs.hasNext()) {
			QuerySolution qs = rs.nextSolution();
			// printQuerySolution(qs);
			Resource loc = qs.getResource("a");
			if (loc != null) {
				actualResultSet.add(loc.getURI());
			}
		}

		SortedSet<String> expectedMinusActual = new TreeSet<>(expectedResultSet);
		expectedMinusActual.removeAll(actualResultSet);
		if (expectedMinusActual.size() > 0) {
			LOG.warn("Expected results that were not found:  {}", expectedMinusActual);
		}
		SortedSet<String> actualMinusExpected = new TreeSet<>(actualResultSet);
		actualMinusExpected.removeAll(expectedResultSet);
		if (actualMinusExpected.size() > 0) {
			LOG.warn("Actual results that were not expected:  {}", actualMinusExpected);
		}

		assertEquals(expectedResultSet.size(), actualResultSet.size());
		assertEquals(0, expectedMinusActual.size());
		assertEquals(0, actualMinusExpected.size());
	}

	protected SpatialIndex index;
	protected QueryExecution qExec;
	protected RecordFactory<Geometry> recordFactory;

	@Before
	public final void setup() {
		SpatialIndexFactory factory = new SpatialIndexFactory();
		factory.configure(getProperties());
		IndexFactoryRegistry.getInstance().register(factory);

		index = factory.createIndex(graph, null);
		IndexManager.getInstance().register(graph, null, factory, index);
		index.open();
		recordFactory = index.getRecordFactory();
		clearKb();
		index.clear();
		loadData("queries/ontology.ttl");

		for (Iterator<Node> graphNames = graphStore.listGraphNodes(); graphNames.hasNext(); ) {
			Node graphName = graphNames.next();
			if (KbGraphStore.MASTER_GRAPH.equals(graphName.getURI())) {
				continue;
			}
			Graph namedGraph = graphStore.getGraph(graphName);
			SpatialIndex namedIndex = factory.createIndex(namedGraph, graphName);
			IndexManager.getInstance().register(namedGraph, graphName, factory, namedIndex);
			namedIndex.open();
			namedIndex.clear();
		}
	}

	@After
	public final void removeIndex() {
		IndexManager.getInstance().unregister(graph, null, index);
	}

	protected abstract Properties getProperties();

	protected ResultSet performQuery(String query) {
		Query q = QueryFactory.create(query, Syntax.syntaxARQ);
		return performQuery(q);
	}

	protected ResultSet performQuery(Query q) {
		long start = System.currentTimeMillis();
		qExec = QueryExecutionFactory.create(q, graphStore.toDataset());
		ResultSet result = qExec.execSelect();
		LOG.debug("DoQuery: {}", (System.currentTimeMillis() - start));
		return result;
	}

	protected void runTest(String queryFile, String resultFile) {
		Query query = QueryFactory.read(queryFile, Syntax.syntaxARQ);
		ResultSet expected = QueryTestUtil.loadResultSet(resultFile);
		ResultSet actual = performQuery(query);

		ResultSetRewindable e = ResultSetFactory.makeRewindable(expected);
		ResultSetRewindable a = ResultSetFactory.makeRewindable(actual);
		boolean matches = QueryTestUtil.equals(e, a, query);
		StringBuilder message = new StringBuilder();
		if (!matches) {
			e.reset();
			a.reset();
			message.append(String.format("Expected:\n%s", ResultSetFormatter.asText(e)));
			message.append(String.format("Actual:\n%s", ResultSetFormatter.asText(a)));
		}
		expected = e;
		actual = a;

		assertTrue(message.toString(),matches);
	}
}
