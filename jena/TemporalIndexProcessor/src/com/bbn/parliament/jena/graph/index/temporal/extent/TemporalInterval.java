// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
package com.bbn.parliament.jena.graph.index.temporal.extent;

import java.util.Objects;

/**
 * Implementation for an interval in time. The concrete representation for <code>TemporalInterval</code>s
 * is a pair of 64-bit <code>long</code>s wrapped inside {@link TemporalInstant}s. These represent the endpoints of the
 * interval. Note that the starting instant must always be before the ending instant for the interval to be
 * valid.  It is also worth noting that an interval consisting of two equivalent instants is also invalid,
 * because its on-disk representation would erroneously match that of a {@link TemporalInstant}.
 * @author dkolas
 * @author mhale
 */
public class TemporalInterval implements TemporalExtent, Comparable<TemporalInterval> {

	private TemporalInstant start;
	private TemporalInstant end;

	/**
	 * Constructs a TemporalInterval from two TemporalInstants.
	 * @param startTime The start time. Null if unbounded.
	 * @param endTime The end time. Null if unbounded.
	 * @throws IllegalArgumentException if the interval is improper (i.e. endpoints equal one another)
	 * or if the interval's end does not chronologically follow its start.
	 * @author mhale
	 */
	public TemporalInterval(TemporalInstant startTime, TemporalInstant endTime)	{

		//Case to construct interval with left unbounded timeline
		if (startTime == null)	{
			start = new TemporalInstant(Long.MIN_VALUE);
		}
		else	{
			start = startTime;
		}
		//Case to construct interval with right unbounded timeline
		if (endTime == null)	{
			end = new TemporalInstant(Long.MAX_VALUE);
		}
		else	{
			end = endTime;
		}
		//Chronology check
		if (!end.greaterThan(start))	{
			throw new IllegalArgumentException("Interval must be proper and chronological.");
		}
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
	@Override
	public boolean sameAs(TemporalExtent other) {
		TemporalInterval that;
		return (
			other != null &&
			TemporalInterval.class.equals(other.getClass()) &&
			start.sameAs((that = (TemporalInterval) other).start) &&
			end.sameAs(that.end)
		);
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
	public boolean equals(Object other) {
		boolean areEqual = this == other;
		if (!areEqual && TemporalInterval.class.equals(other.getClass())) {
			TemporalInterval that = (TemporalInterval) other;
			areEqual = this.start.instant == that.start.instant && this.end.instant == that.end.instant;
		}
		return areEqual;
	}

	@Override
	public int hashCode() {
		return Objects.hash(start.instant, end.instant);
	}

	@Override
	public int compareTo(TemporalInterval that) {
		int comparison = -1;
		if (that != null) {
			comparison = Long.compare(this.start.instant, that.start.instant);
			if (comparison == 0) {
				comparison = Long.compare(this.end.instant, that.end.instant);
			}
		}
		return comparison;
	}

	@Override
	public String toString(){
		return "Interval - ("+start+" - "+end+")";
	}
}
