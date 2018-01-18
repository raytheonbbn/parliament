// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.graph;

import java.io.FileNotFoundException;

import com.bbn.parliament.jena.from_jena_test.AbstractTestGraph;
import com.bbn.parliament.jni.Config;
import com.bbn.parliament.jni.KbInstance;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Node_Literal;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

/** @author dkolas */
public class KbGraphTest extends AbstractTestGraph {
	private Config config;
	private KbGraph graph;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		config = Config.readFromFile();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		if (graph != null) {
			graph.close();
		}
		graph = null;
		KbInstance.deleteKb(config, null);
	}

	public KbGraphTest(String arg0) {
		super(arg0);
	}

	@Override
	public Graph getGraph() {
		if (graph != null) {
			graph.close();
			KbInstance.deleteKb(config, null);
		}
		graph = KbGraphFactory.createDefaultGraph();
		return graph;
	}

	public void testLiteralLanguage() {
		Graph m = getGraphWith("a p 'chat'en");
		ExtendedIterator<Triple> iterator = m.find(null, null, null);
		assertTrue(iterator.hasNext());

		Node obj = iterator.next().getObject();

		assertTrue(obj instanceof Node_Literal);
		Node_Literal literalObj = (Node_Literal) obj;

		//System.out.println(literalObj.getLiteralLanguage());
		assertEquals("en", literalObj.getLiteralLanguage());
	}

	/**
	 * @throws FileNotFoundException
	 * @see com.hp.hpl.jena.graph.test.AbstractTestGraph#testIsomorphismFile()
	 */
	@Override
	public void testIsomorphismFile() throws FileNotFoundException {
		//assertTrue("This test does not work properly due to file locations", false);
	}

	public static void main(String[] args) {
		Config config = Config.readFromFile();
		KbInstance.deleteKb(config, null);
		try (KbGraph graph = KbGraphFactory.createDefaultGraph()) {
			Model model = ModelFactory.createModelForGraph(graph);
			model.read(
				"file:///c:/jfpactd/ontology/ont/ontology/domain/src/capability.ttl", "N3");

			// Triple t = new Triple(Node.create("x"), Node.create("y"), Node.create("z"));
			// Triple t2 = new Triple(Node.create("z"), Node.create("a"), Node.create("b"));
			// Triple t3 = new Triple(Node.create("b"), Node.create("c"), Node.create("d"));

			// graph.add(t);
			// graph.add(t2);
			// graph.add(t3);
			String queryString = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
				+ "SELECT ?q ?r "
				+ "WHERE { "
				+ "?q rdfs:subClassOf ?x . "
				+ "?q rdfs:label \"Standard\" . " + "?q rdfs:comment ?r}";
			Query query = QueryFactory.create(queryString);

			QueryExecution executionFactory = QueryExecutionFactory.create(query,
				model);

			ResultSet result = executionFactory.execSelect();
			while (result.hasNext()) {
				System.out.println(result.nextSolution());
			}

			//System.out.println(graph.size());
			//Triple t = new Triple(Node.create("x"), Node.create("y"), Node.create("z"));
			//graph.add(t);
			//System.out.println(graph.size());
			//Iterator i = graph.find(Node.ANY, Node.ANY, Node.ANY);
			//while (i.hasNext()) {
			//	System.out.println("triple: " + i.next());
			//}
			//graph.delete(t);
			//System.out.println(graph.size());
			//i = graph.find(Node.ANY, Node.ANY, Node.ANY);
			//while (i.hasNext()) {
			//	System.out.println("triple: " + i.next());
			//}
		}
	}

	/** {@inheritDoc} */
	@Override
	public void testRemoveSPO() {
		// TODO Auto-generated method stub
		super.testRemoveSPO();
	}
}
