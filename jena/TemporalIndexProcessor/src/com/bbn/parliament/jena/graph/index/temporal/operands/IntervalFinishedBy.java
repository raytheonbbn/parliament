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

public class IntervalFinishedBy extends MemoryOperatorImplementation {
	public IntervalFinishedBy(ExtentTester extentTester) {
		super(extentTester);
	}

	@Override
	public Iterator<Record<TemporalExtent>> bindFirstVar(final TemporalExtent boundExtent) {
		return new TemporalExtentIterator(wrapIterator(getIndex().beforeFinishInclusive(
			boundExtent)), new InclusionDecider() {
			@Override
			public TemporalExtent test(TemporalInstant instant) {
				TemporalInterval interval = instant.getParentInterval();
				if (interval != null && instant.isEnd()) {
					return (interval.getEnd().sameAs(boundExtent.getEnd()) && interval
						.getStart().lessThan(boundExtent.getStart())) ? interval : null;
				}
				return null;
			}
		});
	}

	@Override
	public Iterator<Record<TemporalExtent>> bindSecondVar(final TemporalExtent boundExtent) {
		return new TemporalExtentIterator(wrapIterator(getIndex().beforeFinishInclusive(
			boundExtent)), new InclusionDecider() {
			@Override
			public TemporalExtent test(TemporalInstant instant) {
				TemporalInterval interval = instant.getParentInterval();
				if (interval != null && instant.isEnd()) {
					return (interval.getEnd().sameAs(boundExtent.getEnd()) && interval
						.getStart().greaterThan(boundExtent.getStart())) ? interval : null;
				}
				return null;
			}
		});
	}

	@Override
	public boolean testExtents(TemporalExtent extent1, TemporalExtent extent2) {
		if (extent1 instanceof TemporalInterval interval1
			&& extent2 instanceof TemporalInterval interval2) {
			return interval1.finishedBy(interval2);
		}
		return false;
	}
}
