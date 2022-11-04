// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
package com.bbn.parliament.jena.graph.index.temporal.extent;

import java.util.Calendar;
import java.util.Objects;

import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;

/**
 * Implementation of an instant in time. Similar to {@link TemporalInterval}s in that
 * they can be represented by two 64-bit longs.
 * Different in that both longs are always the same value, and in turn must represent the same point
 * in time. For this reason {@link #getStart()} and {@link #getEnd()} are designed to both
 * return the same thing--a reference to the instant itself.
 *
 * @author dkolas
 * */

public class TemporalInstant implements TemporalExtent, Comparable<TemporalInstant> {
	protected long instant;
	protected TemporalInterval parentInterval;
	protected boolean isStart = false;
	protected boolean isEnd = false;
	protected Node anonymousNode;

	public Node getAnonymousNode() {
		if (anonymousNode == null) {
			anonymousNode = NodeFactory.createBlankNode();
		}
		return anonymousNode;
	}

	protected TemporalInstant() {
	}

	public TemporalInstant(Calendar c)	{
		this(c.getTimeInMillis());
	}

	public TemporalInstant(XSDDateTime time) {
		this(time.asCalendar().getTimeInMillis());
	}

	public TemporalInstant(long timeInMillis) {
//		if (timeInMillis < Long.MIN_VALUE || timeInMillis > Long.MAX_VALUE)	{
//			throw new IllegalArgumentException("Instant is past the index's upper/lower"
//				+ " temporal bound. Overflow may have occured");
//		}
		instant = timeInMillis;
	}

	public TemporalInstant(long time, TemporalInterval parentInterval, boolean isStart) {
		this(time);
		this.parentInterval = parentInterval;
		this.isStart = isStart;
		this.isEnd = !isStart;
	}

	public TemporalInstant(Calendar c, TemporalInterval parentInterval, boolean isStart) {
		this(c.getTimeInMillis(), parentInterval, isStart);
	}

	public TemporalInstant(XSDDateTime time, TemporalInterval parentInterval, boolean isStart) {
		this(time.asCalendar().getTimeInMillis(), parentInterval, isStart);
	}

	/**
	 * Performs a total-ordering comparison. This means that 2 TemporalInstant objects that
	 * represent the same time, but for different intervals are not actually equal.
	 *
	 * TemporalInstants are ordered as follows:
	 * 1) TemporalInstants with earlier epochs ("instants") precede those with later epochs.
	 * 2) TemporalInstants that are not part of a TemporalInterval precede those that are.
	 * 3) TemporalInstants that are the starts of their TemporalIntervals precede those that end theirs.
	 * 4) TemporalInstants that are otherwise equivalent break their ties based upon their TemporalIntervals' orderings.
	 *
	 * @param that TemporalInstant object to compare with
	 * @return -1 if the given argument is greater than this object. 1 if the argument is
	 *         smaller. 0 if they are equal.
	 */
	@Override
	public int compareTo(TemporalInstant that) {
		int comparison = Long.compare(this.instant, that.instant);
		if (comparison == 0) {
			comparison = (
				(this.parentInterval == null ? -1 : 0) +
				(that.parentInterval == null ? 1 : 0)
			);
			if (comparison == 0 && this.parentInterval != null) {
				comparison = (this.isStart ? -1 : 0) + (that.isStart ? 1 : 0);
				if (comparison == 0) {
					comparison = this.parentInterval.compareTo(that.parentInterval);
				}
			}
		}
		return comparison;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj instanceof TemporalInstant ti) {
			return instant == ti.instant
				&& isStart == ti.isStart
				&& Objects.equals(parentInterval, ti.parentInterval);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(instant, isStart, parentInterval);
	}

	@Override
	public String toString(){
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(instant);
		XSDDateTime dt = new XSDDateTime(calendar);
		return dt.toString();
	}

	public long getInstant() {
		return instant;
	}

	/** @see com.bbn.parliament.jena.graph.index.temporal.extent.TemporalExtent#getStart() */
	@Override
	public TemporalInstant getStart() {
		return this;
	}

	/** @see com.bbn.parliament.jena.graph.index.temporal.extent.TemporalExtent#getEnd() */
	@Override
	public TemporalInstant getEnd() {
		return this;
	}

	public TemporalInterval getParentInterval() {
		return parentInterval;
	}

	public boolean isEnd() {
		return isEnd;
	}

	public boolean isStart() {
		return isStart;
	}

	/**
	 * Note that this is different from an expected implementation of .equals() in that it
	 * does not compare the actual nodes that use this instant. Instead, it only compares
	 * the value of the instant.
	 */
	@Override
	public boolean sameAs(TemporalExtent other) {
		return (
			other != null &&
			TemporalInstant.class.equals(other.getClass()) &&
			instant == ((TemporalInstant) other).instant
		);
	}

	/**
	 * Used to compute whether this instant is less than the given instant. This simply
	 * compares the time-stamps.
	 *
	 * @return true if this instant is less than the argument
	 */
	public boolean lessThan(TemporalInstant instant2) {
		return instant < instant2.instant;
	}

	/**
	 * Used to compute whether this instant is greater than the given instant. This simply
	 * compares the time-stamps.
	 *
	 * @return true if this instant is greater than the argument
	 */
	public boolean greaterThan(TemporalInstant instant2) {
		return instant > instant2.instant;
	}

	/**
	 * Creates a TemporalInstant based on this one but with a time that is 1ms less. This
	 * is to deal with the total-ordering of the Instants in the TemporalIndex. Otherwise,
	 * you could miss some of the potential objects when returning a subset based on a
	 * pivot point with duplicate entries.
	 *
	 * @return TemporalInstant with 1ms less time
	 */
	public TemporalInstant createSmallerInstant() {
		TemporalInstant rv = new TemporalInstant(instant, parentInterval, isStart);
		rv.instant -= 1;
		return rv;
	}

	/**
	 * Creates a TemporalInstant based on this one but with a time that is 1ms more. This
	 * is to deal with the total-ordering of the Instants in the TemporalIndex. Otherwise,
	 * you could miss some of the potential objects when returning a subset based on a
	 * pivot point with duplicate entries.
	 *
	 * @return TemporalInstant with 1ms more time
	 */
	public TemporalInstant createLargerInstant() {
		TemporalInstant rv = new TemporalInstant(instant, parentInterval, isStart);
		rv.instant += 1;
		return rv;
	}
}
