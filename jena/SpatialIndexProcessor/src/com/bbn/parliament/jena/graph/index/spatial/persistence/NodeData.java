package com.bbn.parliament.jena.graph.index.spatial.persistence;

import java.io.Serializable;

/**
 * Representation of the geometry data to store.
 *
 * @author rbattle
 */
public class NodeData implements Serializable {
	private static final long serialVersionUID = 1L;

	private String node;
	private byte[] extent;
	private int id;
	private String crsCode;

	/**
	 * Create a new instance.
	 *
	 * @param id The id.
	 * @param node The node.
	 * @param extent The extent.
	 * @param crsCode The coordinate reference system code.
	 */
	public NodeData(int id, String node, byte[] extent, String crsCode) {
		this.id = id;
		this.node = node;
		this.extent = extent;
		this.crsCode = crsCode;
	}

	/** Get the coordinate reference system code. */
	public String getCRSCode() {
		return crsCode;
	}

	/** Get the id. */
	public int getId() {
		return id;
	}

	/** Get the node. */
	public String getNode() {
		return node;
	}

	/** Get the extent. */
	public byte[] getExtent() {
		return extent;
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return "NodeData [node=" + node + ", id=" + id + ", crsCode=" + crsCode + "]";
	}
}
