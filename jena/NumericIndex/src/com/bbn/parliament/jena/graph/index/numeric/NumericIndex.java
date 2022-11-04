package com.bbn.parliament.jena.graph.index.numeric;

import java.io.File;
import java.util.Iterator;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;

import com.bbn.parliament.jena.graph.index.IndexBase;
import com.bbn.parliament.jena.graph.index.IndexException;
import com.bbn.parliament.jena.graph.index.RangeIndex;
import com.bbn.parliament.jena.graph.index.Record;
import com.bbn.parliament.jena.query.index.IndexPatternQuerierManager;
import com.bbn.parliament.jena.query.index.QueryCache;
import com.bbn.parliament.jena.util.FileUtil;
import com.bbn.parliament.jena.util.NodeUtil;
import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.SecondaryConfig;
import com.sleepycat.je.SecondaryCursor;
import com.sleepycat.je.SecondaryDatabase;
import com.sleepycat.je.SecondaryKeyCreator;

/**
 * Abstract base class for numeric indexes. The <code>NumericIndex</code> indexes all
 * numbers for a specified property. The subclasses provide implementations for indexing
 * ints, longs, doubles, and floats.
 *
 * @author rbattle
 */
public abstract class NumericIndex<T extends Number & Comparable<T>>
	extends IndexBase<T> implements RangeIndex<T> {

	/** The secondary key creator that creates a key based on the bytes for the number */
	private static class NumbersKeyCreator implements SecondaryKeyCreator {
		private int size;

		public NumbersKeyCreator(int size) {
			this.size = size;
		}

		@Override
		public boolean createSecondaryKey(SecondaryDatabase secondary, DatabaseEntry key,
			DatabaseEntry data, DatabaseEntry result) throws DatabaseException {
			byte[] bytes = new byte[size];
			for (int i = 0; i < bytes.length; i++) {
				bytes[i] = data.getData()[i];
			}
			result.setData(bytes);
			return true;
		}
	}

	private static final String NODES_DB_NAME = "nodes";
	private static final String NUMBERS_DB_NAME = "numbers";

	private final String dirName;
	private final NumericRecordFactory<T> recordFactory;
	private final QueryCache<T> queryCache;
	private final NumericIndexPatternQuerier<T> querier;

	protected T min;
	protected T max;

	// bdb members
	private Environment environment;
	private DatabaseConfig dbConfig;
	private SecondaryConfig numbersDbConfig;
	private EnvironmentConfig envConfig;

	protected Database db;
	protected SecondaryDatabase numbersDb;

	/**
	 * Construct a new instance.
	 *
	 * @param predicate the predicate to index
	 * @param indexDir the directory to store the index
	 * @param recordFactory the record factory used to create records
	 */
	public NumericIndex(String predicate, String indexDir,
		NumericRecordFactory<T> recordFactory) {
		this.recordFactory = recordFactory;
		this.dirName = "%s%cnumeric_%s".formatted(
			indexDir, File.separatorChar, FileUtil.encodeStringForFilename(predicate));
		this.queryCache = new QueryCache<>(100);
		this.querier = new NumericIndexPatternQuerier<>(predicate, this);
	}

	/** Get the querier. */
	public NumericIndexPatternQuerier<T> getQuerier() {
		return querier;
	}

	/** Get the primary database. This is a view indexing nodes to numbers. */
	public Database getNodesDatabase() {
		return db;
	}

	/** Get the secondary view of the database. This is a view indexing numbers to nodes. */
	public SecondaryDatabase getNumbersDatabase() {
		return numbersDb;
	}

	/** {@inheritDoc} */
	@Override
	public QueryCache<T> getQueryCache() {
		return queryCache;
	}

	/** {@inheritDoc} */
	@Override
	public void doClose() throws IndexException {
		try {
			IndexPatternQuerierManager.getInstance().unregister(this);
			closeIndexes();
			environment.cleanLog();
			environment.close();
		} catch (DatabaseException e) {
			throw new IndexException(this, "Database failed to close", e);
		}
	}

	/**
	 * Close the bdb databases.
	 *
	 * @throws DatabaseException if an error occurs while closing
	 */
	private void closeIndexes() throws DatabaseException {
		numbersDb.close();
		db.close();
	}

	/**
	 * Open the bdb databases.
	 *
	 * @throws DatabaseException if an error occurs while opening
	 */
	private void openIndexes() throws DatabaseException {
		db = environment.openDatabase(null, NODES_DB_NAME, dbConfig);
		numbersDb = environment.openSecondaryDatabase(null, NUMBERS_DB_NAME, db,
			numbersDbConfig);
	}

	/** {@inheritDoc} */
	@Override
	public void doOpen() throws IndexException {
		try {
			initialize();

			openIndexes();

			readMinAndMax();
			IndexPatternQuerierManager.getInstance().register(this, querier);
		} catch (DatabaseException e) {
			throw new IndexException(this, "Could not open database", e);
		}
	}

	/**
	 * Initialize the environment and configure the databases.
	 *
	 * @throws DatabaseException if an error occurs
	 */
	private void initialize() throws DatabaseException {

		File dirFile = new File(dirName);
		dirFile.mkdirs();

		envConfig = new EnvironmentConfig();
		envConfig.setAllowCreate(true);
		envConfig.setLocking(false);
		envConfig.setReadOnly(false);
		envConfig.setTransactional(false);

		environment = new Environment(dirFile, envConfig);

		dbConfig = new DatabaseConfig();
		dbConfig.setAllowCreate(true);
		dbConfig.setDeferredWrite(true);
		dbConfig.setReadOnly(false);
		dbConfig.setTransactional(false);

		// Create secondary database for indexing values
		numbersDbConfig = new SecondaryConfig();
		numbersDbConfig.setAllowCreate(true);
		numbersDbConfig.setSortedDuplicates(true);
		numbersDbConfig.setAllowPopulate(true);
		numbersDbConfig.setKeyCreator(new NumbersKeyCreator(recordFactory.getNumberSize()));
	}

	/**
	 * Read the minimum and maximum values.
	 *
	 * @throws DatabaseException
	 */
	private void readMinAndMax() throws DatabaseException {
		DatabaseEntry key = new DatabaseEntry();
		DatabaseEntry value = new DatabaseEntry();

		try (SecondaryCursor cursor = numbersDb.openCursor(null, CursorConfig.READ_UNCOMMITTED)) {
			// Cursor cursor = startsDatabase.openCursor(null,
			// CursorConfig.READ_UNCOMMITTED);
			// OperationStatus retVal = cursor.getSearchKey(key, value,
			// LockMode.READ_UNCOMMITTED);

			OperationStatus status = cursor.getFirst(key, value, LockMode.READ_UNCOMMITTED);

			// If we get one thing back, there is at least one element in the db
			if (status == OperationStatus.SUCCESS) {
				min = getRecordFactory().getNumberForBytes(value.getData());

				cursor.getLast(key, value, LockMode.READ_UNCOMMITTED);
				max = getRecordFactory().getNumberForBytes(value.getData());
			} else {
				min = getRecordFactory().getMaximum();
				max = getRecordFactory().getMinimum();
			}
		}
	}

	/** {@inheritDoc} */
	@Override
	public void doDelete() throws IndexException {
		File dir = new File(dirName);
		FileUtil.delete(dir);
	}

	/** {@inheritDoc} */
	@Override
	public void doClear() throws IndexException {
		try {
			closeIndexes();
			environment.truncateDatabase(null, NUMBERS_DB_NAME, false);
			environment.truncateDatabase(null, NODES_DB_NAME, false);
			openIndexes();
		} catch (DatabaseException e) {
			throw new IndexException(this, "Error with the BDB database", e);
		}
	}

	/** {@inheritDoc} */
	@Override
	public long doSize() throws IndexException {
		try {
			return db.count();
		} catch (DatabaseException e) {
			throw new IndexException(this, "Error with the BDB database", e);
		}
	}

	/** {@inheritDoc} */
	@Override
	public boolean doAdd(Record<T> r) throws IndexException {
		Node node = r.getKey();
		T n = r.getValue();

		DatabaseEntry key = new DatabaseEntry();
		DatabaseEntry data = new DatabaseEntry();
		key.setData(NodeUtil.getStringRepresentation(node).getBytes());

		byte[] value = recordFactory.getBytesForNumber(n);
		data.setData(value);
		try {
			db.put(null, key, data);
		} catch (DatabaseException e) {
			throw new IndexException(this, "Error with the BDB database", e);
		}

		if (n.compareTo(min) < 0) {
			min = n;
		}
		if (n.compareTo(max) > 0) {
			max = n;
		}
		return true;
	}

	/** {@inheritDoc} */
	@Override
	public void doAdd(Iterator<Record<T>> records) throws IndexException {
		while (records.hasNext()) {
			doAdd(records.next());
		}
	}

	/** {@inheritDoc} */
	@Override
	public boolean doRemove(Record<T> r) throws IndexException {
		Node node = r.getKey();
		DatabaseEntry key = new DatabaseEntry(NodeUtil.getStringRepresentation(node)
			.getBytes());
		boolean success = false;
		try {
			OperationStatus status = db.delete(null, key);
			if (status.equals(OperationStatus.SUCCESS)) {
				success = true;
			}
		} catch (DatabaseException e) {
			throw new IndexException(this, "Error with the BDB database", e);
		}
		return success;
	}

	/** {@inheritDoc} */
	@Override
	public void doRemove(Iterator<Record<T>> records) throws IndexException {
		while (records.hasNext()) {
			doRemove(records.next());
		}
	}

	/** {@inheritDoc} */
	@Override
	public NumericRecordFactory<T> getRecordFactory() {
		return recordFactory;
	}

	/** {@inheritDoc} */
	@Override
	public Record<T> find(Node node) {
		DatabaseEntry key = new DatabaseEntry(NodeUtil.getStringRepresentation(node)
			.getBytes());
		DatabaseEntry value = new DatabaseEntry();
		try {
			db.get(null, key, value, LockMode.READ_UNCOMMITTED);
			if (null == value.getData()) {
				return null;
			}
			return Record.create(node, recordFactory.getNumberForBytes(value.getData()));
		} catch (DatabaseException e) {
			throw new RuntimeException("Error retrieving from the db");
		}
	}

	/** {@inheritDoc} */
	@Override
	public Iterator<Record<T>> query(T value) {
		return iterator(value, value);
	}

	/** {@inheritDoc} */
	@Override
	public Iterator<Record<T>> doIterator() {
		return iterator(min, max);
	}

	/** {@inheritDoc} */
	@Override
	public Iterator<Record<T>> iterator(T start, T end) {
		T s = start;
		T e = end;
		if (null == s) {
			s = min;
		}
		if (null == e) {
			e = max;
		}
		return new NumericIndexIterator<>(this, s, e);
	}

	/** {@inheritDoc} */
	@Override
	public void register(Graph graph, Node graphName) {
		// do nothing
	}

	/** {@inheritDoc} */
	@Override
	public void unregister(Graph graph, Node graphName) {
		// do nothing
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return getClass().getName() + " [dirName=" + dirName + ", min=" + min + ", max="
			+ max + "]";
	}

	/** {@inheritDoc} */
	@Override
	public void flush() {
		doClose();
		doOpen();
	}

	/** A <code>NumericIndex</code> for {@link Integer}s. */
	public static class IntegerIndex extends NumericIndex<Integer> {
		public IntegerIndex(String predicate, String indexDir) {
			super(predicate, indexDir, new NumericRecordFactory.IntegerRecordFactory(
				predicate));
		}
	}

	/** A <code>NumericIndex</code> for {@link Long}s. */
	public static class LongIndex extends NumericIndex<Long> {
		public LongIndex(String predicate, String indexDir) {
			super(predicate, indexDir, new NumericRecordFactory.LongRecordFactory(predicate));
		}
	}

	/** A <code>NumericIndex</code> for {@link Double}s. */
	public static class DoubleIndex extends NumericIndex<Double> {
		public DoubleIndex(String predicate, String indexDir) {
			super(predicate, indexDir, new NumericRecordFactory.DoubleRecordFactory(
				predicate));
		}
	}

	/** A <code>NumericIndex</code> for {@link Float}s. */
	public static class FloatIndex extends NumericIndex<Float> {
		public FloatIndex(String predicate, String indexDir) {
			super(predicate, indexDir,
				new NumericRecordFactory.FloatRecordFactory(predicate));
		}
	}
}
