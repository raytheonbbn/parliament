package com.bbn.parliament.kb_graph.index.spatial.persistence;

import java.io.Serializable;

/**
 * Representation of the key for stored map.
 *
 * @author rbattle
 */
public class NodeKey implements Serializable {
	private static final long serialVersionUID = 1L;

	private String node;

	/** Create a new instance. */
	public NodeKey(String node) {
		this.node = node;
	}

	/** Get the node name. */
	public String getNode() {
		return node;
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return "NodeKey [node=" + node + "]";
	}
}
