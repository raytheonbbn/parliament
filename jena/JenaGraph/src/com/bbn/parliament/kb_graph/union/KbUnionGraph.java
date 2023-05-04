// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.kb_graph.union;

import java.util.Set;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.compose.Union;
import org.apache.jena.util.CollectionFactory;
import org.apache.jena.util.iterator.ExtendedIterator;


/** @author dkolas */
public class KbUnionGraph extends Union implements KbUnionableGraph {
	private KbUnionableGraph left;
	private KbUnionableGraph right;
	private boolean filtering = true;

	private Node leftGraphName;
	private Node rightGraphName;
	protected boolean isClosed = false;

	public KbUnionGraph(KbUnionableGraph L, Node leftGraphName, KbUnionableGraph R, Node rightGraphName) {
		super(L, R);
		left = L;
		right = R;
		this.leftGraphName = leftGraphName;
		this.rightGraphName = rightGraphName;
	}

	public Node getLeftGraphName() {
		return leftGraphName;
	}

	public Node getRightGraphName() {
		return rightGraphName;
	}

	@Override
	public void close() {
		isClosed = true;
	}

	/** {@inheritDoc} */
	@Override
	public boolean isClosed() {
		return isClosed;
	}

	@Override
	public long getNodeCountInPosition(Node node, int position) {
		return left.getNodeCountInPosition(node, position)
			+ right.getNodeCountInPosition(node, position);
	}

	@Override
	public ExtendedIterator<Triple> _graphBaseFind(Triple t) {
		if (filtering) {
			Set<Triple> seen = CollectionFactory.createHashedSet();
			return recording(L.find(t), seen).andThen(rejecting(R.find(t), seen));
		}
		return L.find(t).andThen(R.find(t));
	}

	public boolean isFiltering() {
		return filtering;
	}

	public void setFiltering(boolean filtering) {
		this.filtering = filtering;
		if (left instanceof KbUnionGraph leftUnionGraph) {
			leftUnionGraph.setFiltering(filtering);
		}
		if (right instanceof KbUnionGraph rightUnionGraph) {
			rightUnionGraph.setFiltering(filtering);
		}
	}
}
