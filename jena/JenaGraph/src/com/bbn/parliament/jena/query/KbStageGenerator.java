package com.bbn.parliament.jena.query;

import org.apache.jena.graph.Graph;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.engine.ExecutionContext;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.engine.main.StageGenerator;

import com.bbn.parliament.jena.query.SolverUtil.BasicGraphSolverExecutor;

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
