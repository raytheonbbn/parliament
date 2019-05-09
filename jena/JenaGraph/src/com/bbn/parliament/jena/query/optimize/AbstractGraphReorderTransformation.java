package com.bbn.parliament.jena.query.optimize;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.sparql.core.BasicPattern;
import com.hp.hpl.jena.sparql.engine.optimizer.reorder.ReorderTransformation;

public abstract class AbstractGraphReorderTransformation implements ReorderTransformation {
	protected Graph graph;

	public AbstractGraphReorderTransformation(Graph graph) {
		this.graph = graph;
	}

	@Override
	public final BasicPattern reorder(BasicPattern pattern) {
		return reorderIndexes(pattern).reorder(pattern);
	}
}
