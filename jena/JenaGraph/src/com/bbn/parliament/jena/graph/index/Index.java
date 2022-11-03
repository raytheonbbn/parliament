package com.bbn.parliament.jena.graph.index;

import java.util.Iterator;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;

import com.bbn.parliament.jena.query.index.QueryCache;

/**
 * The interface that defines an external collection of {@link Record}s. This
 * interface defines operations for storage (add, remove, clear, delete).
 * <br><br>
 * An <code>Index</code> is associated with a {@link Graph} via an
 * {@link IndexListener}. This listener captures add and remove events from the
 * <code>Graph</code>, creates <code>Record</code>s for those events via a
 * {@link RecordFactory}, and sends the <code>Record</code>s to the
 * <code>Index</code> to process.
 * <br><br>
 * <code>Index</code> instances need should be created by an associated
 * {@link IndexFactory}. This <code>IndexFactory</code> needs to be registered
 * with the {@link IndexFactoryRegistry} in order for <code>Index</code>es to be
 * created at run time through the use of the
 * <code>&lt;http://parliament.semwebcentral.org/pfunction#enableIndex&gt;</code>
 * property function.
 *
 * @param <T> The type of object to index
 *
 * @author rbattle
 *
 * @see RecordFactory
 * @see IndexFactory
 * @see IndexManager
 * @see IndexListener
 * @see QueryCache
 */
public interface Index<T> {
	/**
	 * Answer whether or not the index is closed.
	 *
	 * @return <code>true</code> if the index is closed; otherwise <code>false</code>.
	 */
	public boolean isClosed();

	/**
	 * Close this instance. Any resources held will be closed.
	 *
	 * @throws IndexException if an error occurs while closing.
	 */
	public void close() throws IndexException;

	/**
	 * Open this instance. Any initialization that has not happened will occur here.
	 *
	 * @throws IndexException if an error occurs while opening.
	 */
	public void open() throws IndexException;

	/**
	 * Delete any resources held by this instance.
	 *
	 * @throws IndexException if an error occurs while deleting this instance.
	 */
	public void delete() throws IndexException;

	/**
	 * Clear all records.
	 *
	 * @throws IndexException if an error occurs while clearing the records.
	 */
	public void clear() throws IndexException;

	/**
	 * The size of the index.
	 *
	 * @return the size of the index.
	 * @throws IndexException if an error occurs while reading the size.
	 */
	public long size() throws IndexException;

	/**
	 * Add a bulk set of records to the index.
	 *
	 * @param records the records to add.
	 * @throws IndexException if an error occurs while adding a record.
	 */
	public void add(Iterator<Record<T>> records) throws IndexException;

	/**
	 * Add a record to the index.
	 *
	 * @param r a record to add.
	 * @return <code>true</code> if the record was added; otherwise <code>false</code>
	 * @throws IndexException if an error occurs while adding the record.
	 */
	public boolean add(Record<T> r) throws IndexException;

	/**
	 * Remove a bulk set of records from the index.
	 *
	 * @param records the records to remove.
	 * @throws IndexException if an error occurs while removing a record.
	 */
	public void remove(Iterator<Record<T>> records) throws IndexException;

	/**
	 * Remove a record from the index.
	 *
	 * @param r a record to remove.
	 * @return <code>true</code> if the record was removed; otherwise <code>false</code>.
	 * @throws IndexException if an error occurs while removing the record.
	 */
	public boolean remove(Record<T> r) throws IndexException;

	/** Get the record factory used to create records. */
	public RecordFactory<T> getRecordFactory();

	/** Get an iterator over the entire index. */
	public Iterator<Record<T>> iterator();

	/**
	 * Perform any registration actions. This is called by the {@link IndexManager} when
	 * this instance is registered.
	 *
	 * @param graph the graph
	 * @param graphName the name of the graph
	 */
	public void register(Graph graph, Node graphName);

	/**
	 * Perform any registration actions. This is called by the {@link IndexManager} when
	 * this instance is unregistered.
	 *
	 * @param graph the graph
	 * @param graphName the name of the graph
	 */
	public void unregister(Graph graph, Node graphName);

	/**
	 * Flush the index.
	 *
	 * @throws IndexException if an error occurs while flushing.
	 */
	public void flush() throws IndexException;
}
