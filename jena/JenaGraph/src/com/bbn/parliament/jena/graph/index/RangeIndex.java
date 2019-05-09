package com.bbn.parliament.jena.graph.index;

import java.util.Iterator;

/**
 * A {@link QueryableIndex} that supports iterating over a range of values.
 *
 * The primary use of the <code>RangeIndex</code> is to support access to the
 * index via a range expression in a FILTER query.
 *
 * @param <T> the type of object to index
 * @author rbattle
 */
public interface RangeIndex<T extends Comparable<T>> extends QueryableIndex<T> {
	/**
	 * Get an iterator of indexed values within the specified range (inclusive).
	 *
	 * @param start the start of the range (can be null to include all values
	 *        less than the end)
	 * @param end the end of the range (can be null to include all values greater
	 *        than the start)
	 * @return the indexed values.
	 */
	public Iterator<Record<T>> iterator(T start, T end);

}
