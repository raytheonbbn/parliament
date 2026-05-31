package com.bbn.parliament.kb_graph.query.optimize;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

class TriplePatternCount {
	public Triple triple;
	public long count;
	public long estimate;
	public List<Node> unboundVariables = new ArrayList<>();

	TriplePatternCount(Triple triple, long count) {
		this.triple = triple;
		this.count = count;
		estimate = 0;
		unboundVariables = OptimizeUtil.getVariables(triple);
	}

	@Override
	public String toString() {
		return "%1$s, Count: %2$d, Estimate: %3$d".formatted(triple, count, estimate);
	}
}
