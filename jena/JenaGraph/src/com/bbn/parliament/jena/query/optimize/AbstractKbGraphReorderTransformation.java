package com.bbn.parliament.jena.query.optimize;

import com.bbn.parliament.jena.graph.KbGraph;

public abstract class AbstractKbGraphReorderTransformation extends AbstractGraphReorderTransformation {
	public AbstractKbGraphReorderTransformation(KbGraph graph) {
		super(graph);
	}

	protected KbGraph getGraph() {
		return (KbGraph)graph;
	}
}
