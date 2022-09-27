package com.bbn.parliament.jena.query.optimize.pattern;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.bbn.parliament.jena.query.index.operand.Operand;
import com.bbn.parliament.jena.query.index.operand.OperandFactoryHelper;
import com.bbn.parliament.jena.query.index.pfunction.IndexPropertyFunction;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.sparql.core.BasicPattern;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIterRepeatApply;

public class IndexSubPatternPropertyFunction<T> extends IndexSubPattern {
	protected IndexPropertyFunction<T> function;
	protected List<Node> subjects;
	protected List<Node> objects;
	protected Node predicate;

	public IndexSubPatternPropertyFunction(IndexPropertyFunction<T> function,
		Node predicate, List<Node> subjects, List<Node> objects, Map<Node, Operand<T>> operands) {
		this.function = function;
		this.subjects = subjects;
		this.objects = objects;
		this.predicate = predicate;

		for (Node n : subjects) {
			this.addAll(BasicPattern.wrap(operands.get(n).getTriples()));
		}
		for (Node n : objects) {
			this.addAll(BasicPattern.wrap(operands.get(n).getTriples()));
		}
	}

	@Override
	public QueryIterator evaluate(QueryIterator input, ExecutionContext context) {
		return new QueryIterRepeatApply(input, context) {
			@Override
			protected QueryIterator nextStage(Binding binding) {
				Map<Node, Operand<T>> operands = OperandFactoryHelper.getOperands(
					function.getOperandFactory(), subjects, objects, binding,
					function.getPattern(), true);
				return function.execBinding(binding, subjects, objects, operands, getExecContext());
			}
		};
	}

	@Override
	public String toString() {
		return "IndexSubPatternPropertyFunction [subjects=%1$s, predicate=%2$s, objects=%3$s]"
			.formatted(subjects, predicate, objects);
	}

	@Override
	public Set<Node> getVariables() {
		Set<Node> vars = new HashSet<>();
		for (Node n : subjects) {
			if (n.isVariable()) {
				vars.add(n);
			}
		}
		for (Node n : objects) {
			if (n.isVariable()) {
				vars.add(n);
			}
		}
		return vars;
	}
}
