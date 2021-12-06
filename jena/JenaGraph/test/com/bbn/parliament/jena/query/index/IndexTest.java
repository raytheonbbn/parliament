package com.bbn.parliament.jena.query.index;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.bbn.parliament.jena.TestingDataset;
import com.bbn.parliament.jena.graph.index.Index;
import com.bbn.parliament.jena.graph.index.IndexFactoryRegistry;
import com.bbn.parliament.jena.graph.index.IndexManager;
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

public class IndexTest {
	private static TestingDataset dataset;
	private static MockIndexFactory factory;
	private static final Triple t = Triple.create(
		Node.createAnon(),
		Node.createURI("http://mock.example.org/asdf"),
		Node.createURI("http://example.org/object"));
	private static final Triple nonIndexed = Triple.create(
		Node.createAnon(),
		Node.createURI("http://example.org/foo"),
		Node.createURI("http://example.org/object"));

	@BeforeAll
	public static void beforeAll() {
		dataset = new TestingDataset();
		factory = new MockIndexFactory();
		IndexFactoryRegistry.getInstance().register(factory);
	}

	@AfterAll
	public static void afterAll() {
		dataset.clear();
		IndexFactoryRegistry.getInstance().unregister(factory);
	}

	private MockIndex defaultIndex;
	private MockIndex namedGraphIndex;

	@BeforeEach
	public void beforeEach() {
		List<Index<?>> indexes;
		indexes = IndexManager.getInstance().createAndRegisterAll(dataset.getDefaultGraph(), null);
		defaultIndex = (MockIndex)indexes.get(0);

		indexes = IndexManager.getInstance().createAndRegisterAll(dataset.getNamedGraph(), TestingDataset.NAMED_GRAPH_URI);
		namedGraphIndex = (MockIndex)indexes.get(0);
	}

	@AfterEach
	public void afterEach() {
		dataset.reset();
		IndexManager.getInstance().unregister(dataset.getDefaultGraph(), null, defaultIndex);
		IndexManager.getInstance().unregister(dataset.getNamedGraph(), TestingDataset.NAMED_GRAPH_URI, namedGraphIndex);
	}

	@Test
	public void testAdd() {
		assertFalse(defaultIndex.isAdded());
		dataset.getDefaultGraph().add(t);
		assertTrue(defaultIndex.isAdded());
	}

	@Test
	public void testAddNonIndexed() {
		assertFalse(defaultIndex.isAdded());
		dataset.getDefaultGraph().add(nonIndexed);
		assertFalse(defaultIndex.isAdded());
	}

	@Test
	public void testRemove() {
		assertFalse(defaultIndex.isRemoved());
		dataset.getDefaultGraph().delete(t);
		assertTrue(defaultIndex.isRemoved());
	}

	@Test
	public void testClear() {
		assertFalse(defaultIndex.isCleared());
		dataset.getDefaultGraph().clear();
		assertTrue(defaultIndex.isCleared());
	}

	@Disabled
	@Test
	public void testClose() {
		assertFalse(defaultIndex.isClosed());
		dataset.getDefaultGraph().close();
		assertTrue(defaultIndex.isClosed());
	}

	@SuppressWarnings("static-method")
	@Test
	public void testSimpleQuery() {
		dataset.getDefaultGraph().add(t);
		dataset.getDefaultGraph().add(nonIndexed);
		Query q = QueryFactory.create("SELECT * WHERE { ?s <" + MockPropertyFunction.URI + "> ?o }");
		QueryExecution exec = QueryExecutionFactory.create(q, dataset.getGraphStore().toDataset());
		ResultSet rs = exec.execSelect();
		while (rs.hasNext()) {
			rs.next();
		}
		assertTrue(MockPropertyFunction.isCalled());
	}

	@SuppressWarnings("static-method")
	@Test
	public void testQuery() {
		dataset.getDefaultGraph().add(t);
		dataset.getDefaultGraph().add(nonIndexed);
		Query q = QueryFactory.create("SELECT * WHERE { ?s <" + MockPropertyFunction.URI + "> ?o . ?s <http://example.org/foo> ?y . }");
		QueryExecution exec = QueryExecutionFactory.create(q, dataset.getGraphStore().toDataset());
		ResultSet rs = exec.execSelect();
		while (rs.hasNext()) {
			rs.next();
		}
		assertTrue(MockPropertyFunction.isCalled());
	}
}
