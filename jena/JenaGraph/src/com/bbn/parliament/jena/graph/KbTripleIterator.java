// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.graph;

import com.bbn.parliament.jni.StmtIterator;
import com.bbn.parliament.jni.StmtIterator.Statement;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.util.iterator.NiceIterator;

public class KbTripleIterator extends NiceIterator<Triple> {
	private StmtIterator si;
	private KbGraph graph;
	private Triple lastTriple;

	public KbTripleIterator(KbGraph graph, StmtIterator si) throws Throwable {
		super();
		this.graph = graph;
		this.si = si;
	}

	@Override
	public void close() {
		super.close();
		si.finalize();
	}

	@Override
	public void remove() {
		graph.delete(lastTriple);
	}

	@Override
	public Triple next() {
		Statement statement = si.next();
		Node s = graph.getResourceNodeForId(statement.getSubject());
		Node p = graph.getResourceNodeForId(statement.getPredicate());
		Node o = statement.isLiteral() ? graph.getLiteralNodeForId(statement.getObject())
			: graph.getResourceNodeForId(statement.getObject());
		lastTriple = Triple.create(s, p, o);
		return lastTriple;
	}

	@Override
	public boolean hasNext() {
		return si.hasNext();
	}
}
