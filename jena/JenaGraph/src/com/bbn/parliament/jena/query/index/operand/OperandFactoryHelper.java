package com.bbn.parliament.jena.query.index.operand;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.sparql.core.BasicPattern;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.vocabulary.RDF;

public class OperandFactoryHelper {
	protected static List<Node> getSubordinateNodes(Node n, BasicPattern pattern) {
		List<Node> ret = new ArrayList<>();
		for (Triple t : pattern) {
			if (t.predicateMatches(RDF.type.asNode())) {
				continue;
			}
			if (t.subjectMatches(n) && (t.getObject().isVariable() || t.getObject().isURI())) {
				ret.add(t.getObject());
				ret.addAll(getSubordinateNodes(t.getObject(), pattern));
			}
		}
		return ret;
	}

	public static <T> Map<Node, Operand<T>> getOperands(OperandFactory<T> opFactory, List<Node> subjects,
		List<Node> objects, Binding binding, BasicPattern pattern, boolean findSubordinates) {
		Map<Node, Operand<T>> operands = new HashMap<>();
		List<Node> allNodes = new ArrayList<>(subjects.size() + objects.size());
		allNodes.addAll(subjects);
		allNodes.addAll(objects);
		if (findSubordinates) {
			if (null != pattern) {
				List<Node> toAdd = new ArrayList<>();
				for (Node n : allNodes) {
					toAdd.addAll(getSubordinateNodes(n, pattern));
				}
				allNodes.addAll(toAdd);
			}
		}
		for (Node n : allNodes) {
			Operand<T> op = null;
			if (null != pattern) {
				op = opFactory.createOperand(n, pattern, binding);
			} else {
				op = opFactory.createOperand(n, binding);
			}
			if (null != op) {
				operands.put(n, op);
			}
		}
		return operands;
	}
}
