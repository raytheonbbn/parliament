package com.bbn.parliament.jena.query.index;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public abstract class IndexTestMethods<T extends Index<I>, I> implements AutoCloseable {
	public enum IndexUnderTest { DEFAULT_GRAPH, NAMED_GRAPH }

	private static final Node NAMED_GRAPH_NAME = Node.createURI("http://example.org/testGraph");
	private static final File KB_DIR = new File("test-kb-data");

	private KbGraphStore store;
	private Model defaultModel;
	private Model namedModel;
	private IndexFactory<T, I> indexFactory;
	private T defaultGraphIndex;
	private T namedGraphIndex;

	protected abstract IndexFactory<T, I> getIndexFactory();
	protected abstract Record<I> createRecord(int seed);
	protected abstract void doSetup();
	protected abstract boolean checkDeleted(T index, Graph graph, Node graphName);

	// =============== Setup/Teardown Methods ===============

	// Call from @BeforeEach
	public IndexTestMethods() {
		if (KB_DIR.exists() && !KB_DIR.isDirectory()) {
			throw new RuntimeException(String.format(
				"%1$s exists but is not a directory%n", KB_DIR.getAbsolutePath()));
		} else if (KB_DIR.isDirectory() && KB_DIR.listFiles().length > 0) {
			throw new RuntimeException(String.format(
				"%1$s is a non-empty directory%n", KB_DIR.getAbsolutePath()));
		}

		@SuppressWarnings("resource")
		KbGraph defaultGraph = KbGraphFactory.createDefaultGraph();
		store = new KbGraphStore(defaultGraph);
		store.initialize();

		@SuppressWarnings("resource")
		KbGraph namedGraph = KbGraphFactory.createNamedGraph();
		store.addGraph(NAMED_GRAPH_NAME, namedGraph);

		defaultModel = ModelFactory.createModelForGraph(defaultGraph);
		namedModel = ModelFactory.createModelForGraph(namedGraph);

		indexFactory = getIndexFactory();
		IndexFactoryRegistry.getInstance().register(indexFactory);

		defaultGraphIndex = IndexManager.getInstance().createAndRegister(defaultGraph, null, indexFactory);
		namedGraphIndex = IndexManager.getInstance().createAndRegister(namedGraph, NAMED_GRAPH_NAME, indexFactory);

		try {
			defaultGraphIndex.open();
			namedGraphIndex.open();

			defaultGraphIndex.clear();
			namedGraphIndex.clear();
		} catch (IndexException e) {
			fail(e.getMessage());
		}
	}

	// Call from @AfterEach
	@Override
	public void close() {
		store.clear();

		store = null;
		defaultModel = null;
		namedModel = null;
		indexFactory = null;
		defaultGraphIndex = null;
		namedGraphIndex = null;

		if (KB_DIR.isDirectory()) {
			if (KB_DIR.listFiles().length > 0) {
				throw new RuntimeException(String.format(
					"Unable to delete %1$s (directory is not empty)%n", KB_DIR.getAbsolutePath()));
			} else {
				KB_DIR.delete();
			}
		}
	}

	// =============== Access Methods ===============

	public Dataset getDataset() {
		return store.toDataset();
	}

	public T getIndex(IndexUnderTest iut) {
		return (iut == IndexUnderTest.DEFAULT_GRAPH)
			? defaultGraphIndex
			: namedGraphIndex;
	}

	public Graph getGraph(IndexUnderTest iut) {
		return (iut == IndexUnderTest.DEFAULT_GRAPH)
			? store.getDefaultGraph()
			: store.getGraph(NAMED_GRAPH_NAME);
	}

	@SuppressWarnings("static-method")
	public Node getGraphName(IndexUnderTest iut) {
		return (iut == IndexUnderTest.DEFAULT_GRAPH)
			? null
			: NAMED_GRAPH_NAME;
	}

	public Model getModel(IndexUnderTest iut) {
		return (iut == IndexUnderTest.DEFAULT_GRAPH)
			? defaultModel
			: namedModel;
	}

	// =============== Test Methods ===============

	// Test method
	public void testAddAndRemove(T index) {
		try {
			assertEquals(0L, index.size());

			Record<I> r = createRecord(0);

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
			fail(e.getMessage());
		}
	}

	// Test method
	public void testOpenClose(T index) {
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
			fail(e.getMessage());
		}
	}

	// Test method
	public void testIterator(T index) {
		assertFalse(index.iterator().hasNext());

		Set<Record<I>> records = IntStream.range(0, 5)
			.mapToObj(this::createRecord)
			.collect(Collectors.toSet());
		try {
			records.forEach(record -> index.add(record));
		} catch (IndexException ex) {
			fail(ex.getMessage());
		}

		Iterator<Record<I>> it = index.iterator();
		assertTrue(it.hasNext());
		int i = 0;
		while (i <= records.size() && it.hasNext()) {
			Record<I> r = it.next();
			assertTrue(records.contains(r));
			++i;
		}
		assertEquals(records.size(), i);
	}

	// Test method
	public void testAddClosed(T index) {
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

	// Test method
	public void testRemoveClosed(T index) {
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

	// Test method
	public void testIteratorClosed(T index) {
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

	// Test method
	public void testDelete(T index, Graph graph, Node graphName) {
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
		assertTrue(checkDeleted(index, graph, graphName), "Not all resources deleted");
	}

	// Test method
	public void testDeleteOpen(T index) {
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

	// Test method
	public void testClear(T index) {
		try {
			Record<I> r = createRecord(0);
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
