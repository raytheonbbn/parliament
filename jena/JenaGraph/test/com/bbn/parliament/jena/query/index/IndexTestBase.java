package com.bbn.parliament.jena.query.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.bbn.parliament.jena.graph.KbGraph;
import com.bbn.parliament.jena.graph.KbGraphFactory;
import com.bbn.parliament.jena.graph.KbGraphStore;
import com.bbn.parliament.jena.graph.index.Index;
import com.bbn.parliament.jena.graph.index.IndexException;
import com.bbn.parliament.jena.graph.index.IndexFactory;
import com.bbn.parliament.jena.graph.index.IndexFactoryRegistry;
import com.bbn.parliament.jena.graph.index.IndexManager;
import com.bbn.parliament.jena.graph.index.Record;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;

public abstract class IndexTestBase<T extends Index<I>, I> {

	protected abstract IndexFactory<T, I> getIndexFactory();

	protected abstract Record<I> createRecord(int seed);

	protected abstract void doSetup();

	protected T defaultGraphIndex;
	protected KbGraphStore store;
	protected T namedGraphIndex;
	protected static final Node namedGraphName = Node
		.createURI("http://example.org/testGraph");

	@Before
	public void setUp() {
		@SuppressWarnings("resource")
		KbGraph graph = KbGraphFactory.createDefaultGraph();
		store = new KbGraphStore(graph);
		store.initialize();

		@SuppressWarnings("resource")
		KbGraph namedGraph = KbGraphFactory.createNamedGraph();
		store.addGraph(namedGraphName, namedGraph);
		IndexFactory<T, I> f = getIndexFactory();
		IndexFactoryRegistry.getInstance().register(f);

		defaultGraphIndex = IndexManager.getInstance().createAndRegister(graph, null, f);
		namedGraphIndex = IndexManager.getInstance().createAndRegister(namedGraph, namedGraphName, f);

		try {
			defaultGraphIndex.clear();
			namedGraphIndex.clear();
		} catch (IndexException e) {
			fail();
		}
	}

	@After
	public void tearDown() {
		try {
			defaultGraphIndex.close();
			namedGraphIndex.close();
		} catch (IndexException e) {
			fail();
		}
		store.clear();
	}

	@Test
	public void testAddAndRemove() {
		testAddAndRemove(defaultGraphIndex);
		testAddAndRemove(namedGraphIndex);
	}

	private void testAddAndRemove(T index) {
		Record<I> r;

		try {
			assertEquals(0L, index.size());

			r = createRecord(0);

			// remove from an empty index should do nothing
			index.remove(r);
			assertEquals(0L, index.size());

			index.add(r);
			assertEquals(1L, index.size());

			// add same node twice
			index.add(r);
			assertEquals(1L, index.size());

			r = createRecord(1);
			index.add(r);
			assertEquals(2L, index.size());

			index.remove(r);
			assertEquals(1L, index.size());

			// delete same node twice
			index.remove(r);
			assertEquals(1L, index.size());
		} catch (IndexException e) {
			fail();
		}
	}

	@Test
	public void testOpenClose() {
		testOpenClose(defaultGraphIndex);
		testOpenClose(namedGraphIndex);
	}

	private void testOpenClose(T index) {
		try {
			assertEquals(0, index.size());

			Record<I> record = createRecord(0);
			index.add(record);
			assertEquals(1, index.size());

			index.close();

			index.open();
			assertEquals(1, index.size());

			Record<I> record2 = createRecord(1);
			index.add(record2);
			assertEquals(2, index.size());

			index.remove(record);
			assertEquals(1, index.size());
		} catch (IndexException e) {
			fail();
		}
	}

	@Test
	public void testIterator() {
		testIterator(defaultGraphIndex);
		testIterator(namedGraphIndex);
	}

	private void testIterator(T index) {
		Iterator<Record<I>> it;

		it = index.iterator();
		assertFalse(it.hasNext());

		List<Record<I>> records = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			Record<I> record = createRecord(i);
			records.add(record);
			try {
				index.add(record);
			} catch (IndexException e) {
				fail();
			}
		}

		it = index.iterator();
		assertTrue(it.hasNext());
		int i = 0;
		while (i <= records.size() && it.hasNext()) {
			Record<I> r = it.next();
			assertTrue(records.contains(r));
			i++;
		}
		assertEquals(records.size(), i);
	}

	@Test
	public void testAddClosed() {
		testAddClosed(defaultGraphIndex);
		testAddClosed(namedGraphIndex);
	}

	private void testAddClosed(T index) {
		try {
			index.close();
		} catch (IndexException e) {
			fail("Could not close index");
		}
		boolean threw = false;
		try {
			index.add(createRecord(0));
			fail("Should not add to closed index");
		} catch (Exception e) {
			threw = true;
		}
		assertTrue(threw);
	}

	@Test
	public void testRemoveClosed() {
		testRemoveClosed(defaultGraphIndex);
		testRemoveClosed(namedGraphIndex);
	}

	private void testRemoveClosed(T index) {
		Record<I> record = createRecord(0);
		try {
			index.add(record);
			index.close();
		} catch (IndexException e) {
			fail("Could not setup test");
		}
		boolean threw = false;
		try {
			index.remove(record);
			fail("Should not remove from closed index");
		} catch (Exception e) {
			threw = true;
		}
		assertTrue(threw);
	}

	@Test
	public void testIteratorClosed() {
		testIteratorClosed(defaultGraphIndex);
		testIteratorClosed(namedGraphIndex);
	}

	private void testIteratorClosed(T index) {
		Record<I> record = createRecord(0);
		try {
			index.add(record);
			index.close();
		} catch (IndexException e) {
			fail("Could not setup test");
		}
		boolean threw = false;
		try {
			index.iterator();
			fail("Should not iterate over closed index");
		} catch (Exception e) {
			threw = true;
		}
		assertTrue(threw);
	}

	@Test
	public void testDelete() {
		testDelete(defaultGraphIndex, store.getDefaultGraph(), null);
		testDelete(namedGraphIndex, store.getGraph(namedGraphName), namedGraphName);
	}

	private void testDelete(T index, Graph graph, Node graphName) {
		List<Record<I>> records = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			Record<I> record = createRecord(i);
			records.add(record);
			try {
				index.add(record);
			} catch (IndexException e) {
				fail("Could not create test records");
			}
		}
		try {
			index.close();
			index.delete();
		} catch (IndexException e) {
			fail("Could not delete index");
			return;
		}
		assertTrue("Not all resources deleted", checkDeleted(index, graph, graphName));
	}

	protected abstract boolean checkDeleted(T index, Graph graph, Node graphName);

	@Test
	public void testDeleteOpen() {
		testDeleteOpen(defaultGraphIndex);
		testDeleteOpen(namedGraphIndex);
	}

	private void testDeleteOpen(T index) {
		List<Record<I>> records = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			Record<I> record = createRecord(i);
			records.add(record);
			try {
				index.add(record);
			} catch (IndexException e) {
				fail("Could not create test records");
			}
		}
		Exception exp = null;
		try {
			index.delete();
			fail("Should throw exception");
		} catch (Exception e) {
			exp = e;
		}
		assertNotNull(exp);
	}

	@Test
	public void testClear() {
		testClear(defaultGraphIndex);
		testClear(namedGraphIndex);
	}

	private void testClear(T index) {
		Record<I> r;

		try {
			r = createRecord(0);
			index.add(r);

			r = createRecord(1);
			index.add(r);
		} catch (IndexException e) {
			fail("Could not create test records");
		}
		assertEquals(2, index.size());
		try {
			index.clear();
		} catch (IndexException e) {
			fail("Could not clear index");
		}
		assertEquals(0, index.size());
	}
}
