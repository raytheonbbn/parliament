// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
package com.bbn.parliament.jena.graph.index.temporal.extent;

import java.util.Calendar;
import java.util.Objects;

import com.hp.hpl.jena.datatypes.xsd.XSDDateTime;
import com.hp.hpl.jena.graph.Node;

/** @author dkolas */
public class TemporalInstant extends TemporalExtent implements Comparable<TemporalInstant> {
	protected long instant;
	protected TemporalInterval parentInterval;
	protected boolean isStart = false;
	protected boolean isEnd = false;
	protected Node anonymousNode;

	public Node getAnonymousNode() {
		if (anonymousNode == null) {
			anonymousNode = Node.createAnon();
		}
		return anonymousNode;
	}

	protected TemporalInstant() {
	}

	public TemporalInstant(XSDDateTime time) {
		instant = time.asCalendar().getTimeInMillis();
	}

	public TemporalInstant(long timeInMillis) {
		instant = timeInMillis;
	}

	public TemporalInstant(long time, TemporalInterval parentInterval, boolean start) {
		this(time);
		this.parentInterval = parentInterval;
		this.isStart = start;
		this.isEnd = !start;
	}

	/**
	 * Performs a total-ordering comparison. This means that 2 TemporalInstant objects that
	 * represent the same time, but for different intervals are not actually equal.
	 *
	 * @param o TemporalInstant object to compare with
	 * @return -1 if the given argument is greater than this object. 1 if the argument is
	 *         smaller. 0 if they are equal.
	 */
	@Override
	public int compareTo(TemporalInstant o) {
		if (instant < o.instant) {
			return -1;
		} else if (instant > o.instant) {
			return 1;
		} else { // they have equivalent timestamps
			if (isStart && !o.isStart) { // this object's interval occurs after
				return 1;
			} else if (isEnd && o.isStart) { // this object's interval occurs before
				return -1;
			} else {
				return hashCode() - o.hashCode();
			}
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj instanceof TemporalInstant ti) {
			return instant == ti.instant
				&& isStart == ti.isStart
				&& isEnd == ti.isEnd
				&& parentInterval == ti.parentInterval;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(instant, isStart, isEnd, parentInterval);
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
	public boolean sameAs(TemporalInstant instant2) {
		return instant == instant2.instant;
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
