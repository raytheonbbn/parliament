// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
package com.bbn.parliament.jena.graph.index.temporal.bdb;

import java.util.NoSuchElementException;

import com.bbn.parliament.jena.graph.index.Record;
import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalExtent;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.util.iterator.ClosableIterator;
import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.SecondaryCursor;
import com.sleepycat.je.SecondaryDatabase;

/** @author dkolas */
public class PersistentTemporalExtentIterator implements
ClosableIterator<Record<TemporalExtent>> {
	private long minStart;
	private long minEnd;
	private long maxStart;
	private long maxEnd;

	private SecondaryDatabase starts;
	private SecondaryDatabase ends;

	private SecondaryCursor secondaryCursor;

	private DatabaseEntry data;
	private DatabaseEntry primaryKey;
	private DatabaseEntry entry;

	private Record<TemporalExtent> current;
	@SuppressWarnings("unused")
	private Database primary;

	private boolean preparedForNext = false;

	private PersistentTemporalIndex index;

	private boolean usingStartsNotEnds;

	private boolean closed = false;

	public PersistentTemporalExtentIterator(PersistentTemporalIndex index,
		long minStart, long maxStart,
		long minEnd, long maxEnd) {

		this.minStart = minStart;
		this.maxStart = maxStart;
		this.minEnd = minEnd;
		this.maxEnd = maxEnd;
		this.index = index;
		this.starts = this.index.getStartsDatabase();
		this.ends = this.index.getEndsDatabase();
		this.primary = this.index.getPrimaryDatabase();

		this.data = new DatabaseEntry();
		this.primaryKey = new DatabaseEntry();

		long startsEstimate = this.index.estimateStartsForRange(minStart,
			maxStart);
		long endsEstimate = this.index.estimateEndsForRange(minEnd, maxEnd);

		OperationStatus initialStatus = null;

		try {
			if (startsEstimate < endsEstimate) {
				secondaryCursor = starts.openCursor(null,
					CursorConfig.READ_UNCOMMITTED);
				entry = new DatabaseEntry(
					PersistentTemporalIndex
					.getBytesForLong(minStart));
				initialStatus = secondaryCursor
					.getSearchKeyRange(entry, primaryKey, data,
						LockMode.READ_UNCOMMITTED);
				usingStartsNotEnds = true;
			} else {
				secondaryCursor = ends.openCursor(null,
					CursorConfig.READ_UNCOMMITTED);
				entry = new DatabaseEntry(
					PersistentTemporalIndex
					.getBytesForLong(minEnd));
				initialStatus = secondaryCursor
					.getSearchKeyRange(entry, primaryKey, data,
						LockMode.READ_UNCOMMITTED);
				usingStartsNotEnds = false;
			}

			if (initialStatus.equals(OperationStatus.SUCCESS)) {
				current = getRecord();
				if (!testCurrent()) {
					hasNext();
				} else {
					preparedForNext = true;
				}
			}
		} catch (DatabaseException e) {
			throw new RuntimeException(
				"Error with database while setting up iterator.",
				e);
		}
	}

	private TemporalExtent getExtent() {
		TemporalExtent extent = PersistentTemporalIndex.dataToExtent(data.getData());
		return extent;
	}

	private Node getNode() {
		Node node = PersistentTemporalIndex.getNodeRepresentation(
			new String(primaryKey.getData()));
		return node;
	}

	private Record<TemporalExtent> getRecord() {
		return Record.create(getNode(), getExtent());
	}

	private void checkClosed() {
		if (closed) {
			throw new UnsupportedOperationException("Cannot iterate over a closed iterator");
		}
	}

	@Override
	public boolean hasNext() {
		checkClosed();
		if (preparedForNext) {
			return current != null;
		}
		try {
			while (cursorNext()) {
				current = getRecord();
				if (pastEnd()) {
					break;
				}
				if (testCurrent()) {
					preparedForNext = true;
					return true;
				}
			}
			preparedForNext = true;
			closeCursors();
			current = null;
		} catch (DatabaseException e) {
			throw new RuntimeException("Problem querying the database", e);
		}

		return false;
	}

	private void closeCursors() throws DatabaseException {
		secondaryCursor.close();
	}

	private boolean pastEnd() {
		if (usingStartsNotEnds) {
			return current.getValue().getStart().getInstant() > maxStart;
		}
		return current.getValue().getEnd().getInstant() > maxEnd;
	}

	private boolean testCurrent() {
		long start = current.getValue().getStart().getInstant();
		long end = current.getValue().getEnd().getInstant();

		boolean value = ((minStart == maxStart && minStart == start) || (start > minStart && start < maxStart))
			&& ((minEnd == maxEnd && minEnd == end) || (end > minEnd && end < maxEnd));

		// System.out.println("Checked "+current.getNode()+": "+value);
		return value;
	}

	private boolean cursorNext() throws DatabaseException {
		OperationStatus status = null;

		status = secondaryCursor.getNext(entry, primaryKey, data, LockMode.READ_UNCOMMITTED);

		return status == OperationStatus.SUCCESS;
	}

	@Override
	public Record<TemporalExtent> next() {
		checkClosed();
		if (!preparedForNext) {
			if (!hasNext()) {
				throw new NoSuchElementException("No more data!");
			}
		}
		preparedForNext = false;
		return current;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("Remove is not supported");
	}

	@Override
	public void close() {
		if (closed) {
			return;
		}

		try {
			closeCursors();
		} catch (DatabaseException e) {
		}
		closed = true;
	}
}
