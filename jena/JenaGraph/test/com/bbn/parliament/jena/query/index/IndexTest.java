package com.bbn.parliament.jena.query.index;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.bbn.parliament.jena.graph.index.Index;
import com.bbn.parliament.jena.graph.index.IndexFactoryRegistry;
import com.bbn.parliament.jena.graph.index.IndexManager;
import com.bbn.parliament.jena.query.AbstractKbTestCase;
import com.bbn.parliament.jena.query.index.mock.MockIndex;
import com.bbn.parliament.jena.query.index.mock.MockIndexFactory;
import com.bbn.parliament.jena.query.index.mock.MockPropertyFunction;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;

public class IndexTest extends AbstractKbTestCase {
	private static MockIndexFactory factory;
	private static final Triple t = Triple.create(
		Node.createAnon(),
		Node.createURI("http://mock.example.org/asdf"),
		Node.createURI("http://example.org/object"));
	private static final Triple nonIndexed = Triple.create(
		Node.createAnon(),
		Node.createURI("http://example.org/foo"),
		Node.createURI("http://example.org/object"));

	@BeforeClass
	public static void addFactory() {
		factory = new MockIndexFactory();
		IndexFactoryRegistry.getInstance().register(factory);
	}

	@AfterClass
	public static void removeFactory() {
		IndexFactoryRegistry.getInstance().unregister(factory);
	}

	private MockIndex defaultIndex;
	private MockIndex namedGraphIndex;

	@Before
	public void createIndex() {
		List<Index<?>> indexes;
		indexes = IndexManager.getInstance().createAndRegisterAll(defaultGraph, null);
		defaultIndex = (MockIndex)indexes.get(0);

		indexes = IndexManager.getInstance().createAndRegisterAll(namedGraph, NAMED_GRAPH_URI);
		namedGraphIndex = (MockIndex)indexes.get(0);
	}

	@After
	public void removeIndex() {
		IndexManager.getInstance().unregister(defaultGraph, null, defaultIndex);
		IndexManager.getInstance().unregister(namedGraph, NAMED_GRAPH_URI, namedGraphIndex);
	}

	@Test
	public void testAdd() {
		assertFalse(defaultIndex.isAdded());
		defaultGraph.add(t);
		assertTrue(defaultIndex.isAdded());
	}

	@Test
	public void testAddNonIndexed() {
		assertFalse(defaultIndex.isAdded());
		defaultGraph.add(nonIndexed);
		assertFalse(defaultIndex.isAdded());
	}

	@Test
	public void testRemove() {
		assertFalse(defaultIndex.isRemoved());
		defaultGraph.delete(t);
		assertTrue(defaultIndex.isRemoved());
	}

	@Test
	public void testClear() {
		assertFalse(defaultIndex.isCleared());
		defaultGraph.clear();
		assertTrue(defaultIndex.isCleared());
	}

	@Ignore
	@Test
	public void testClose() {
		assertFalse(defaultIndex.isClosed());
		defaultGraph.close();
		assertTrue(defaultIndex.isClosed());
	}

	@SuppressWarnings("static-method")
	@Test
	public void testSimpleQuery() {
		defaultGraph.add(t);
		defaultGraph.add(nonIndexed);
		Query q = QueryFactory.create("SELECT * WHERE { ?s <" + MockPropertyFunction.URI + "> ?o }");
		QueryExecution exec = QueryExecutionFactory.create(q, dataset.toDataset());
		ResultSet rs = exec.execSelect();
		while (rs.hasNext()) {
			rs.next();
		}
		assertTrue(MockPropertyFunction.isCalled());
	}

	@SuppressWarnings("static-method")
	@Test
	public void testQuery() {
		defaultGraph.add(t);
		defaultGraph.add(nonIndexed);
		Query q = QueryFactory.create("SELECT * WHERE { ?s <" + MockPropertyFunction.URI + "> ?o . ?s <http://example.org/foo> ?y . }");
		QueryExecution exec = QueryExecutionFactory.create(q, dataset.toDataset());
		ResultSet rs = exec.execSelect();
		while (rs.hasNext()) {
			rs.next();
		}
		assertTrue(MockPropertyFunction.isCalled());
	}
}
