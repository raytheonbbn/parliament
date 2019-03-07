// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
package com.bbn.parliament.jena.graph.index.temporal.bdb;

import java.io.File;
import java.util.Iterator;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jena.graph.KbGraph;
import com.bbn.parliament.jena.graph.index.Record;
import com.bbn.parliament.jena.graph.index.temporal.TemporalIndex;
import com.bbn.parliament.jena.graph.index.temporal.TemporalPropertyFunctionFactory;
import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalExtent;
import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalInstant;
import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalInterval;
import com.bbn.parliament.jena.util.FileUtil;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.AnonId;
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
		DatabaseEntry key = new DatabaseEntry();
		DatabaseEntry value = new DatabaseEntry();

		try (SecondaryCursor cursor = startsDatabase.openCursor(null, CursorConfig.READ_UNCOMMITTED)) {
			OperationStatus status = cursor.getFirst(key, value, LockMode.READ_UNCOMMITTED);
			if (status == OperationStatus.SUCCESS) {
				// If we get one thing back, there is at least one element in the db
				minStart = getLongForBytes(value.getData(), 0);
				cursor.getLast(key, value, LockMode.READ_UNCOMMITTED);
				maxStart = getLongForBytes(value.getData(), 0);
			} else {
				minStart = 0L;
				maxStart = Long.MAX_VALUE;
			}
		}

		try (SecondaryCursor cursor = endsDatabase.openCursor(null, CursorConfig.READ_UNCOMMITTED)) {
			OperationStatus status = cursor.getFirst(key, value, LockMode.READ_UNCOMMITTED);
			if (status == OperationStatus.SUCCESS) {
				// If we get one thing back, there is at least one element in the db
				minEnd = getLongForBytes(value.getData(), 8);
				cursor.getLast(key, value, LockMode.READ_UNCOMMITTED);
				maxEnd = getLongForBytes(value.getData(), 8);
			} else {
				minEnd = 0L;
				maxEnd = Long.MAX_VALUE;
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
			result = Node.createAnon(AnonId.create(representation
				.substring(KbGraph.MAGICAL_BNODE_PREFIX.length())));
		} else {
			result = Node.createURI(representation);
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

	/** @see com.bbn.parliament.jena.graph.index.temporal.TemporalIndex#clear() */
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
		return new PersistentTemporalExtentIterator(this, 0, Long.MAX_VALUE, 0, Long.MAX_VALUE);
	}

	public static TemporalExtent dataToExtent(byte[] data) {
		long start = getLongForBytes(data, 0);
		long end = getLongForBytes(data, 8);
		if (start == end) {
			return new TemporalInstant(start);
		}
		TemporalInstant startInstant = new TemporalInstant(start);
		TemporalInstant endInstant = new TemporalInstant(end);
		return new TemporalInterval(startInstant, endInstant);
	}

	/** @see com.bbn.parliament.jena.graph.index.temporal.TemporalIndex#size() */
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

	public long estimate(long minimumStart, long maximumStart, long minimumEnd, long maximumEnd) {
		if (alwaysUseFirst) {
			return 0;
		}
		//return Math.min(estimateStartsForRange(minimumStart, maximumStart),
		//	estimateEndsForRange(minimumEnd, maximumEnd));

		long countMinStart = (minimumStart == minStart)
			? size
			: countRecords(minimumStart, true);
		long countMaxStart = (maximumStart == minimumStart)
			? countMinStart
			: countRecords(maximumStart, true);

		long countMinEnd = (minimumEnd == minEnd)
			? size
			: countRecords(minimumEnd, false);
		long countMaxEnd = (maximumEnd == minimumEnd)
			? countMinEnd
			: countRecords(maximumEnd, false);

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
			OperationStatus status = cursor.getSearchKeyRange( key, data, LockMode.READ_UNCOMMITTED);
			if (OperationStatus.SUCCESS.equals(status)) {
				int count = 1;
				long start = System.currentTimeMillis();
				while (OperationStatus.SUCCESS.equals(cursor.getNext(key, data, LockMode.READ_UNCOMMITTED))) {
					count++;
				}
				long length = System.currentTimeMillis() - start;
				LOG.debug("Search took: " + length);
				return count;
			}
		}
		return Long.MAX_VALUE;
	}

	private void intialize(String indexDir) {
		dirName = indexDir + File.separatorChar + "temporal";
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
		if (!closed) {
			close();
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
		DatabaseEntry key = new DatabaseEntry();
		DatabaseEntry data = new DatabaseEntry();
		key.setData(getStringRepresentation(node).getBytes());
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
		DatabaseEntry key = new DatabaseEntry(getStringRepresentation(node)
			.getBytes());
		boolean success = false;
		try {
			OperationStatus status = nodeIndexedDatabase.delete(null, key);
			if (status.equals(OperationStatus.SUCCESS)) {
				success = true;
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
			value.getEnd().getInstant(), value.getEnd().getInstant());
	}

	/** {@inheritDoc} */
	@Override
	protected TemporalPropertyFunctionFactory<PersistentTemporalIndex> getPropertyFunctionFactory() {
		return new PersistentPropertyFunctionFactory();
	}
}
