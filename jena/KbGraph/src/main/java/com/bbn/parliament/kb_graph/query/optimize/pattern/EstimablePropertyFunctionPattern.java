package com.bbn.parliament.kb_graph.query.optimize.pattern;

import java.util.List;
import java.util.Map;

import org.apache.jena.graph.Node;

import com.bbn.parliament.kb_graph.query.index.operand.Operand;
import com.bbn.parliament.kb_graph.query.index.pfunction.EstimableIndexPropertyFunction;

public class EstimablePropertyFunctionPattern<T> extends IndexSubPatternPropertyFunction<T> implements EstimablePattern {
	private Map<Node, Operand<T>> operands;

	public EstimablePropertyFunctionPattern(EstimableIndexPropertyFunction<T> function,
		Node predicate, List<Node> subjects, List<Node> objects, Map<Node, Operand<T>> operands) {
		super(function, predicate, subjects, objects, operands);
		this.operands = operands;
	}

	@Override
	public long estimate() {
		return ((EstimableIndexPropertyFunction<T>)function).estimate(subjects, objects, operands);
	}
}
