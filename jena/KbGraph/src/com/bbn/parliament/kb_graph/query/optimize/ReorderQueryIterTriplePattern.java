// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2011, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.kb_graph.query.optimize;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Substitute;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.ExecutionContext;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.iterator.QueryIterRepeatApply;
import org.apache.jena.sparql.engine.iterator.QueryIterTriplePattern;
import org.apache.jena.sparql.util.IterLib;

import com.bbn.parliament.kb_graph.KbGraph;
import com.bbn.parliament.kb_graph.query.ReifiedTriple;
import com.bbn.parliament.kb_graph.query.SolverUtil;

/**
 * An iterator that reorders triples before sending them to a {@link QueryIterTriplePattern}.
 * <br><br>
 * For each binding in the input {@link QueryIterator}, the input pattern is checked to
 * see if any variables in the triple are bound. If there are no matching variables,
 * nothing is changed.
 * <br><br>
 * If they are bound and the graph is a {@link KbGraph}, the pattern is reordered via the
 * {@link SolverUtil#optimizeTripleOrder(BasicPattern, KbGraph, ExecutionContext)}
 * method. If the graph is any other type, the triples with bound variables are
 * ordered before triples with no bound variables.
 *
 * @author rbattle
 */
public class ReorderQueryIterTriplePattern extends QueryIterRepeatApply {
	private BasicPattern pattern;

	/**
	 * Construct a new instance.
	 *
	 * @param pattern the pattern to reorder and process
	 * @param input an iterator of bindings
	 * @param context the context
	 */
	public ReorderQueryIterTriplePattern(BasicPattern pattern,
		QueryIterator input,
		ExecutionContext context) {
		super(input, context);
		this.pattern = pattern;
	}

	/** {@inheritDoc} */
	@Override
	protected QueryIterator nextStage(Binding binding) {
		QueryIterator ret = IterLib.result(binding, getExecContext());

		List<Triple> triples = new ArrayList<>(pattern.size());
		Graph graph = getExecContext().getActiveGraph();
		if (graph instanceof KbGraph kbGraph) {
			boolean optimize = false;
			BasicPattern bound = substitute(pattern, binding);
			for (Triple t : pattern.getList()) {
				List<Node> vars = OptimizeUtil.getVariables(t);
				for (Node v : vars) {
					Var var = Var.alloc(v);
					if (binding.contains(var)) {
						optimize = true;
						break;
					}
				}
				if (optimize) {
					break;
				}
			}
			// only optimize triple order if a bound variable is in the pattern
			if (optimize) {
				bound = SolverUtil.optimizeTripleOrder(bound, kbGraph, getExecContext());
			}
			triples = bound.getList();
		} else {
			// order triples by those with bound variables first
			List<Triple> remaining = new ArrayList<>();
			for (Triple t : pattern.getList()) {
				List<Node> vars = OptimizeUtil.getVariables(t);
				boolean contains = false;
				for (Node v : vars) {
					Var var = Var.alloc(v);
					if (binding.contains(var)) {
						contains = true;
						break;
					}
				}
				if (contains || vars.isEmpty()) {
					triples.add(t);
				} else {
					remaining.add(t);
				}
			}

			triples.addAll(remaining);
		}

		// create the return iterator
		for (Triple t : triples) {
			if (t instanceof ReifiedTriple reifT){
				ret = new QueryIterReifiedTriplePattern(ret, reifT, getExecContext());
			}else{
				ret = new QueryIterTriplePattern(ret, t, getExecContext());
			}
		}
		return ret;
	}

	/**
	 * We need our own version of the substitute method, because some of the triples could
	 * be ReifiedTriples
	 *
	 * @return a basic pattern with the new variables substituted
	 */
	public static BasicPattern substitute(BasicPattern bgp, Binding binding) {
		if (isNotNeeded(binding))
			return bgp;

		BasicPattern bgp2 = new BasicPattern();
		for (Triple triple : bgp) {
			Triple t = substitute(triple, binding);
			bgp2.add(t);
		}
		return bgp2;
	}

	public static Triple substitute(Triple triple, Binding binding) {
		if (isNotNeeded(binding))
			return triple;

		ReifiedTriple rTriple = (triple instanceof ReifiedTriple reifTriple)
			? reifTriple : null;


		Node s = triple.getSubject();
		Node p = triple.getPredicate();
		Node o = triple.getObject();
		Node name = null;
		if (rTriple != null){
			name = rTriple.getName();
		}

		Node s1 = Substitute.substitute(s, binding);
		Node p1 = Substitute.substitute(p, binding);
		Node o1 = Substitute.substitute(o, binding);

		Node name1 = null;
		if (rTriple != null){
			name1 = Substitute.substitute(name, binding);
		}

		Triple t = triple;
		if (rTriple == null){
			if (s1 != s || p1 != p || o1 != o)
				t = new Triple(s1, p1, o1);
		}else{
			if (s1 != s || p1 != p || o1 != o || name1 != name){
				t = new ReifiedTriple(name1, s1, p1, o1);
			}
		}
		return t;
	}

	private static boolean isNotNeeded(Binding b) {
		return b.isEmpty();
	}
}
