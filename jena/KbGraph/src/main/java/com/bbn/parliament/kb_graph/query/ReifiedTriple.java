package com.bbn.parliament.kb_graph.query;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.shared.PrefixMapping;

/** @author dkolas */
public class ReifiedTriple extends Triple {
	private static final long serialVersionUID = 1L;

	private Node name;

	// Fixing the deprecation of the Triple ctor turns out to be quite difficult.
	// See the documentation in my failed attempt to do this in the class
	// ReifiedTriples (plural).
	public ReifiedTriple(Node name, Node s, Node p, Node o){
		super(s, p, o);
		this.name = name;
	}

	public Node getMatchName(){
		return Node.ANY.equals( name ) ? null : name;
	}

	public Node getName(){
		return name;
	}

	@Override
	public String toString(PrefixMapping pm) {
		return "["+name.toString(pm)+"] "+super.toString(pm);
	}
}
