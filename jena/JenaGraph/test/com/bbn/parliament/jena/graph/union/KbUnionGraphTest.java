// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2018, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.graph.union;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jena.graph.KbGraph;
import com.bbn.parliament.jena.graph.KbGraphFactory;
import com.bbn.parliament.jena.graph.KbGraphStore;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.FileManager;

@RunWith(JUnitPlatform.class)
public class KbUnionGraphTest {
	private static final String LEFT_BASE = "http://example.org/left";
	private static final String RIGHT_BASE = "http://example.org/right";
	private static final String UNION_BASE = "http://example.org/union";

	private static final Logger log = LoggerFactory.getLogger(KbUnionGraphTest.class);

	private static int counter = -1;
	private KbGraph defaultGraph;
	private KbGraph leftGraph;
	private KbGraph rightGraph;
	private KbGraphStore graphStore;
	private KbUnionGraph union;

	@BeforeEach
	public void beforeEach() {
		Node leftName = Node.createURI(LEFT_BASE + counter);
		Node rightName = Node.createURI(RIGHT_BASE + counter);
		Node unionName = Node.createURI(UNION_BASE + counter);

		defaultGraph = KbGraphFactory.createDefaultGraph();
		leftGraph = KbGraphFactory.createNamedGraph();
		rightGraph = KbGraphFactory.createNamedGraph();

		graphStore = new KbGraphStore(defaultGraph);
		graphStore.addGraph(leftName, leftGraph);
		graphStore.addGraph(rightName, rightGraph);
		graphStore.addUnionGraph(unionName, leftName, rightName);

		union = (KbUnionGraph) graphStore.getGraph(unionName);
	}

	@AfterEach
	public void afterEach() {
		graphStore.clear();
	}

	private static Model loadModel(String path) {
		Model m = FileManager.get().loadModel(path);
		return m;
	}

	private Model getLeftModel(KbUnionGraph graph) {
		Graph g = getLeftGraph(graph);
		return ModelFactory.createModelForGraph(g);
	}

	private Graph getLeftGraph(KbUnionGraph graph) {
		return graphStore.getGraph(graph.getLeftGraphName());
	}

	private Model getRightModel(KbUnionGraph graph) {
		Graph g = getRightGraph(graph);
		return ModelFactory.createModelForGraph(g);
	}

	private Graph getRightGraph(KbUnionGraph graph) {
		return graphStore.getGraph(graph.getRightGraphName());
	}

	@Test
	public void testCreateUnionGraph() {
		Graph left = getLeftGraph(union);
		Model leftModel = getLeftModel(union);
		Graph right = getRightGraph(union);
		Model rightModel = getRightModel(union);

		assertEquals(0, union.size());
		Model data;

		data = loadModel("data/data-r2/triple-match/data-01.ttl");
		leftModel.add(data);

		assertEquals(2, union.size());
		assertTrue(union.isIsomorphicWith(left));

		data = loadModel("data/data-r2/triple-match/data-02.ttl");
		rightModel.add(data);

		assertEquals(5, union.size());
		assertFalse(union.isIsomorphicWith(left));
		assertFalse(union.isIsomorphicWith(right));
	}

	@Test
	public void testCloseUnionGraph() {
		Graph left = getLeftGraph(union);
		Graph right = getRightGraph(union);

		union.close();
		assertTrue(union.isClosed());

		assertFalse(left.isClosed());
		assertFalse(right.isClosed());
	}

	private static final String UNION_TEST_QUERY = ""
		+ "prefix : <http://example.org/data/> "
		+ "select * where { "
		+ "  graph <%1$s%2$d> {"
		+ "    :x %3$s ?o ."
		+ "  }"
		+ "}";

	@Test
	public void testQueryUnionGraph() {
		Graph left = getLeftGraph(union);
		Model leftModel = getLeftModel(union);
		//Graph right = getRightGraph(union);
		Model rightModel = getRightModel(union);

		leftModel.add(loadModel("data/data-r2/triple-match/data-01.ttl"));

		assertEquals(2, union.size());
		assertTrue(union.isIsomorphicWith(left));

		rightModel.add(loadModel("data/data-r2/triple-match/data-02.ttl"));

		ResultSet rs;
		int count;

		rs = QueryExecutionFactory.create(
			String.format(UNION_TEST_QUERY, UNION_BASE, counter, ":p"),
			graphStore.toDataset()).execSelect();
		count = printResultSet("Union graph query 1", rs);
		assertEquals(2, count);

		rs = QueryExecutionFactory.create(
			String.format(UNION_TEST_QUERY, UNION_BASE, counter, "?p"),
			graphStore.toDataset()).execSelect();
		count = printResultSet("Union graph query 2", rs);
		assertEquals(3, count);
	}

	private static int printResultSet(String queryLabel, ResultSet rs) {
		int count = 0;
		List<String> vars = rs.getResultVars();
		while (rs.hasNext()) {
			QuerySolution qs = rs.next();
			log.debug("Result {} of {}", ++count, queryLabel);
			vars.forEach(var -> log.debug("   ?{} = '{}'", var, qs.get(var)));
		}
		if (count == 0) {
			log.debug("No results for {}", queryLabel);
		}
		return count;
	}
}
