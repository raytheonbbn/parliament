
package com.bbn.parliament.jena.query;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.shared.PrefixMapping;

/**
 *
 * @author dkolas
 */
public class ReifiedTriple extends Triple {

	private Node name;

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
