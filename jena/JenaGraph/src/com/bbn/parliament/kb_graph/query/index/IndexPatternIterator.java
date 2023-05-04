package com.bbn.parliament.kb_graph.query.index;

import java.util.List;

import org.apache.jena.sparql.engine.ExecutionContext;
import org.apache.jena.sparql.engine.QueryIterator;

import com.bbn.parliament.kb_graph.query.optimize.pattern.IndexSubPattern;
import com.bbn.parliament.kb_graph.util.AbstractIteratorWithChildren;

/**
 * QueryIterator for Subqueries. Each subquery is iterated over. The binding
 * from one subquery execution is fed into the next. The resulting bindings are
 * aggregated and returned.
 *
 * @author rbattle
 */
public class IndexPatternIterator extends AbstractIteratorWithChildren {
	private List<IndexSubPattern> subPatterns;

	public IndexPatternIterator(IndexPattern pattern, QueryIterator input, ExecutionContext context) {
		super(input, context);
		this.subPatterns = pattern.getSubPatterns();
	}

	@Override
	protected int sizeOfChildren() {
		return subPatterns.size();
	}

	@Override
	protected QueryIterator createChildIterator(int index, QueryIterator input) {
		IndexSubPattern pattern = subPatterns.get(index);
		return pattern.evaluate(input, getExecContext());
	}
}
