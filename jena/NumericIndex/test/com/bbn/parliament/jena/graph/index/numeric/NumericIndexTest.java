
package com.bbn.parliament.jena.graph.index.numeric;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import com.bbn.parliament.jena.graph.index.IndexException;
import com.bbn.parliament.jena.graph.index.IndexFactory;
import com.bbn.parliament.jena.graph.index.IndexFactory.IndexFactoryHelper;
import com.bbn.parliament.jena.graph.index.Record;
import com.bbn.parliament.jena.graph.index.numeric.Constants.NumberType;
import com.bbn.parliament.jena.query.index.QueryableIndexTestBase;
import com.bbn.parliament.jena.util.FileUtil;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.XSD;

/** @author rbattle */
public class NumericIndexTest extends
QueryableIndexTestBase<NumericIndex<Integer>, Integer> {
	private static final String AGE_URI = "http://example.org#age";

	private Model defaultModel;
	private Model namedModel;
	@Before
	public void createModel() {
		defaultModel = ModelFactory.createModelForGraph(store.getDefaultGraph());
		namedModel = ModelFactory.createModelForGraph(store.getGraph(namedGraphName));
	}

	//
	// @Test
	// public void testAddAndRemove() {
	// Record<Integer> r;
	//
	// r = Record.create(Node.createURI("http://example.org/1"), new Integer(1));
	// index.add(r);
	// assertEquals(1L, index.size());
	//
	// r = Record.create(Node.createURI("http://example.org/2"), new Integer(2));
	// index.add(r);
	// assertEquals(2L, index.size());
	//
	// index.remove(r);
	// assertEquals(1L, index.size());
	// }
	//
	// @Test
	// public void testOpenClose() {
	// Record<Integer> r;
	//
	// r = Record.create(Node.createURI("http://example.org/1"), new Integer(1));
	// index.add(r);
	// assertEquals(1L, index.size());
	//
	// index.close();
	//
	// index.open();
	// assertEquals(1L, index.size());
	//
	// r = Record.create(Node.createURI("http://example.org/2"), new Integer(2));
	// index.add(r);
	// assertEquals(2L, index.size());
	// }

	@Test
	public void testFilter() {
		testFilter(defaultGraphIndex, defaultModel);
		testFilter(namedGraphIndex, namedModel);
	}

	private void testFilter(NumericIndex<Integer> index, Model model) {
		// String example = "http://example.org/";
		Node p = Node.createURI(AGE_URI);
		Node o20 = ResourceFactory.createTypedLiteral(new Integer(20)).asNode();
		Node o25 = ResourceFactory.createTypedLiteral(new Integer(25)).asNode();
		Node o29 = ResourceFactory.createTypedLiteral(new Integer(29)).asNode();
		Node o32 = ResourceFactory.createTypedLiteral(new Integer(32)).asNode();
		Node o53 = ResourceFactory.createTypedLiteral(new Integer(53)).asNode();
		List<Statement> statements = new ArrayList<>();
		statements.add(model.asStatement(Triple.create(createRecord(1).getKey(),
			p, o20)));
		statements.add(model.asStatement(Triple.create(createRecord(2).getKey(),
			p, o25)));
		statements.add(model.asStatement(Triple.create(createRecord(3).getKey(),
			p, o29)));
		statements.add(model.asStatement(Triple.create(createRecord(4).getKey(),
			p, o32)));
		statements.add(model.asStatement(Triple.create(createRecord(5).getKey(),
			p, o53)));
		model.add(statements);
		// model.add(ResourceFactory.createResource(example + "1"),
		// ResourceFactory.createProperty(AGE_URI),
		// );
		// model.add(ResourceFactory.createResource(example + "2"),
		// ResourceFactory.createProperty(AGE_URI),
		// ResourceFactory.createTypedLiteral(new Integer(25)));
		// model.add(ResourceFactory.createResource(example + "3"),
		// ResourceFactory.createProperty(AGE_URI),
		// ResourceFactory.createTypedLiteral(new Integer(29)));
		// model.add(ResourceFactory.createResource(example + "4"),
		// ResourceFactory.createProperty(AGE_URI),
		// ResourceFactory.createTypedLiteral(new Integer(32)));
		// model.add(ResourceFactory.createResource(example + "5"),
		// ResourceFactory.createProperty(AGE_URI),
		// ResourceFactory.createTypedLiteral(new Integer(53)));
		String query = "PREFIX xsd: <"
			+ XSD.getURI()
			+ ">\nSELECT ?x WHERE { ?x <"
			+ AGE_URI
			+ "> ?z . FILTER (?z < \"30\"^^xsd:integer && ?z >= \"25\"^^xsd:integer)  }";

		try {
			assertEquals(5L, index.size());
		} catch (IndexException e) {
			fail();
		}
		QueryExecution qExec = QueryExecutionFactory.create(query,
			store.toDataset());
		ResultSet result = qExec.execSelect();
		assertTrue(result.hasNext());
		int count = 0;
		while (result.hasNext()) {
			result.nextSolution();
			count++;
		}
		assertEquals(2, count);
	}

	@Test
	public void testComplexQuery() {
		testComplexQuery(defaultGraphIndex, defaultModel);
		testComplexQuery(namedGraphIndex, namedModel);
	}
	private void testComplexQuery(NumericIndex<Integer> index, Model model) {
		String example = "http://example.org/";
		String age = "http://example.org#age";
		String name = "http://example.org#name";
		String friend = "http://example.org#friend";

		Property ageProp = ResourceFactory.createProperty(age);
		Property nameProp = ResourceFactory.createProperty(name);
		Property friendProp = ResourceFactory.createProperty(friend);

		Resource one = ResourceFactory.createResource(example + "1");
		Resource two = ResourceFactory.createResource(example + "2");
		Resource three = ResourceFactory.createResource(example + "3");
		Resource four = ResourceFactory.createResource(example + "4");
		Resource five = ResourceFactory.createResource(example + "5");

		model.add(one, ageProp,
			ResourceFactory.createTypedLiteral(new Integer(20)));
		model.add(one, nameProp, ResourceFactory.createTypedLiteral("One"));
		model.add(one, friendProp, two);

		model.add(two, ageProp,
			ResourceFactory.createTypedLiteral(new Integer(25)));
		model.add(two, nameProp, ResourceFactory.createTypedLiteral("Two"));
		model.add(two, friendProp, one);
		model.add(two, friendProp, five);

		model.add(three, ageProp,
			ResourceFactory.createTypedLiteral(new Integer(29)));
		model.add(three, nameProp, ResourceFactory.createTypedLiteral("Three"));
		model.add(three, friendProp, five);

		model.add(four, ageProp,
			ResourceFactory.createTypedLiteral(new Integer(32)));
		model.add(four, nameProp, ResourceFactory.createTypedLiteral("Four"));

		model.add(five, ageProp,
			ResourceFactory.createTypedLiteral(new Integer(53)));
		model.add(five, nameProp, ResourceFactory.createTypedLiteral("Five"));
		model.add(five, friendProp, two);
		model.add(five, friendProp, three);
		String query = "SELECT ?x ?name ?y WHERE { "
			+ "?x <http://example.org#age> ?age . "
			+ "?x <http://example.org#name> ?name ."
			+ "OPTIONAL { " + "  ?x <http://example.org#friend> ?y ." + "}"
			+ "FILTER (!bound(?y)) " + "FILTER (?age > 25.0)  " + "}";

		try {
			assertEquals(5L, index.size());
		} catch (IndexException e) {
			fail();
		}
		QueryExecution qExec = QueryExecutionFactory.create(query,
			store.toDataset());
		ResultSet result = qExec.execSelect();
		assertTrue(result.hasNext());
		int counter = 0;
		while (result.hasNext()) {
			result.nextSolution();
			counter++;
		}
		assertEquals(1, counter);
	}

	@Test
	public void testRangeIterator() {
		testRangeIterator(defaultGraphIndex);
		testRangeIterator(namedGraphIndex);
	}

	private void testRangeIterator(NumericIndex<Integer> index) {
		try {
			index.add(createRecord(1));
			index.add(createRecord(2));
			index.add(createRecord(3));
			index.add(createRecord(4));
		} catch (IndexException e) {
			fail();
		}

		Iterator<Record<Integer>> it;

		it = index.iterator();
		assertTrue(it.hasNext());

		int counter = 0;
		while (it.hasNext()) {
			it.next();
			counter++;
		}
		assertEquals(4, counter);

		it = index.iterator(2, 3);
		counter = 0;
		while (it.hasNext()) {
			it.next();
			counter++;
		}
		assertEquals(2, counter);

		it = index.iterator(1, 3);
		counter = 0;
		while (it.hasNext()) {
			it.next();
			counter++;
		}
		assertEquals(3, counter);

		it = index.iterator(2, 10);
		counter = 0;
		while (it.hasNext()) {
			it.next();
			counter++;
		}
		assertEquals(3, counter);

		it = index.iterator(5, 10);
		counter = 0;
		assertFalse(it.hasNext());

		it = index.iterator(null, 2);
		counter = 0;
		while (it.hasNext()) {
			it.next();
			counter++;
		}
		assertEquals(2, counter);

		it = index.iterator(2, null);
		counter = 0;
		while (it.hasNext()) {
			it.next();
			counter++;
		}
		assertEquals(3, counter);
	}

	@Override
	protected IndexFactory<NumericIndex<Integer>, Integer> getIndexFactory() {
		NumericIndexFactory<Integer> f = new NumericIndexFactory.IntegerIndexFactory();
		Properties p = new Properties();
		p.setProperty(Constants.NUMBER_TYPE, NumberType.Integer.toString());
		p.setProperty(Constants.PROPERTY, AGE_URI);
		f.configure(p);
		return f;
	}

	@Override
	protected Record<Integer> createRecord(int seed) {
		Node key = Node.createURI(AGE_URI + seed);
		Record<Integer> record = Record.create(key, seed);
		return record;
	}

	@Override
	protected void doSetup() {
	}

	@Override
	protected boolean checkDeleted(NumericIndex<Integer> index, Graph graph, Node graphName) {
		String indexDir = IndexFactoryHelper.getIndexDirectory(graph, graphName);
		String predicate = index.getQuerier().getPredicate();
		String dirName = String.format("%s%cnumeric_%s", indexDir,
			File.separatorChar, FileUtil
			.encodeStringForFilename(predicate));
		return !new File(dirName).exists();
	}
}
