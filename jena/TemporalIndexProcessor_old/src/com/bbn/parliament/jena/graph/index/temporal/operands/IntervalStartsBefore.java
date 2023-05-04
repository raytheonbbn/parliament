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

/** @author dkolas */
public class IntervalStartsBefore extends MemoryOperatorImplementation {
	public IntervalStartsBefore(ExtentTester extentTester) {
		super(extentTester);
	}

	/**
	 * @see com.bbn.parliament.jena.graph.index.temporal.TemporalPropertyFunction#bindFirstVar(
	 *      com.bbn.parliament.jena.graph.index.temporal.extent.TemporalExtent)
	 */
	@Override
	public Iterator<Record<TemporalExtent>> bindFirstVar(TemporalExtent boundExtent) {
		return new TemporalExtentIterator(wrapIterator(
			getIndex().beforeStart(boundExtent)),
			new AlwaysIncludeStartsInclusionDecider(boundExtent));
	}

	/**
	 * @see com.bbn.parliament.jena.graph.index.temporal.TemporalPropertyFunction#bindSecondVar(
	 *      com.bbn.parliament.jena.graph.index.temporal.extent.TemporalExtent)
	 */
	@Override
	public Iterator<Record<TemporalExtent>> bindSecondVar(TemporalExtent boundExtent) {
		return new TemporalExtentIterator(wrapIterator(
			getIndex().afterStart(boundExtent)),
			new AlwaysIncludeStartsInclusionDecider(boundExtent));
	}
}
