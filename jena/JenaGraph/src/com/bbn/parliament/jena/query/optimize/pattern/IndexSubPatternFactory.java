package com.bbn.parliament.jena.query.optimize.pattern;

import java.util.List;
import java.util.Map;
import com.bbn.parliament.jena.query.index.GraphSubPattern;
import com.bbn.parliament.jena.query.index.IndexPatternQuerier;
import com.bbn.parliament.jena.query.index.operand.Operand;
import com.bbn.parliament.jena.query.index.pfunction.EstimableIndexPropertyFunction;
import com.bbn.parliament.jena.query.index.pfunction.IndexPropertyFunction;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.sparql.core.BasicPattern;

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
