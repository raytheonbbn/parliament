package com.bbn.parliament.kb_graph.query.optimize;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.engine.optimizer.reorder.ReorderProc;

import com.bbn.parliament.kb_graph.KbGraph;

public class UpdatedStaticCountTransformation extends AbstractCountTransformation {
	public UpdatedStaticCountTransformation(KbGraph graph) {
		super(graph);
	}

	@Override
	public ReorderProc reorderIndexes(BasicPattern pattern) {
		return new ReorderProc() {
			@Override
			public BasicPattern reorder(BasicPattern bgp) {
				List<Triple> orderedTriples = orderByCounts(bgp.getList(), new ArrayList<>(), 1, true);
				BasicPattern result = new BasicPattern();
				for (Triple t : orderedTriples) {
					result.add(t);
				}
				return result;
			}
		};
	}
}
