// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
package com.bbn.parliament.jena.graph;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.impl.SimpleBulkUpdateHandler;
import com.hp.hpl.jena.util.iterator.NiceIterator;

/**
 * @author sallen
 * @author dkolas
 */
public class KbBulkUpdateHandler extends SimpleBulkUpdateHandler
{
	private static final int SLICE_SIZE = 1000;

	public KbBulkUpdateHandler(KbGraph graph)
	{
		super(graph);
	}

	@Override
	public void removeAll()
	{
		((KbGraph) graph).clear();
		notifyRemoveAll();
	}

	// Batch the iterator inserts/deletes so that the naive event logic in SimpleBulkUpdateHandler doesn't
	// exhaust the memory when collapsing the iterator to a List to pass to listeners.
	// (TODO: are we allowed to batch this? Or do listeners expect a single event per SPARQL/Update query?)
	@SuppressWarnings("null")
	@Override
	public void addIterator(Iterator<Triple> it, boolean notify) {
		try {
			List<Triple> triples = notify ? new ArrayList<>(SLICE_SIZE) : null;
			int count = 0;
			while (it.hasNext()) {
				Triple t = it.next();
				graph.performAdd(t);

				if (notify) {
					if (count >= SLICE_SIZE) {
						manager.notifyAddIterator(graph, triples);
						triples.clear();
						count = 0;
					}
					triples.add(t);
					++count;
				}
			}
			if (notify) {
				manager.notifyAddIterator(graph, triples);
			}
		}
		finally {
			NiceIterator.close(it);
		}
	}

	@SuppressWarnings("null")
	@Override
	public void deleteIterator(Iterator<Triple> it, boolean notify) {
		try {
			List<Triple> triples = notify ? new ArrayList<>(SLICE_SIZE) : null;
			int count = 0;
			while (it.hasNext()) {
				Triple t = it.next();
				graph.performDelete(t);

				if (notify) {
					if (count >= SLICE_SIZE) {
						manager.notifyDeleteIterator(graph, triples);
						triples.clear();
						count = 0;
					}
					triples.add(t);
					count++;
				}
			}
			if (notify) {
				manager.notifyDeleteIterator(graph, triples);
			}
		}
		finally {
			NiceIterator.close(it);
		}
	}
}
