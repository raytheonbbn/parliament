package com.bbn.parliament.jena.util;

import org.apache.jena.graph.BlankNodeId;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;

import com.bbn.parliament.jena.graph.KbGraph;

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
			// The equivalent concept for the (BlankNodeId) API is AnonId. Historically, that has been in the org.apache.jena.rdf.model package.
			result = NodeFactory.createBlankNode(BlankNodeId.create(
				representation.substring(KbGraph.MAGICAL_BNODE_PREFIX.length())));
		} else {
			result = NodeFactory.createURI(representation);
		}
		return result;
	}
}
