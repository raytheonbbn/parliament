package com.bbn.parliament.jena.graph.index.numeric;

import java.util.NoSuchElementException;

import org.apache.jena.graph.Node;
import org.apache.jena.util.iterator.ClosableIterator;

import com.bbn.parliament.jena.graph.index.Record;
import com.bbn.parliament.jena.util.NodeUtil;
import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.SecondaryCursor;
import com.sleepycat.je.SecondaryDatabase;

/**
 * Iterator for data stored in a {@link NumericIndex}. The iterator iterates over the
 * values in the bdb and checks to see if they are within the specified range.
 *
 * @author rbattle
 * @param <T> the type of data that is indexed.
 */
public class NumericIndexIterator<T extends Number & Comparable<T>>
implements ClosableIterator<Record<T>> {

	private T start;
	private T end;

	private SecondaryCursor cursor;
	private Record<T> current;

	private NumericIndex<T> index;
	private DatabaseEntry entry;
	private DatabaseEntry currentData;
	private DatabaseEntry primaryKey;
	private boolean preparedForNext;
	private boolean closed;

	/**
	 * Construct a new instance.
	 *
	 * @param index the index to iterate over
	 * @param start the minimum value
	 * @param end the maximum value
	 */
	public NumericIndexIterator(NumericIndex<T> index, T start, T end) {
		this.closed = false;

		this.start = start;
		this.end = end;
		this.index = index;
		OperationStatus initialStatus = null;

		this.entry = null;
		this.primaryKey = new DatabaseEntry();
		this.currentData = new DatabaseEntry();

		this.preparedForNext = false;
		try {
			@SuppressWarnings("resource")
			SecondaryDatabase secDb = index.getNumbersDatabase();
			cursor = secDb.openCursor(null, CursorConfig.READ_UNCOMMITTED);

			entry = new DatabaseEntry(index.getRecordFactory()
				.getBytesForNumber(start));
			initialStatus = cursor.getSearchKeyRange(entry, primaryKey,
				currentData,
				LockMode.READ_UNCOMMITTED);

			if (OperationStatus.SUCCESS.equals(initialStatus)) {
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

	/**
	 * Test the current value to make sure it is in the correct range.
	 *
	 * @return <code>true</code> if the value is acceptable; otherwise <code>false</code>.
	 */
	private boolean testCurrent() {
		T value = current.getValue();

		boolean valid = start.compareTo(value) <= 0
			&& end.compareTo(value) >= 0;

			return valid;
	}

	/** Get the current number. */
	private T getNumber() {
		T value = index.getRecordFactory().getNumberForBytes(currentData
			.getData());
		return value;
	}

	/** Get the current node. */
	private Node getNode() {
		Node node = NodeUtil.getNodeRepresentation(new String(primaryKey
			.getData()));
		return node;
	}

	/** Get the current record. */
	private Record<T> getRecord() {
		return Record.create(getNode(), getNumber());
	}

	/** {@inheritDoc} */
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
			close();
			current = null;
		} catch (DatabaseException e) {
			throw new RuntimeException("Problem querying the database", e);
		}

		return false;
	}

	/**
	 * Check whether the current value is past the end value.
	 *
	 * @return <code>true</code> if the value is greater than the end value; otherwise
	 *         <code>false</code>.
	 */
	private boolean pastEnd() {
		T value = current.getValue();
		return value.compareTo(end) > 0;
	}

	/**
	 * Move the cursor to the next value.
	 *
	 * @return <code>true</code> if the operation was successful; otherwise
	 *         <code>false</code>.
	 * @throws DatabaseException if an error occurs
	 */
	private boolean cursorNext() throws DatabaseException {
		OperationStatus status = null;

		status = cursor.getNext(entry, primaryKey, currentData,
			LockMode.READ_UNCOMMITTED);

		return status == OperationStatus.SUCCESS;
	}

	/** Check if the iterator is closed. If it is, throw an unsupported operation exception. */
	private void checkClosed() {
		if (closed) {
			throw new UnsupportedOperationException(
				"Cannot iterate over a closed iterator");
		}
	}

	/** {@inheritDoc} */
	@Override
	public Record<T> next() {
		checkClosed();
		if (!preparedForNext) {
			if (!hasNext()) {
				throw new NoSuchElementException("No more data!");
			}
		}
		preparedForNext = false;
		return current;
	}

	/** {@inheritDoc} */
	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	/** {@inheritDoc} */
	@Override
	public void close() {
		if (closed) {
			return;
		}
		try {
			cursor.close();
		} catch (DatabaseException e) {
			throw new RuntimeException(e);
		} finally {
			closed = true;
		}
	}
}
