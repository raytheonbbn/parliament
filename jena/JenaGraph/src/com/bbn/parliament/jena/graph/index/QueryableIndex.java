package com.bbn.parliament.jena.graph.index;

import java.util.Iterator;

import org.apache.jena.graph.Node;

import com.bbn.parliament.jena.query.index.IndexPatternQuerier;
import com.bbn.parliament.jena.query.index.IndexPatternQuerierManager;
import com.bbn.parliament.jena.query.index.QueryCache;
import com.bbn.parliament.jena.query.index.pfunction.IndexPropertyFunction;

/**
 * An extension of <code>Index</code> that adds methods for accessing data.
 * <br><br>
 * If the <code>Index</code> is not going to be accessed via property functions, an
 * {@link IndexPatternQuerier} needs to be implemented and associated with the
 * {@link QueryableIndex} through the {@link IndexPatternQuerierManager}.
 * <br><br>
 * The {@link QueryCache} is used to maintain an in-memory cache of objects that are
 * defined at query time, but may be needed over several iterations of the query. Binding
 * objects are passed along to each stage of the query execution and they contain
 * mappings of variables to the nodes that correspond to answers of the query. In some
 * cases, however, you need to store more information than just a node. For instance, a
 * query may define a floating spatial region and check items for their inclusion in that
 * region. The <code>Index</code> can assign a blank node to that region, map that node
 * to the binding, and also map the node to the actual region in the cache. As items are
 * bound and passed to the <code>Index</code>, it can access the region through the query
 * cache and check to see if the bound items intersect it.
 *
 * @author rbattle
 *
 * @param <T> the type of object to index
 *
 * @see IndexPatternQuerier
 * @see IndexPatternQuerierManager
 * @see IndexPropertyFunction
 */
public interface QueryableIndex<T> extends Index<T> {
	/**
	 * Find a record in the index.
	 *
	 * @param node the key of the record to find
	 * @return the record from the index.
	 */
	public Record<T> find(Node node);

	/**
	 * Query for all records containing a specific value.
	 *
	 * @param value the value to match
	 * @return an iterator of records matching the value.
	 */
	public Iterator<Record<T>> query(T value);

	/** Get the query cache. */
	public QueryCache<T> getQueryCache();
}
