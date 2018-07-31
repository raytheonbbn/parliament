// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Map;
import java.util.StringTokenizer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.bbn.parliament.jena.NodeCreateUtils;
import com.bbn.parliament.jni.Config;
import com.bbn.parliament.jni.KbInstance;
import com.hp.hpl.jena.graph.Factory;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.GraphUtil;
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
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.util.CollectionFactory;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

/** @author dkolas */
@RunWith(JUnitPlatform.class)
public class KbGraphTest {
	private Config config;
	private KbGraph graph;

	@BeforeEach
	protected void beforeEach() {
		config = Config.readFromFile();
	}

	@AfterEach
	public void afterEach() {
		if (graph != null) {
			graph.close();
		}
		graph = null;
		KbInstance.deleteKb(config, null);
	}

	private Graph getGraph() {
		if (graph != null) {
			graph.close();
			KbInstance.deleteKb(config, null);
		}
		graph = KbGraphFactory.createDefaultGraph();
		return graph;
	}

	@Test
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

	/** Check that contains respects by-value semantics */
	@Test
	public void testContainsByValue() {
		if (getGraph().getCapabilities().handlesLiteralTyping()) {
			Graph g1 = getGraphWith("x P '1'xsd:integer");
			assertTrue(g1.contains(NodeCreateUtils.createTriple("x P '01'xsd:int")));

			Graph g2 = getGraphWith("x P '1'xsd:int");
			assertTrue(g2.contains(NodeCreateUtils.createTriple("x P '1'xsd:integer")));

			Graph g3 = getGraphWith("x P '123'xsd:string");
			assertTrue(g3.contains(NodeCreateUtils.createTriple("x P '123'")));
		}
	}

	/**
	 * Test that remove(s, p, o) works, in the presence of inferencing graphs
	 * that mean emptyness isn't available. This is why we go round the houses
	 * and test that expected ~= initialContent + addedStuff - removed -
	 * initialContent.
	 *
	 * @param addStr the triples to add to the graph to start with
	 * @param removeStr the pattern to use in the removal
	 * @param resultStr the triples that should remain in the graph
	 */
	@ParameterizedTest
	@CsvSource({
		"x R y,               x R y,    ",
		"x R y; a P b,        x R y,    a P b",
		"x R y; a P b,        ?? R y,   a P b",
		"x R y; a P b,        x R ??,   a P b",
		"x R y; a P b,        x ?? y,   a P b",
		"x R y; a P b,        ?? ?? ??, ",
		"x R y; a P b; c P d, ?? P ??,  x R y",
		"x R y; a P b; x S y, x ?? ??,  a P b",
	})
	public void testRemoveSPO(String addStr, String removeStr, String resultStr) {
		Graph content = getGraph();
		Graph baseContent = copy(content);
		graphAdd(content, addStr);
		Triple remove = NodeCreateUtils.createTriple(removeStr);
		Graph expected = graphWith(resultStr == null ? "" : resultStr);
		content.getBulkUpdateHandler().remove(remove.getSubject(), remove.getPredicate(), remove.getObject());
		Graph finalContent = remove(copy(content), baseContent);
		assertIsomorphic(removeStr, expected, finalContent);
	}

	private Graph getGraphWith(String facts) {
		Graph g = getGraph();
		graphAdd(g, facts);
		return g;
	}

	/**
	 * Answer the graph <code>g</code> after adding to it every triple encoded in
	 * <code>s</code> in the fashion of <code>tripleArray</code>, a
	 * semi-separated sequence of space-separated node descriptions.
	 */
	private static Graph graphAdd(Graph g, String s) {
		StringTokenizer semis = new StringTokenizer(s, ";");
		while (semis.hasMoreTokens()) {
			g.add(NodeCreateUtils.createTriple(PrefixMapping.Extended, semis.nextToken()));
		}
		return g;
	}

	/**
	 * Answer a new memory-based graph with initial contents as described by
	 * <code>s</code> in the fashion of <code>graphAdd()</code>.
	 */
	private static final Graph graphWith(String s) {
		Graph g = Factory.createGraphMem();
		g.getPrefixMapping().setNsPrefixes(PrefixMapping.Extended);
		return graphAdd(g, s);
	}

	private static Graph remove(Graph toUpdate, Graph toRemove) {
		toUpdate.getBulkUpdateHandler().delete(toRemove);
		return toUpdate;
	}

	private static Graph copy(Graph g) {
		Graph result = Factory.createDefaultGraph();
		result.getBulkUpdateHandler().add(g);
		return result;
	}

	/**
	 * Assert that the supplied graph <code>got</code> is isomorphic with the the
	 * desired graph <code>expected</code>; if not, display a readable
	 * description of both graphs.
	 */
	private static void assertIsomorphic(String title, Graph expected, Graph got) {
		if (!expected.isIsomorphicWith(got)) {
			Map<Node, Object> map = CollectionFactory.createHashedMap();
			fail(title + ": wanted " + nice(expected, map) + "\nbut got " + nice(got, map));
		}
	}

	/**
	 * Answer a string which is a newline-separated list of triples (as produced
	 * by niceTriple) in the graph <code>g</code>. The map <code>bnodes</code>
	 * maps already-seen bnodes to their "nice" strings.
	 */
	private static String nice(Graph g, Map<Node, Object> bnodes) {
		StringBuffer b = new StringBuffer(g.size() * 100);
		ExtendedIterator<Triple> it = GraphUtil.findAll(g);
		while (it.hasNext()) {
			niceTriple(b, bnodes, it.next());
		}
		return b.toString();
	}

	/**
	 * Append to the string buffer <code>b</code> a "nice" representation of the
	 * triple <code>t</code> on a new line, using (and updating)
	 * <code>bnodes</code> to supply "nice" strings for any blank nodes.
	 */
	private static void niceTriple(StringBuffer b, Map<Node, Object> bnodes, Triple t) {
		b.append("\n    ");
		appendNode(b, bnodes, t.getSubject());
		appendNode(b, bnodes, t.getPredicate());
		appendNode(b, bnodes, t.getObject());
	}

	/**
	 * A counter for new bnode strings; it starts at 1000 so as to make the bnode
	 * strings more uniform (at least for the first 9000 bnodes).
	 */
	private static int bnc = 1000;

	/**
	 * Append to the string buffer <code>b</code> a space followed by the "nice"
	 * representation of the node <code>n</code>. If <code>n</code> is a bnode,
	 * re-use any existing string for it from <code>bnodes</code> or make a new
	 * one of the form <i>_bNNNN</i> with NNNN a new integer.
	 */
	private static void appendNode(StringBuffer b, Map<Node, Object> bnodes, Node n) {
		b.append(' ');
		if (n.isBlank()) {
			Object already = bnodes.get(n);
			if (already == null) {
				bnodes.put(n, already = "_b" + bnc++);
			}
			b.append(already);
		} else {
			b.append(n.toString(PrefixMapping.Extended, true));
		}
	}
}
