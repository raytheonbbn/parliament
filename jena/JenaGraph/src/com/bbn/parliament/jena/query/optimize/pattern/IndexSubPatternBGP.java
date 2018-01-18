package com.bbn.parliament.jena.query.optimize.pattern;

import java.util.HashSet;
import java.util.Set;
import com.bbn.parliament.jena.query.index.IndexPatternQuerier;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.sparql.core.BasicPattern;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.engine.QueryIterator;

public class IndexSubPatternBGP extends IndexSubPattern implements EstimablePattern {
	private IndexPatternQuerier querier;

	public IndexSubPatternBGP(IndexPatternQuerier querier) {
		this.querier = querier;
	}

	public IndexSubPatternBGP(IndexPatternQuerier querier, BasicPattern triples) {
		this.querier = querier;
		addAll(triples);
	}

	public IndexPatternQuerier getQuerier() {
		return querier;
	}

	/** {@inheritDoc} */
	@Override
	public long estimate() {
		return querier.estimate(this);
	}

	/** {@inheritDoc} */
	@Override
	public QueryIterator evaluate(QueryIterator input, ExecutionContext context) {
		return querier.query(this, input, context);
	}

	@Override
	public Set<Node> getVariables() {
		Set<Node> vars = new HashSet<>();
		for (Triple t : getList()) {
			if (t.getSubject().isVariable()) {
				vars.add(t.getSubject());
			}
			if (t.getPredicate().isVariable()) {
				vars.add(t.getPredicate());
			}
			if (t.getObject().isVariable()) {
				vars.add(t.getObject());
			}
		}
		return vars;
	}
}
