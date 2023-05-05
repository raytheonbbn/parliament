// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.kb_graph.index.temporal.bdb;

import java.io.File;
import java.util.Iterator;
import java.util.Properties;

import org.apache.jena.graph.BlankNodeId;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.kb_graph.KbGraph;
import com.bbn.parliament.kb_graph.index.Record;
import com.bbn.parliament.kb_graph.index.temporal.TemporalIndex;
import com.bbn.parliament.kb_graph.index.temporal.TemporalPropertyFunctionFactory;
import com.bbn.parliament.kb_graph.index.temporal.extent.TemporalExtent;
import com.bbn.parliament.kb_graph.index.temporal.extent.TemporalInstant;
import com.bbn.parliament.kb_graph.index.temporal.extent.TemporalInterval;
import com.bbn.parliament.kb_graph.util.FileUtil;
import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.EnvironmentLockedException;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.SecondaryConfig;
import com.sleepycat.je.SecondaryCursor;
import com.sleepycat.je.SecondaryDatabase;
import com.sleepycat.je.SecondaryKeyCreator;

/** @author dkolas */
public class PersistentTemporalIndex extends TemporalIndex {
	public static final String INDEX_SUB_DIR = "temporal";

	private static final Logger LOG = LoggerFactory.getLogger(PersistentTemporalIndex.class);

	private Database nodeIndexedDatabase;
	private Environment environment;
	private SecondaryDatabase startsDatabase;
	private SecondaryDatabase endsDatabase;

	private boolean closed = true;
	private DatabaseConfig dbConfig;
	private SecondaryConfig startsConfig;
	private SecondaryConfig endsConfig;
	private EnvironmentConfig envConfig;
	private String dirName;
	private long minEnd;
	private long maxEnd;
	private long minStart;
	private long maxStart;

	private static final String NODES_DATABASE_NAME = "nodes";
	private static final String STARTS_DATABASE_NAME = "starts";
	private static final String ENDS_DATABASE_NAME = "ends";

	private boolean initialized = false;
	private long size;

	public PersistentTemporalIndex(Graph graph, Properties configuration,
			String indexDir) {
		super(graph, configuration);
		intialize(indexDir);
	}

	/** {@inheritDoc} */
	@Override
	public void open() {
		if (!closed) {
			return;
		}

		try {
			if (!initialized) {
				initializeEnvironment(true);
			}
			nodeIndexedDatabase = environment.openDatabase(null, NODES_DATABASE_NAME, dbConfig);
			startsDatabase = environment.openSecondaryDatabase(null, STARTS_DATABASE_NAME,
					nodeIndexedDatabase, startsConfig);
			endsDatabase = environment.openSecondaryDatabase(null, ENDS_DATABASE_NAME,
					nodeIndexedDatabase, endsConfig);
			readMinAndMaxes();
			closed = false;
		} catch (DatabaseException e) {
			throw new RuntimeException("Could not open database", e);
		}
	}

	private void readMinAndMaxes() throws DatabaseException {
		minStart = 0L;
		maxStart = Long.MAX_VALUE;
		minEnd = 0L;
		maxEnd = Long.MAX_VALUE;

		DatabaseEntry key = new DatabaseEntry();
		DatabaseEntry value = new DatabaseEntry();

		try (SecondaryCursor cursor = startsDatabase.openCursor(null, CursorConfig.READ_UNCOMMITTED)) {
			if (cursor.getFirst(key, value, LockMode.READ_UNCOMMITTED) == OperationStatus.SUCCESS) {
				minStart = getLongForBytes(value.getData(), 0);
			}
			if (cursor.getLast(key, value, LockMode.READ_UNCOMMITTED) == OperationStatus.SUCCESS) {
				maxStart = getLongForBytes(value.getData(), 0);
			}
		}

		try (SecondaryCursor cursor = endsDatabase.openCursor(null, CursorConfig.READ_UNCOMMITTED)) {
			if (cursor.getFirst(key, value, LockMode.READ_UNCOMMITTED) == OperationStatus.SUCCESS) {
				minEnd = getLongForBytes(value.getData(), 8);
			}
			if (cursor.getLast(key, value, LockMode.READ_UNCOMMITTED) == OperationStatus.SUCCESS) {
				maxEnd = getLongForBytes(value.getData(), 8);
			}
		}

		size = nodeIndexedDatabase.count();
	}

	private static class StartsKeyCreator implements SecondaryKeyCreator {
		@Override
		public boolean createSecondaryKey(SecondaryDatabase arg0,
				DatabaseEntry keyEntry, DatabaseEntry dataEntry,
				DatabaseEntry resultEntry) throws DatabaseException {
			byte[] data = new byte[8];
			for (int i = 0; i < data.length; i++) {
				data[i] = dataEntry.getData()[i];
			}
			resultEntry.setData(data);
			return true;
		}
	}

	private static class EndsKeyCreator implements SecondaryKeyCreator {
		@Override
		public boolean createSecondaryKey(SecondaryDatabase arg0,
				DatabaseEntry keyEntry, DatabaseEntry dataEntry,
				DatabaseEntry resultEntry) throws DatabaseException {
			byte[] data = new byte[8];
			for (int i = 0; i < 8; i++) {
				data[i] = dataEntry.getData()[i + 8];
			}
			resultEntry.setData(data);
			return true;
		}
	}

	public static final void getBytesForLong(byte[] bytes, long value, int index) {
		bytes[index + 0] = (byte) (value >> 56);
		bytes[index + 1] = (byte) (value >> 48);
		bytes[index + 2] = (byte) (value >> 40);
		bytes[index + 3] = (byte) (value >> 32);
		bytes[index + 4] = (byte) (value >> 24);
		bytes[index + 5] = (byte) (value >> 16);
		bytes[index + 6] = (byte) (value >> 8);
		bytes[index + 7] = (byte) value;
	}

	public static final long getLongForBytes(byte[] bytes, int index) {
		return ((bytes[index + 0] & 0xffL) << 56)
				| ((bytes[index + 1] & 0xffL) << 48)
				| ((bytes[index + 2] & 0xffL) << 40)
				| ((bytes[index + 3] & 0xffL) << 32)
				| ((bytes[index + 4] & 0xffL) << 24)
				| ((bytes[index + 5] & 0xffL) << 16)
				| ((bytes[index + 6] & 0xffL) << 8) | ((bytes[index + 7] & 0xffL));
	}

	public static Node getNodeRepresentation(String representation) {
		Node result = null;
		if (representation.startsWith(KbGraph.MAGICAL_BNODE_PREFIX)) {
			result = NodeFactory.createBlankNode(BlankNodeId.create(representation
					.substring(KbGraph.MAGICAL_BNODE_PREFIX.length())));
		} else {
			result = NodeFactory.createURI(representation);
		}
		return result;
	}

	public static String getStringRepresentation(Node n) {
		String stringRep = n.toString();
		if (n.isBlank()) {
			stringRep = KbGraph.MAGICAL_BNODE_PREFIX + stringRep;
		}
		return stringRep;
	}

	/** @see com.bbn.parliament.kb_graph.index.temporal.TemporalIndex#clear() */
	@Override
	public void clear() {
		try {
			boolean init = initialized;
			close(false);
			if (init) {
				environment.truncateDatabase(null, ENDS_DATABASE_NAME, false);
				environment.truncateDatabase(null, STARTS_DATABASE_NAME, false);
				environment.truncateDatabase(null, NODES_DATABASE_NAME, false);
			}
			// initializeEnvironment(false);
			open();
		} catch (DatabaseException e) {
			throw new RuntimeException("Error with the BDB database", e);
		}
	}

	@Override
	public Iterator<Record<TemporalExtent>> iterator() {
		return new PersistentTemporalExtentIterator(this, 0, Long.MAX_VALUE, 0, Long.MAX_VALUE, TemporalExtent.class);
	}

	/**
	 *
	 * @param data The on-disk data to read the extent from.
	 * @return Either a {@link TemporalInterval} or a {@link TemporalInstant},
	 * depending on if the longs represent equal points in time.
	 */
	public static TemporalExtent dataToExtent(byte[] data) {
		long start = getLongForBytes(data, 0);
		long end = getLongForBytes(data, 8);
		if (start == end) {
			return new TemporalInstant(start);
		}
		TemporalInstant startInstant = new TemporalInstant(start, null, true);
		TemporalInstant endInstant = new TemporalInstant(end, null, false);
		return new TemporalInterval(startInstant, endInstant);
	}

	/** @see com.bbn.parliament.kb_graph.index.temporal.TemporalIndex#size() */
	@Override
	public long size() {
		if (closed) {
			return 0;
		}
		try {
			if (null == nodeIndexedDatabase) {
				return 0;
			}
			return size;
		} catch (DatabaseException e) {
			throw new RuntimeException("Error with the BDB database", e);
		}
	}

	@Override
	public void close() {
		if (!closed) {
			close(true);
		}
	}

	private void close(boolean closeEnvironment) {
		if (closed) {
			return;
		}
		try {
			endsDatabase.close();
			startsDatabase.close();
			nodeIndexedDatabase.close();
			if (closeEnvironment) {
				environment.cleanLog();
				environment.close();
				initialized = false;
			}
		} catch (DatabaseException e) {
			throw new RuntimeException("Database failed to close", e);
		}
		closed = true;
	}

	public static byte[] getBytesForLong(long value) {
		byte[] bytes = new byte[8];
		getBytesForLong(bytes, value, 0);
		return bytes;
	}

	public SecondaryDatabase getStartsDatabase() {
		return startsDatabase;
	}

	public void setStartsDatabase(SecondaryDatabase startsDatabase) {
		this.startsDatabase = startsDatabase;
	}

	public SecondaryDatabase getEndsDatabase() {
		return endsDatabase;
	}

	public void setEndsDatabase(SecondaryDatabase endsDatabase) {
		this.endsDatabase = endsDatabase;
	}

	public long estimateStartsForRange(long lowest, long highest) {
		return Math.min(highest, maxStart) - Math.max(lowest, minStart);
	}

	public long estimateEndsForRange(long lowest, long highest) {
		return Math.min(highest, maxEnd) - Math.max(lowest, minEnd);
	}

	public Database getPrimaryDatabase() {
		return nodeIndexedDatabase;
	}

	@SuppressWarnings("hiding")
	public long estimate(long minStart, long maxStart, long minEnd, long maxEnd) {
		if (alwaysUseFirst) {
			return 0;
		}
		//return Math.min(estimateStartsForRange(minStart, maxStart),
		//	estimateEndsForRange(minEnd, maxEnd));

		long countMinStart;
		long countMinEnd;
		long countMaxStart;
		long countMaxEnd;

		if (minStart == this.minStart) {
			countMinStart = size;
		} else {
			countMinStart = countRecords(minStart, true);
		}
		if (maxStart == minStart) {
			countMaxStart = countMinStart;
		} else {
			countMaxStart = countRecords(maxStart, true);
		}

		if (minEnd == this.minEnd) {
			countMinEnd = size;
		} else {
			countMinEnd = countRecords(minEnd, false);
		}
		if (maxEnd == minEnd) {
			countMaxEnd = countMinEnd;
		} else {
			countMaxEnd = countRecords(maxEnd, false);
		}


		long startsEstimate = countMinStart - countMaxStart;
		long endsEstimate = countMinEnd - countMaxEnd;

		return Math.min(startsEstimate, endsEstimate);
	}

	private long countRecords(long time, boolean starts) {
		@SuppressWarnings("resource")
		SecondaryDatabase sdb = (starts) ? getStartsDatabase() : getEndsDatabase();

		DatabaseEntry key = new DatabaseEntry(PersistentTemporalIndex.getBytesForLong(time));
		DatabaseEntry data = new DatabaseEntry();

		try (SecondaryCursor cursor = sdb.openCursor(null, CursorConfig.READ_UNCOMMITTED)) {
			OperationStatus status = cursor.getSearchKeyRange(key, data, LockMode.READ_UNCOMMITTED);
			if (OperationStatus.SUCCESS.equals(status)) {
				int count = 1;
				long start = System.currentTimeMillis();
				while (OperationStatus.SUCCESS.equals(cursor.getNext(key, data, LockMode.READ_UNCOMMITTED))) {
					++count;
				}
				long length = System.currentTimeMillis() - start;
				LOG.debug("Search took: " + length);
				return count;
			}
		}
		return Long.MAX_VALUE;
	}

	private void intialize(String indexDir) {
		dirName = indexDir + File.separatorChar + INDEX_SUB_DIR;
	}

	private void initializeEnvironment(boolean newEnvironment)
			throws EnvironmentLockedException, DatabaseException {
		if (newEnvironment) {
			File dirFile = new File(dirName);
			dirFile.mkdirs();

			envConfig = new EnvironmentConfig();
			envConfig.setAllowCreate(true);
			envConfig.setLocking(false);
			envConfig.setReadOnly(false);
			envConfig.setTransactional(false);

			environment = new Environment(dirFile, envConfig);
		}
		dbConfig = new DatabaseConfig();
		dbConfig.setAllowCreate(true);
		dbConfig.setDeferredWrite(true);
		dbConfig.setReadOnly(false);
		dbConfig.setTransactional(false);

		// Create secondary database for indexing Starts
		startsConfig = new SecondaryConfig();
		startsConfig.setAllowCreate(true);
		startsConfig.setSortedDuplicates(true);
		startsConfig.setAllowPopulate(true);
		startsConfig.setKeyCreator(new StartsKeyCreator());

		// Create secondary database for indexing Ends
		endsConfig = new SecondaryConfig();
		endsConfig.setAllowCreate(true);
		endsConfig.setSortedDuplicates(true);
		endsConfig.setAllowPopulate(true);
		endsConfig.setKeyCreator(new EndsKeyCreator());
		initialized = true;
	}

	/** {@inheritDoc} */
	@Override
	public void delete() {
		// (J. Craig, 2019-01-07)
		// The old code closed the index if it was open.  The test suite expects this method to throw an Exception
		// (see com.bbn.parliament.jena.graph.index.IndexBase).  I changed this code based upon the assumption that the
		// test suite is correct.
		if (!closed) {
			throw new IllegalStateException("Index open");
		}
		File dirFile = new File(dirName);
		if (dirFile.exists()) {
			FileUtil.delete(dirFile);
		}
	}

	/** {@inheritDoc} */
	@Override
	public boolean isClosed() {
		return closed;
	}

	@Override
	public boolean add(Record<TemporalExtent> r) {
		Node node = r.getKey();
		TemporalExtent extent = r.getValue();

		// (J. Craig, 2019-01-07)
		// The BDB documentation for DatabaseConfig::setSortedDuplicates() states:
		//   "Although two records may have the same key, they may not also have the same data item. Two identical
		//   records, that have the same key and data, may not be stored in a database."
		// Despite nodeIndexedDatabase's not being configured to allow duplicates, the behavior before I added this
		// block made it seem like trying to add the same record twice (as we do in IndexTestMethods) was a violation of
		// an assumption that this API does not enforce.
		// Thus, the overhead of checking to see whether the value previously existed seems to unfortunately be
		// necessary.
		Record<TemporalExtent> previous = find(node);
		if (previous != null && extent.sameAs(previous.getValue())) {
			return false;
		}

		DatabaseEntry key = convertToKey(node);
		DatabaseEntry data = new DatabaseEntry();
		byte[] databytes = new byte[16];

		minStart = Math.min(minStart, extent.getStart().getInstant());
		maxStart = Math.max(maxStart, extent.getStart().getInstant());
		minEnd = Math.min(minEnd, extent.getEnd().getInstant());
		maxEnd = Math.max(maxEnd, extent.getEnd().getInstant());

		getBytesForLong(databytes, extent.getStart().getInstant(), 0);
		getBytesForLong(databytes, extent.getEnd().getInstant(), 8);
		data.setData(databytes);
		try {
			nodeIndexedDatabase.put(null, key, data);
			size++;
		} catch (DatabaseException e) {
			throw new RuntimeException("Error with the BDB database", e);
		}

		return true;
	}

	private static DatabaseEntry convertToKey(Node node) {
		DatabaseEntry key = new DatabaseEntry();
		key.setData(getStringRepresentation(node).getBytes());
		return key;
	}

	/** {@inheritDoc} */
	@Override
	public void add(Iterator<Record<TemporalExtent>> records) {
		while (records.hasNext()) {
			add(records.next());
		}
	}

	@Override
	public boolean remove(Record<TemporalExtent> r) {
		Node node = r.getKey();
		DatabaseEntry key = convertToKey(node);
		boolean success = false;
		try {
			OperationStatus status = nodeIndexedDatabase.delete(null, key);
			if (status.equals(OperationStatus.SUCCESS)) {
				success = true;
				--size;
			}
		} catch (DatabaseException e) {
			throw new RuntimeException("Error with the BDB database", e);
		}
		return success;
	}

	/** {@inheritDoc} */
	@Override
	public void remove(Iterator<Record<TemporalExtent>> records) {
		while (records.hasNext()) {
			remove(records.next());
		}
	}

	@Override
	public Record<TemporalExtent> find(Node node) {
		DatabaseEntry key = new DatabaseEntry(getStringRepresentation(node).getBytes());
		DatabaseEntry value = new DatabaseEntry();
		try {
			nodeIndexedDatabase.get(null, key, value, LockMode.READ_UNCOMMITTED);
			if (null == value.getData()) {
				return null;
			}
			return Record.create(node, dataToExtent(value.getData()));
		} catch (DatabaseException e) {
			throw new RuntimeException("Error retrieving from the db");
		}
	}

	/** {@inheritDoc} */
	@Override
	public Iterator<Record<TemporalExtent>> query(TemporalExtent value) {
		return new PersistentTemporalExtentIterator(this,
				value.getStart().getInstant(), value.getStart().getInstant(),
				value.getEnd().getInstant(), value.getEnd().getInstant(), TemporalExtent.class);
	}

	/** {@inheritDoc} */
	@Override
	protected TemporalPropertyFunctionFactory<PersistentTemporalIndex> getPropertyFunctionFactory() {
		return new PersistentPropertyFunctionFactory();
	}
}
