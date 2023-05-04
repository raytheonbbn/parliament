// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.graph.index.temporal.operands;

import java.util.Iterator;

import com.bbn.parliament.jena.graph.index.temporal.ExtentTester;
import com.bbn.parliament.jena.graph.index.temporal.TemporalExtentIterator;
import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalExtent;
import com.bbn.parliament.kb_graph.index.Record;

public class IntervalContains extends MemoryOperatorImplementation {
	public IntervalContains(ExtentTester extentTester) {
		super(extentTester);
	}

	/** Returns all TemporalExtent objects that 'contain' the given boundExtent */
	@Override
	public Iterator<Record<TemporalExtent>> bindFirstVar(TemporalExtent boundExtent) {
		return new TemporalExtentIterator(wrapIterator(
			getIndex().beforeStart(boundExtent)),
			new FinishesAfterEndInclusionDecider(boundExtent));
	}

	@Override
	public Iterator<Record<TemporalExtent>> bindSecondVar(TemporalExtent boundExtent) {
		return new TemporalExtentIterator(wrapIterator(
			getIndex().afterStart(boundExtent)),
			new FinishesBeforeEndInclusionDecider(boundExtent));
	}
}
