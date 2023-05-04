package com.bbn.parliament.kb_graph.query.optimize;

import java.util.List;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;


class OrderExpressionResult {
	private List<Triple> _expressionList;
	private long _estimate;
	private List<Node> _boundVariables;

	OrderExpressionResult(List<Triple> expressionList,
		long estimate, List<Node> boundVariables) {
		_expressionList = expressionList;
		_estimate = estimate;
		_boundVariables = boundVariables;
	}

	public List<Node> getBoundVariables() {
		return _boundVariables;
	}

	public long getEstimate() {
		return _estimate;
	}

	public List<Triple> getExpressionList() {
		return _expressionList;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("{\n");
		for (Triple triple : _expressionList) {
			builder.append(triple + "\n");
		}
		builder.append("}\n");
		return builder.toString();
	}
}
