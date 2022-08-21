package com.bbn.parliament.jena.query.index;

import com.bbn.parliament.jena.graph.KbGraph;
import com.bbn.parliament.jena.graph.union.KbUnionGraph;
import com.bbn.parliament.jena.query.SolverUtil;
import com.bbn.parliament.jena.query.optimize.DefaultCountTransformation;
import com.bbn.parliament.jena.query.optimize.pattern.IndexSubPatternBGP;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.sparql.core.BasicPattern;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.engine.QueryIterator;

public final class GraphSubPattern extends IndexSubPatternBGP {
	private Graph graph;

	public GraphSubPattern(Graph graph) {
		super(null);
		this.graph = graph;
	}

	public GraphSubPattern(Graph graph, BasicPattern other) {
		this(graph);
		addAll(other);
	}

	public Graph getGraph() {
		return graph;
	}

	@Override
	public long estimate() {
		if (graph instanceof KbGraph kbGraph) {
			return new DefaultCountTransformation(kbGraph).estimateSelectivity(this);
		} else if (graph instanceof KbUnionGraph union) {
			Graph l = (Graph)union.getL();
			Graph r = (Graph)union.getR();
			BasicPattern pattern = BasicPattern.wrap(getList());
			GraphSubPattern lp = new GraphSubPattern(l, pattern);
			GraphSubPattern rp = new GraphSubPattern(r, pattern);

			long lest = lp.estimate();
			long rest = rp.estimate();

			if (lest == -1) {
				return rest;
			} else if (rest == -1) {
				return lest;
			}
			return (lest < rest) ? lest : rest;
		}
		return -1;
	}

	/** {@inheritDoc} */
	@Override
	public QueryIterator evaluate(QueryIterator input, ExecutionContext context) {
		return SolverUtil.solve(this, input, context, SolverUtil.DEFAULT_SOLVER_EXECUTOR);
	}
}
