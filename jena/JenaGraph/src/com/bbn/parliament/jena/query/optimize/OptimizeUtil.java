package com.bbn.parliament.jena.query.optimize;

import java.util.ArrayList;
import java.util.List;

import com.bbn.parliament.jena.query.ReifiedTriple;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

public class OptimizeUtil {
	/**
	 * Get a list of variable nodes from a triple
	 * @param triple the triple
	 * @return a list of the variables.
	 */
	public static List<Node> getVariables(Triple triple) {
		List<Node> vars = new ArrayList<>();
		if (triple.getSubject().isVariable()) {
			vars.add(triple.getSubject());
		}
		if (triple.getPredicate().isVariable()) {
			vars.add(triple.getPredicate());
		}
		if (triple.getObject().isVariable()) {
			vars.add(triple.getObject());
		}
		if ((triple instanceof ReifiedTriple reifTriple) && reifTriple.getName().isVariable()) {
			vars.add(reifTriple.getName());
		}
		return vars;
	}
}
