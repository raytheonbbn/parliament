package com.bbn.parliament.kb_graph.query.optimize;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.engine.optimizer.reorder.ReorderProc;

import com.bbn.parliament.kb_graph.KbGraph;

public class DefaultCountTransformation extends AbstractCountTransformation {
	public DefaultCountTransformation(KbGraph graph) {
		super(graph);
	}

	@Override
	public ReorderProc reorderIndexes(BasicPattern pattern) {
		return new ReorderProc() {
			@Override
			public BasicPattern reorder(BasicPattern bgp) {
				List<Triple> orderedTriples = orderByCounts(bgp.getList(), new ArrayList<Node>(), 1, false);
				BasicPattern result = new BasicPattern();
				for (Triple t : orderedTriples) {
					result.add(t);
				}
				return result;
			}
		};
	}
}