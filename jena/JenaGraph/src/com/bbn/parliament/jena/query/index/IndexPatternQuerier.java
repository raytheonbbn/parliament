package com.bbn.parliament.jena.query.index;

import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.engine.ExecutionContext;
import org.apache.jena.sparql.engine.QueryIterator;

/**
 * A handler for querying an index for a given pattern. The
 * <code>IndexPatternQuerier</code> is given a <code>BasicPattern</code> from the SPARQL
 * query to examine. It tells the query engine which part of the query that it handles.
 * When the query engine executes the query, the <code>IndexPatternQuerier</code> is fed
 * the part of the query that it said it could handle, along with the query input and
 * context up to that point. This allows for flexible indexes that can define their own
 * storage and access policies for data in the query.
 *
 * @author rbattle
 */
public interface IndexPatternQuerier {

	/**
	 * Estimate how selective this index would be for the given predicate. This method
	 * should return a number greater than or equal to the number of query solutions that
	 * would be returned if the query were to actually be executed.
	 *
	 * @param pattern the pattern of triples to examine.
	 * @return the upper bound on query results.
	 */
	public long estimate(BasicPattern pattern);

	/**
	 * Execute the given pattern.
	 *
	 * @param pattern the pattern to execute.
	 * @param input the query input.
	 * @param context the execution context.
	 * @return an iterator of bindings solving the pattern.
	 */
	public QueryIterator query(BasicPattern pattern, QueryIterator input,
		ExecutionContext context);

	/**
	 * Examine a pattern for handled content.
	 *
	 * @param pattern a pattern to examine.
	 * @return the sub-pattern that is handled by this instance.
	 */
	public BasicPattern examine(BasicPattern pattern);
}
