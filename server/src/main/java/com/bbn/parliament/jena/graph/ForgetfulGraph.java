// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
/**
 *
 */

package com.bbn.parliament.jena.graph;

import org.apache.jena.graph.Triple;
import org.apache.jena.graph.impl.GraphBase;
import org.apache.jena.shared.NotFoundException;
import org.apache.jena.util.iterator.ExtendedIterator;

/**
 * This is a graph that keeps track of the number of statements added,
 * but throws them away and doesn't actually store them.  It is useful
 * for syntax verification / count applications.
 *
 * @author sallen
 */
public class ForgetfulGraph extends GraphBase {
	protected int _numStatements = 0;

	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.graph.impl.GraphBase#graphBaseFind(com.hp.hpl.jena.graph.TripleMatch)
	 */
	@Override
	protected ExtendedIterator<Triple> graphBaseFind(Triple m) {
		throw new NotFoundException("ForgetfulGraph::graphBaseFind");
	}

	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.graph.impl.GraphBase#performAdd(com.hp.hpl.jena.graph.Triple)
	 */
	@Override
	public void performAdd(Triple t) {
		_numStatements++;
	}

	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.graph.impl.GraphBase#graphBaseSize()
	 */
	@Override
	protected int graphBaseSize() {
		return _numStatements;
	}
}
