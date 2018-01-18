// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
package com.bbn.parliament.jena.graph.index.temporal.operands;

import java.util.Iterator;

import com.bbn.parliament.jena.graph.index.Record;
import com.bbn.parliament.jena.graph.index.temporal.ExtentTester;
import com.bbn.parliament.jena.graph.index.temporal.TemporalExtentIterator;
import com.bbn.parliament.jena.graph.index.temporal.TemporalExtentIterator.InclusionDecider;
import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalExtent;
import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalInstant;
import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalInterval;

public class IntervalStarts extends MemoryOperatorImplementation {
	public IntervalStarts(ExtentTester extentTester) {
		super(extentTester);
	}

	@Override
	public Iterator<Record<TemporalExtent>> bindFirstVar(final TemporalExtent boundExtent) {
		return new TemporalExtentIterator(wrapIterator(getIndex().afterStartInclusive(
			boundExtent)), new InclusionDecider() {
			@Override
			public TemporalExtent test(TemporalInstant instant) {
				TemporalInterval interval = instant.getParentInterval();
				if (interval != null && instant.isStart()) {
					return (interval.getStart().sameAs(boundExtent.getStart()) && interval
						.getEnd().lessThan(boundExtent.getEnd())) ? interval : null;
				}
				return null;
			}
		});
	}

	@Override
	public Iterator<Record<TemporalExtent>> bindSecondVar(final TemporalExtent boundExtent) {
		return new TemporalExtentIterator(wrapIterator(getIndex().afterStartInclusive(
			boundExtent)), new InclusionDecider() {
			@Override
			public TemporalExtent test(TemporalInstant instant) {
				TemporalInterval interval = instant.getParentInterval();
				if (interval != null && instant.isStart()) {
					return (interval.getStart().sameAs(boundExtent.getStart()) && interval
						.getEnd().greaterThan(boundExtent.getEnd())) ? interval : null;
				}
				return null;
			}
		});
	}
}
