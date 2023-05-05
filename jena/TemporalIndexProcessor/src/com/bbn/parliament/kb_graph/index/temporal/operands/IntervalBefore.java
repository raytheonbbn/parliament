// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.kb_graph.index.temporal.operands;

import java.util.Iterator;

import com.bbn.parliament.kb_graph.index.Record;
import com.bbn.parliament.kb_graph.index.temporal.ExtentTester;
import com.bbn.parliament.kb_graph.index.temporal.TemporalExtentIterator;
import com.bbn.parliament.kb_graph.index.temporal.extent.TemporalExtent;

/** @author dkolas */
public class IntervalBefore extends MemoryOperatorImplementation {
	public IntervalBefore(ExtentTester extentTester) {
		super(extentTester);
	}

	/**
	 * @see com.bbn.parliament.kb_graph.index.temporal.TemporalPropertyFunction#bindFirstVar(
	 *      com.bbn.parliament.kb_graph.index.temporal.extent.TemporalExtent)
	 */
	@Override
	public Iterator<Record<TemporalExtent>> bindFirstVar(TemporalExtent boundExtent) {
		return new TemporalExtentIterator(wrapIterator(
			getIndex().beforeStart(boundExtent)),
			new EndsBeforeInclusionDecider(boundExtent));
	}

	@Override
	public Iterator<Record<TemporalExtent>> bindSecondVar(TemporalExtent boundExtent) {
		return new TemporalExtentIterator(wrapIterator(
			getIndex().afterFinish(boundExtent)),
			new StartsAfterInclusionDecider(boundExtent));
	}
}
