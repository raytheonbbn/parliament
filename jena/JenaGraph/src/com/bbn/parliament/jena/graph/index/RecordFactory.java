package com.bbn.parliament.jena.graph.index;

import java.util.List;

import org.apache.jena.graph.Triple;

/**
 * A factory for creating {@link Record}s. Each {@link Index} must provide a
 * <code>RecordFactory</code> that will be used to generate records from
 * <code>Triple</code>s.
 *
 * @author rbattle
 */
public interface RecordFactory<T> {
	/** Returns a new index record for the given triple, or null if none could be made. */
	public Record<T> createRecord(Triple triple);

	public List<Triple> getTripleMatchers();
}
