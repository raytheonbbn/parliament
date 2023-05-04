// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.kb_graph;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.util.iterator.NiceIterator;

import com.bbn.parliament.jni.StmtIterator;
import com.bbn.parliament.jni.StmtIterator.Statement;

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
