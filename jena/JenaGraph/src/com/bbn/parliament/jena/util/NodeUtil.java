package com.bbn.parliament.jena.util;

import com.bbn.parliament.jena.graph.KbGraph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.AnonId;

public class NodeUtil {
	/**
	 * Get the string representation for a node. Parliament prepends
	 * {@link KbGraph#MAGICAL_BNODE_PREFIX} to blank nodes.
	 *
	 * @param n a node
	 * @return the string representation of that node.
	 */
	public static final String getStringRepresentation(Node n) {
		String stringRep = n.toString();
		if (n.isBlank()) {
			stringRep = KbGraph.MAGICAL_BNODE_PREFIX + stringRep;
		}
		return stringRep;
	}

	/**
	 * Get the node representation of a string. This only works for URI and blank
	 * nodes. This is the reverse of
	 * {@link NodeUtil#getStringRepresentation(Node)}.
	 *
	 * @param representation a representation of a node.
	 * @return the node.
	 */
	public static final Node getNodeRepresentation(String representation) {
		Node result = null;
		if (representation.startsWith(KbGraph.MAGICAL_BNODE_PREFIX)) {
			result = Node.createAnon(AnonId.create(
				representation.substring(KbGraph.MAGICAL_BNODE_PREFIX.length())));
		} else {
			result = Node.createURI(representation);
		}
		return result;
	}
}
