package com.bbn.parliament.kb_graph.query.optimize;

import com.bbn.parliament.kb_graph.KbGraph;

public abstract class AbstractKbGraphReorderTransformation extends AbstractGraphReorderTransformation {
	public AbstractKbGraphReorderTransformation(KbGraph graph) {
		super(graph);
	}

	protected KbGraph getGraph() {
		return (KbGraph)graph;
	}
}
