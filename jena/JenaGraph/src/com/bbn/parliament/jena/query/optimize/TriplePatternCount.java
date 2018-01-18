package com.bbn.parliament.jena.query.optimize;

import java.util.ArrayList;
import java.util.List;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

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
		return triple.toString() + "Count: " + count + " Estimate: " + estimate;
	}
}
