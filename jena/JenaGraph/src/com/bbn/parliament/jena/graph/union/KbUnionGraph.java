// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.graph.union;

import java.util.Set;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.TripleMatch;
import com.hp.hpl.jena.graph.compose.Union;
import com.hp.hpl.jena.graph.query.QueryHandler;
import com.hp.hpl.jena.graph.query.SimpleQueryHandler;
import com.hp.hpl.jena.util.CollectionFactory;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

/** @author dkolas */
public class KbUnionGraph extends Union implements KbUnionableGraph
{
	private KbUnionableGraph left;
	private KbUnionableGraph right;
	private boolean          filtering = true;

	private Node leftGraphName;
	private Node rightGraphName;
	protected boolean isClosed = false;

	public KbUnionGraph(KbUnionableGraph L, Node leftGraphName, KbUnionableGraph R, Node rightGraphName)
	{
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
	public void close()
	{
		isClosed = true;
	}

	/** {@inheritDoc} */
	@Override
	public boolean isClosed() {
		return isClosed;
	}

	@Override
	public QueryHandler queryHandler()
	{
		if (queryHandler == null)
		{
			queryHandler = new SimpleQueryHandler(this);//, left, right);
		}
		return queryHandler;
	}

	@Override
	public long getNodeCountInPosition(Node node, int position)
	{
		return left.getNodeCountInPosition(node, position)
			+ right.getNodeCountInPosition(node, position);
	}

	@Override
	public ExtendedIterator<Triple> graphBaseFind(TripleMatch t)
	{
		if (filtering)
		{
			Set<Triple> seen = CollectionFactory.createHashedSet();
			return recording(L.find(t), seen).andThen(rejecting(R.find(t), seen));
		}
		return L.find(t).andThen(R.find(t));
	}

	public boolean isFiltering()
	{
		return filtering;
	}

	public void setFiltering(boolean filtering)
	{
		this.filtering = filtering;
		if (left instanceof KbUnionGraph)
		{
			((KbUnionGraph) left).setFiltering(filtering);
		}
		if (right instanceof KbUnionGraph)
		{
			((KbUnionGraph) right).setFiltering(filtering);
		}
	}
}
