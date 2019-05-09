package com.bbn.parliament.jena.graph.index;

import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for implementing indexes. This class provides checks to make sure
 * the index is not in an illegal state when the index operations are called.
 *
 * @param <T> The type of object to index
 * @author rbattle
 */
public abstract class IndexBase<T> implements Index<T> {
	private static final Logger LOG = LoggerFactory.getLogger(IndexBase.class);
	private boolean closed = true;

	/**
	 * Close any resources held by this instance.
	 *
	 * @throws IndexException if an error occurs while closing.
	 */
	protected abstract void doClose() throws IndexException;

	/**
	 * Open this instance.
	 *
	 * @throws IndexException if an error occurs while opening.
	 */
	protected abstract void doOpen() throws IndexException;

	/**
	 * Delete any resources held by this instance.
	 *
	 * @throws IndexException if an error occurs while deleting.
	 */
	protected abstract void doDelete() throws IndexException;

	/**
	 * Clear all records.
	 *
	 * @throws IndexException if an error occurs while clearing all records.
	 */
	protected abstract void doClear() throws IndexException;

	/**
	 * Add a record.
	 *
	 * @param record a record.
	 * @return <code>true</code> if the record was added; otherwise
	 *         <code>false</code>.
	 * @throws IndexException if an error occurs while adding the record.
	 */
	protected abstract boolean doAdd(Record<T> record) throws IndexException;

	/**
	 * Add records.
	 *
	 * @param records records to add.
	 * @throws IndexException if an error occurs while adding the records.
	 */
	protected abstract void doAdd(Iterator<Record<T>> records) throws IndexException;

	/**
	 * Remove a record.
	 *
	 * @param record a record.
	 * @return <code>true</code> if the record was removed; otherwise
	 *         <code>false</code>.
	 * @throws IndexException if an error occurs while removing the record.
	 */
	protected abstract boolean doRemove(Record<T> record) throws IndexException;

	/**
	 * Remove records.
	 *
	 * @param records records to remove.
	 * @throws IndexException if an error occurs while removing the record.
	 */
	protected abstract void doRemove(Iterator<Record<T>> records) throws IndexException;

	/**
	 * Get an iterator over all records.
	 *
	 * @return an iterator.
	 */
	protected abstract Iterator<Record<T>> doIterator();

	/**
	 * The size of the index.
	 *
	 * @return the size of the index.
	 * @throws IndexException if an error occurs while reading the size.
	 */
	protected abstract long doSize() throws IndexException;

	/** {@inheritDoc} */
	@Override
	public final boolean isClosed() {
		return closed;
	}

	/** {@inheritDoc} */
	@Override
	public final void close() throws IndexException {
		if (closed) {
			return;
		}
		doClose();
		closed = true;
	}

	/** {@inheritDoc} */
	@Override
	public final void open() throws IndexException {
		if (!closed) {
			return;
		}
		doOpen();
		closed = false;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @throws IllegalStateException if the index is open
	 */
	@Override
	public final void delete() throws IndexException {
		if (!closed) {
			throw new IllegalStateException("Index open");
		}
		doDelete();
	}

	/**
	 * {@inheritDoc}
	 *
	 * @throws IllegalStateException if the index is closed
	 */
	@Override
	public final void clear() throws IndexException {
		if (closed) {
			throw new IllegalStateException("Index closed");
		}
		doClear();
	}

	/**
	 * {@inheritDoc}
	 *
	 * @throws IllegalStateException if the index is closed
	 */
	@Override
	public final boolean add(Record<T> r) throws IndexException {
		if (closed) {
			throw new IllegalStateException("Index closed");
		}

		long start = System.currentTimeMillis();
		boolean add = doAdd(r);
		long end = System.currentTimeMillis();
		LOG.trace("Add completed in: {}ms", (end - start));
		return add;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @throws IllegalStateException if the index is closed;
	 */
	@Override
	public final void add(Iterator<Record<T>> records) throws IndexException {
		if (closed) {
			throw new IllegalStateException("Index closed");
		}
		doAdd(records);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @throws IllegalStateException if the index is closed
	 */
	@Override
	public final boolean remove(Record<T> r) throws IndexException {
		if (closed) {
			throw new IllegalStateException("Index closed");
		}
		return doRemove(r);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @throws IllegalStateException if the index is closed;
	 */
	@Override
	public final void remove(Iterator<Record<T>> records) throws IndexException {
		if (closed) {
			throw new IllegalStateException("Index closed");
		}
		doRemove(records);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @throws IllegalStateException if the index is closed
	 */
	@Override
	public final long size() throws IndexException {
		if (closed) {
			throw new IllegalStateException("Index closed");
		}
		return doSize();
	}

	/** {@inheritDoc} */
	@Override
	public final Iterator<Record<T>> iterator() {
		if (closed) {
			throw new IllegalStateException("Index closed");
		}
		return doIterator();
	}
}
