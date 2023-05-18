// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
package com.bbn.parliament.kb_graph.index.temporal.extent;

/** @author dkolas */
public class TemporalInterval extends TemporalExtent {

	private TemporalInstant start;
	private TemporalInstant end;

	public TemporalInterval(TemporalInstant startTime, TemporalInstant endTime) {
		start = startTime;
		end = endTime;
		start.isStart = true;
		end.isEnd = true;
		start.parentInterval = this;
		end.parentInterval = this;
	}

	public TemporalInterval() {}

	@Override
	public TemporalInstant getEnd() {
		return end;
	}

	public void setEnd(TemporalInstant end) {
		this.end = end;
	}

	@Override
	public TemporalInstant getStart() {
		return start;
	}

	public void setStart(TemporalInstant start) {
		this.start = start;
	}

	// *******************************************
	// *** Boolean interval relationship tests ***
	// *******************************************

	/**
	 * Note that this is different from an expected implementation of .equals() in that it
	 * does not compare the actual nodes that use this interval. Instead, it only compares
	 * the values of the start and end of the interval. That means that this *is* an
	 * appropriate implementation of the Allen Time Interval 'equals' function.
	 */
	public boolean sameAs(TemporalInterval interval) {
		return getStart().sameAs(interval.getStart())
			&& getEnd().sameAs(interval.getEnd());
	}

	/** Tests whether this interval ends before the given interval begins. */
	public boolean before(TemporalInterval interval) {
		return end.lessThan(interval.start);
	}

	/** Tests whether this interval begins after the given interval ends. */
	public boolean after(TemporalInterval interval) {
		return interval.before(this);
	}

	/** Tests whether this interval contains the given interval. */
	public boolean contains(TemporalInterval interval) {
		return start.lessThan(interval.start) && end.greaterThan(interval.end);
	}

	/** Tests whether this interval occurs during the given interval. */
	public boolean during(TemporalInterval interval) {
		return interval.contains(this);
	}

	/**
	 * Tests whether this interval meets the given interval. Meets is defined such that
	 * this interval ends at the same time the given interval begins.
	 */
	public boolean meets(TemporalInterval interval) {
		return (end.sameAs(interval.start));
	}

	/** Tests whether this interval is met by the given interval. */
	public boolean metBy(TemporalInterval interval) {
		return interval.meets(this);
	}

	/**
	 * Tests whether this interval overlaps the argument interval. An overlap only occurs
	 * if the argument interval starts after this interval. In other words, overlap is not
	 * a symmetric function. This also means that equal intervals do not overlap.
	 */
	public boolean overlaps(TemporalInterval interval) {
		return start.lessThan(interval.start) && end.greaterThan(interval.start)
			&& end.lessThan(interval.end);
	}

	public boolean overlappedBy(TemporalInterval interval) {
		return interval.overlaps(this);
	}

	/**
	 * Tests whether this interval starts the given interval. Starts is defined such that
	 * this interval starts at the same time the given interval begins but ends before the
	 * given interval finishes
	 */
	public boolean starts(TemporalInterval interval) {
		return getStart().sameAs(interval.getStart())
			&& getEnd().lessThan(interval.getEnd());
	}

	/**
	 * Tests whether this interval is started by the given interval. Started by is defined
	 * such that this interval starts at the same time the given interval begins but ends
	 * after the given interval finishes
	 */
	public boolean startedBy(TemporalInterval interval) {
		return getStart().sameAs(interval.getStart())
			&& interval.getEnd().lessThan(getEnd());
	}

	/**
	 * Tests whether this interval finishes the given interval. Finishes is defined such
	 * that this interval finishes at the same time the given interval ends but starts
	 * after the given interval starts
	 */
	public boolean finishes(TemporalInterval interval) {
		return getEnd().sameAs(interval.getEnd())
			&& getStart().greaterThan(interval.getStart());
	}

	/**
	 * Tests whether this interval is finished by the given interval. Finished by is
	 * defined such that this interval finishes at the same time the given interval ends
	 * but starts before the given interval begins
	 */
	public boolean finishedBy(TemporalInterval interval) {
		return getEnd().sameAs(interval.getEnd())
			&& interval.getStart().greaterThan(getStart());
	}

	@Override
	public String toString(){
		return "Interval - ("+start+" - "+end+")";
	}
}
