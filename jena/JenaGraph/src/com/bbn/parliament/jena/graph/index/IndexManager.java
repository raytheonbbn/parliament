package com.bbn.parliament.jena.graph.index;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jena.graph.index.IndexFactory.IndexFactoryHelper;
import com.bbn.parliament.jena.util.FileUtil;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.GraphEventManager;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

/**
 * A registry of managed indexes. The <code>IndexManager</code> maintains a registry of
 * {@link Graph}s to {@link Index}es and <code>Index</code>es to {@link IndexFactory}s.
 * The query engine uses the manager to determine whether indexes are present for a given
 * graph and adjusts the query execution accordingly.
 *
 * @author rbattle
 */
public class IndexManager {
	private static final Logger LOG = LoggerFactory.getLogger(IndexManager.class);

	private static class IndexRegistryHolder {
		private static final IndexManager INSTANCE = new IndexManager();
	}

	/** Get the singleton instance of the <code>IndexManager</code>. */
	public static IndexManager getInstance() {
		return IndexRegistryHolder.INSTANCE;
	}

	private Map<Graph, List<IndexListener<?>>> registry;
	private Map<Index<?>, IndexFactory<?, ?>> indexToFactory;
	private Object lock = new Object();

	private IndexManager() {
		registry = new HashMap<>();
		indexToFactory = new HashMap<>();
	}

	/** Get the number of managed indexes. */
	public int size() {
		synchronized (lock) {
			return registry.size();
		}
	}

	/**
	 * Create, register, and open all available indexes. Indexes are created from all the
	 * IndexFactories registered with the {@link IndexFactoryRegistry}.
	 *
	 * @param graph a graph
	 * @param graphName the name of the graph.
	 * @return the new indexes.
	 */
	public List<Index<?>> createAndRegisterAll(Graph graph, Node graphName) {
		List<Index<?>> indexes = new ArrayList<>();
		for (IndexFactory<?, ?> factory : IndexFactoryRegistry.getInstance()
			.getFactories()) {
			Index<?> index = createAndRegister(graph, graphName, factory);
			indexes.add(index);
		}
		return indexes;
	}

	/**
	 * Create, register, and open an index.
	 *
	 * @param <I> The concrete implementation of the index
	 * @param <T> The type of data that is indexed
	 * @param graph a graph
	 * @param graphName the name of the graph
	 * @param indexFactory a factory that can create indexes
	 * @return the new index.
	 */
	public <I extends Index<T>, T> I createAndRegister(Graph graph,
		Node graphName, IndexFactory<I, T> indexFactory) {
		I index = indexFactory.createIndex(graph, graphName);
		register(graph, graphName, indexFactory, index);
		return index;
	}

	/**
	 * Register an index. This also opens the index if it is closed.
	 *
	 * @param <I> The concrete implementation of the index
	 * @param <T> The type of data that is indexed
	 * @param graph a graph
	 * @param graphName the name of the graph
	 * @param indexFactory a factory that can create indexes
	 * @param index the index to register.
	 */
	public <I extends Index<T>, T> void register(Graph graph, Node graphName,
		IndexFactory<I, T> indexFactory, I index) {
		synchronized (lock) {
			List<IndexListener<?>> indexes = registry.get(graph);
			if (null == indexes) {
				indexes = new ArrayList<>();
				registry.put(graph, indexes);
			}
			IndexListener<T> l = new IndexListener<>(index);
			indexes.add(l);
			graph.getEventManager().register(l);
			indexToFactory.put(index, indexFactory);
			index.register(graph, graphName);

			if (index.isClosed()) {
				try {
					index.open();
				} catch (IndexException e) {
					LOG.error("Error while opening index", e);
				}
			}
		}
	}

	/**
	 * Unregister an index. This closes and deletes the index. If the indexes directory is
	 * empty, it is deleted.
	 *
	 * @param <T> the type of index
	 * @param graph a graph
	 * @param graphName the name of the graph
	 * @param index the index to unregister
	 */
	public <T> void unregister(Graph graph, Node graphName, Index<T> index) {
		synchronized (lock) {
			List<IndexListener<?>> indexes = registry.get(graph);
			if (null == indexes) {
				return;
			}
			IndexListener<T> l = new IndexListener<>(index);
			if (!indexes.contains(l)) {
				return;
			}
			graph.getEventManager().unregister(l);
			indexes.remove(l);
			if (indexes.isEmpty()) {
				registry.remove(graph);
			}
			if (!index.isClosed()) {
				try {
					index.close();
				} catch (IndexException e) {
					LOG.error("Error while closing index", e);
					return;
				}
			}
			index.unregister(graph, graphName);
			try {
				index.delete();
			} catch (IndexException e) {
				LOG.error("Error while deleting index", e);
				return;
			}
			File indexesDir = new File(
				IndexFactoryHelper
				.getIndexDirectory(graph, graphName));
			String[] files = indexesDir.list();
			if (null != files && 0 == files.length) {
				FileUtil.delete(indexesDir);
			}
			indexToFactory.remove(index);
		}
	}

	/**
	 * Returns <code>true</code> if the given graph has registered indexes,
	 * <code>false</code> otherwise
	 */
	public boolean hasIndexes(Graph graph) {
		synchronized (lock) {
			return registry.containsKey(graph);
		}
	}

	/** Get the index factory for the given index. */
	public IndexFactory<?, ?> getIndexFactory(Index<?> index) {
		synchronized (lock) {
			return indexToFactory.get(index);
		}
	}

	/**
	 * Get the indexes registered for the given graph. If there are none, returns an empty
	 * list.
	 */
	public List<Index<?>> getIndexes(Graph graph) {
		synchronized (lock) {
			List<IndexListener<?>> listeners = registry.get(graph);
			if (null == listeners) {
				return Collections.emptyList();
			}
			List<Index<?>> indexes = new ArrayList<>(listeners.size());
			for (IndexListener<?> l : listeners) {
				indexes.add(l.getIndex());
			}
			return indexes;
		}
	}

	/**
	 * Rebuild all indexes for the given graph. This iterates through all the triples in
	 * the graph.
	 */
	public void rebuild(Graph graph) {
		List<Index<?>> indexes = getIndexes(graph);
		if (indexes.isEmpty()) {
			return;
		}
		for (Index<?> index : indexes) {
			if (index.isClosed()) {
				try {
					index.open();
				} catch (IndexException e) {
					LOG.error("Error opening index", e);
					return;
				}
			}
			try {
				index.clear();
			} catch (IndexException e) {
				LOG.error("Error clearing index", e);
				return;
			}
			for (Triple triple : index.getRecordFactory().getTripleMatchers()) {
				ExtendedIterator<Triple> it = graph.find(triple);
				GraphEventManager eventManager = graph.getEventManager();
				try {
					while (it.hasNext()) {
						eventManager.notifyAddTriple(graph, it.next());
					}
				} catch (Throwable t) {
					throw new RuntimeException(t);
				} finally {
					it.close();
				}
			}
		}
	}

	/**
	 * Unregister all indexes for the given graph.
	 *
	 * @param graph a graph
	 * @param graphName the name of the graph
	 */
	public void unregisterAll(Graph graph, Node graphName) {
		List<Index<?>> indexes = getIndexes(graph);
		for (Index<?> index : indexes) {
			unregister(graph, graphName, index);
		}
	}

	/** Close all open indexes for the given graph */
	public void closeAll(Graph graph) {
		List<Index<?>> indexes = getIndexes(graph);
		for (Index<?> index : indexes) {
			if (!index.isClosed()) {
				try {
					index.close();
				} catch (IndexException e) {
					LOG.error("Error while closing index", e);
				}
			}
		}
	}

	/** Open all closed indexes for the given graph */
	public void openAll(Graph graph) {
		List<Index<?>> indexes = getIndexes(graph);
		for (Index<?> index : indexes) {
			if (index.isClosed()) {
				try {
					index.open();
				} catch (IndexException e) {
					LOG.error("Error opening index", e);
				}
			}
		}
	}

	public void flush(Graph graph) {
		List<Index<?>> indexes = getIndexes(graph);
		for (Index<?> index : indexes) {
			if (!index.isClosed()) {
				try {
					index.flush();
				} catch (IndexException e) {
					LOG.error("Error flushing index", e);
				}
			}
		}
	}

	public Collection<Graph> getGraphs() {
		return Collections.unmodifiableCollection(registry.keySet());
	}
}
