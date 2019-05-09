package com.bbn.parliament.jena.query.index;

import java.util.HashMap;
import java.util.Map;

import com.bbn.parliament.jena.graph.index.Index;

/**
 * A register of managed {@link IndexPatternQuerier}s. The
 * <code>IndexPatternQuerierManager</code> maintains a mapping of {@link Index}
 * to <code>IndexPatternQuerier</code>s. The query engine uses this manager to
 * determine whether a query pattern can be handled by an <code>Index</code>.
 *
 * @author rbattle
 */
public class IndexPatternQuerierManager {
	private static final IndexPatternQuerierManager INSTANCE = new IndexPatternQuerierManager();

	/**
	 * Get the instance.
	 *
	 * @return the instance.
	 */
	public static IndexPatternQuerierManager getInstance() {
		return INSTANCE;
	}

	private Map<Index<?>, IndexPatternQuerier> registry;
	private Object lock = new Object();

	private IndexPatternQuerierManager() {
		registry = new HashMap<>();
	}

	/**
	 * Get the number of registered items.
	 *
	 * @return the size.
	 */
	public int size() {
		synchronized (lock) {
			return registry.size();
		}
	}

	/**
	 * Register a querier to an index.
	 *
	 * @param <T> the type of indexed data.
	 * @param index the index to query.
	 * @param querier a querier for the index.
	 */
	public <T> void register(Index<T> index, IndexPatternQuerier querier) {
		synchronized (lock) {
			registry.put(index, querier);
		}
	}

	/**
	 * Unregister an index and it's associated querier.
	 *
	 * @param <T> the type of indexed data.
	 * @param index an index.
	 */
	public <T> void unregister(Index<T> index) {
		synchronized (lock) {
			registry.remove(index);
		}
	}

	/**
	 * Get the querier registered to the index
	 *
	 * @param index the index.
	 * @return a querier; or <code>null</code> if the index is not managed.
	 */
	public IndexPatternQuerier get(Index<?> index) {
		synchronized (lock) {
			IndexPatternQuerier querier = registry.get(index);
			return querier;
		}
	}
}
