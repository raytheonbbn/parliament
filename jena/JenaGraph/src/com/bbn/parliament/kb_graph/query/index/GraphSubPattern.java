package com.bbn.parliament.kb_graph.query.index;

import org.apache.jena.graph.Graph;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.engine.ExecutionContext;
import org.apache.jena.sparql.engine.QueryIterator;

import com.bbn.parliament.kb_graph.KbGraph;
import com.bbn.parliament.kb_graph.query.SolverUtil;
import com.bbn.parliament.kb_graph.query.optimize.DefaultCountTransformation;
import com.bbn.parliament.kb_graph.query.optimize.pattern.IndexSubPatternBGP;
import com.bbn.parliament.kb_graph.union.KbUnionGraph;

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
