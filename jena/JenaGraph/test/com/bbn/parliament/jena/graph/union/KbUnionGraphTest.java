// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2018, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.graph.union;

import java.util.Iterator;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jena.from_jena_test.AbstractTestGraph;
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

public class KbUnionGraphTest extends AbstractTestGraph {
	private static final String LEFT_BASE = "http://example.org/left";
	private static final String RIGHT_BASE = "http://example.org/right";
	private static final String UNION_BASE = "http://example.org/union";

	private static final Logger log = LoggerFactory.getLogger(KbUnionGraphTest.class);

	private int counter = -1;
	private KbGraphStore dataset;

	/** {@inheritDoc} */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		dataset = new KbGraphStore(KbGraphFactory.createDefaultGraph());
		counter = -1;
	}

	/** {@inheritDoc} */
	@SuppressWarnings("resource")
	@Override
	public KbUnionGraph getGraph() {
		++counter;

		KbGraph left = KbGraphFactory.createNamedGraph();
		KbGraph right = KbGraphFactory.createNamedGraph();

		Node leftName = Node.createURI(LEFT_BASE + counter);
		Node rightName = Node.createURI(RIGHT_BASE + counter);
		Node unionName = Node.createURI(UNION_BASE + counter);

		dataset.addGraph(leftName, left);
		dataset.addGraph(rightName, right);
		dataset.addUnionGraph(unionName, leftName, rightName);

		return (KbUnionGraph) dataset.getGraph(unionName);
	}

	public KbUnionGraphTest(String name) {
		super(name);
	}

	@Override
	public void tearDown() throws Exception {
		dataset.clear();
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
		return dataset.getGraph(graph.getLeftGraphName());
	}

	private Model getRightModel(KbUnionGraph graph) {
		Graph g = getRightGraph(graph);
		return ModelFactory.createModelForGraph(g);
	}

	private Graph getRightGraph(KbUnionGraph graph) {
		return dataset.getGraph(graph.getRightGraphName());
	}

	@Test
	public void testCreateUnionGraph() {
		KbUnionGraph union = getGraph();
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
		KbUnionGraph union = getGraph();
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
		KbUnionGraph union = getGraph();
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
			dataset.toDataset()).execSelect();
		count = printResultSet("Union graph query 1", rs);
		assertEquals(2, count);

		rs = QueryExecutionFactory.create(
			String.format(UNION_TEST_QUERY, UNION_BASE, counter, "?p"),
			dataset.toDataset()).execSelect();
		count = printResultSet("Union graph query 2", rs);
		assertEquals(3, count);
	}

	/** {@inheritDoc} */
	@Override
	@Test
	@Ignore
	public void testIsomorphismFile() {
		// no test here as we don't have the file
		log.warn("Skipping test 'testIsomorphismFile', since we don't have the file.");
	}

	/** {@inheritDoc} */
	@Override
	@Test
	@Ignore
	public void testContainsByValue() {
		// Parliament doesn't implement comparisons like xsd:int to xsd:integer,
		// so this test cannot work at present.
		//super.testContainsByValue();
		log.warn("Skipping test 'testContainsByValue', since Parliament doesn't "
			+ "implement comparisons like xsd:int to xsd:integer.");
	}

	private static int printResultSet(String queryLabel, ResultSet rs) {
		int count = 0;
		while (rs.hasNext()) {
			QuerySolution qs = rs.nextSolution();
			++count;
			if (log.isDebugEnabled()) {
				log.debug("Result {} of {}", count, queryLabel);
				Iterator<String> varIter = qs.varNames();
				while (varIter.hasNext()) {
					String var = varIter.next();
					log.debug("   ?{} = '{}'", var, qs.get(var));
				}
			}
		}
		if (count == 0) {
			log.debug("No results for {}", queryLabel);
		}
		return count;
	}
}
