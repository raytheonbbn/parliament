package com.bbn.parliament.jena.query;

import com.bbn.parliament.jena.query.SolverUtil.BasicGraphSolverExecutor;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.sparql.core.BasicPattern;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.engine.main.StageGenerator;

public class KbStageGenerator implements StageGenerator {
	// Using OpExecutor is preferred.
	StageGenerator above = null;

	public KbStageGenerator(StageGenerator original) {
		above = original;
	}

	@Override
	public QueryIterator execute(BasicPattern pattern, QueryIterator input, ExecutionContext execCxt) {
		Graph graph = execCxt.getActiveGraph();

		return SolverUtil.solve(pattern, input, execCxt, graph, new BasicGraphSolverExecutor() {
			@Override
			public QueryIterator handle(BasicPattern p, QueryIterator i, ExecutionContext context) {
				return above.execute(p, i, context);
			}
		});
	}
}
