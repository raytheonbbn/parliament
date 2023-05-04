package com.bbn.parliament.kb_graph.query.optimize.pattern;

import java.util.List;
import java.util.Map;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.BasicPattern;

import com.bbn.parliament.kb_graph.query.index.GraphSubPattern;
import com.bbn.parliament.kb_graph.query.index.IndexPatternQuerier;
import com.bbn.parliament.kb_graph.query.index.operand.Operand;
import com.bbn.parliament.kb_graph.query.index.pfunction.EstimableIndexPropertyFunction;
import com.bbn.parliament.kb_graph.query.index.pfunction.IndexPropertyFunction;

public class IndexSubPatternFactory {
	public static <T> IndexSubPatternPropertyFunction<T> create(IndexPropertyFunction<T> pf,
		Node predicate, List<Node> subjects, List<Node> objects, Map<Node, Operand<T>> operands) {
		if (pf instanceof EstimableIndexPropertyFunction) {
			return new EstimablePropertyFunctionPattern<>((EstimableIndexPropertyFunction<T>) pf,
				predicate, subjects, objects, operands);
		}
		return new IndexSubPatternPropertyFunction<>(pf, predicate, subjects, objects, operands);
	}

	public static IndexSubPatternBGP create(IndexPatternQuerier querier) {
		return new IndexSubPatternBGP(querier);
	}

	public static IndexSubPatternBGP create(
		IndexPatternQuerier querier, BasicPattern other) {
		return new IndexSubPatternBGP(querier, other);
	}

	public static IndexSubPatternBGP create(Graph graph) {
		return new GraphSubPattern(graph);
	}
}
