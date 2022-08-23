package com.bbn.parliament.jena.query.index.operand;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

public class Operand<T> {
	private Node rootNode;
	private T representation;
	private List<Triple> triples;

	public Operand(Node node) {
		this(node, null, null);
	}

	public Operand(Node node, T representation) {
		this(node, representation, null);
	}

	public Operand(Node node, T representation, Collection<Triple> triples) {
		rootNode = node;
		this.representation = representation;
		this.triples = new ArrayList<>();
		if (null != triples && triples.size() > 0) {
			this.triples.addAll(triples);
		}
	}

	public void setRepresentation(T representation) {
		this.representation = representation;
	}

	public List<Triple> getTriples() {
		return triples;
	}

	public Node getRootNode() {
		return rootNode;
	}

	public T getRepresentation() {
		return representation;
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Node: ");
		sb.append(rootNode.toString());
		sb.append("\n");
		sb.append("Representation: ");
		if (null == representation) {
			sb.append("<null>");
		} else {
			sb.append(representation);
		}
		sb.append("\n");
		if (triples.size() > 0) {
			sb.append("Triples:\n");
			for (Triple t : triples) {
				sb.append(t);
				sb.append("\n");
			}
		}

		return sb.toString().strip();
	}
}
