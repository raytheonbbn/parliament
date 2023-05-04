package com.bbn.parliament.jena.graph.index.spatial;

import java.io.File;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.sparql.function.FunctionRegistry;
import org.apache.jena.sparql.pfunction.PropertyFunctionRegistry;
import org.apache.jena.util.iterator.ClosableIterator;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;

import com.bbn.parliament.jena.graph.index.spatial.persistence.NodeData;
import com.bbn.parliament.jena.graph.index.spatial.persistence.NodeIdKeyCreator;
import com.bbn.parliament.jena.graph.index.spatial.persistence.NodeKey;
import com.bbn.parliament.jena.graph.index.spatial.standard.SpatialGeometryFactory;
import com.bbn.parliament.kb_graph.index.IndexBase;
import com.bbn.parliament.kb_graph.index.IndexException;
import com.bbn.parliament.kb_graph.index.QueryableIndex;
import com.bbn.parliament.kb_graph.index.Record;
import com.bbn.parliament.kb_graph.index.RecordFactory;
import com.bbn.parliament.kb_graph.query.PrefixRegistry;
import com.bbn.parliament.kb_graph.query.index.QueryCache;
import com.bbn.parliament.kb_graph.util.FileUtil;
import com.bbn.parliament.kb_graph.util.NodeUtil;
import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.serial.ClassCatalog;
import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.collections.StoredSortedMap;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.EnvironmentLockedException;
import com.sleepycat.je.SecondaryConfig;
import com.sleepycat.je.SecondaryDatabase;
import com.sleepycat.je.Transaction;

/** @author rbattle */
public abstract class SpatialIndex extends IndexBase<Geometry> implements QueryableIndex<Geometry> {
	private static final String ID_NODE_INDEX = "id_node_index";
	private static final String DATA_DB = "data";
	private static final String CATALOG_DB = "catalog";

	private static final ThreadLocal<WKBWriter> WKB_WRITER = new ThreadLocal<>() {
		@Override
		protected WKBWriter initialValue() {
			return new WKBWriter();
		}
	};

	private static final ThreadLocal<WKBReader> WKB_READER = new ThreadLocal<>() {
		@Override
		protected WKBReader initialValue() {
			return new WKBReader(SpatialGeometryFactory.GEOMETRY_FACTORY);
		}
	};

	private static final AtomicInteger INDEX_COUNTER = new AtomicInteger(0);

	/**
	 * Get the <code>Geometry</code> give the well known binary representation.
	 *
	 * @param data The indexed data.
	 * @return a <code>Geometry</code>.
	 * @throws ParseException If an error occurs parsing the data.
	 */
	protected static final Geometry getGeometryRepresentation(NodeData data)
		throws ParseException {
		byte[] wkb = data.getExtent();
		Geometry g = WKB_READER.get().read(wkb);
		g.setUserData(data.getCRSCode());
		return g;
	}

	// bdb members
	private Database catalogDb;
	private ClassCatalog catalog;
	private Environment env;
	private Database db;
	private SecondaryDatabase nodeDb;
	private DatabaseConfig dbConfig;
	private SecondaryConfig secConfig;

	private AtomicInteger idCounter;

	private Object lock = new Object();
	private boolean bulkLoading = false;
	private boolean hasChanged = false;

	protected Properties configuration;
	protected ThreadLocal<QueryCache<Geometry>> cache;

	protected StoredSortedMap<NodeKey, NodeData> nodes;
	protected StoredSortedMap<Integer, NodeData> idNodes;

	protected File indexDir;

	protected long size;

	protected Profile profile;

	public SpatialIndex(Profile profile, Properties configuration, String indexDir) {
		this.profile = profile;
		this.configuration = configuration;

		cache = new ThreadLocal<>() {
			@Override
			protected QueryCache<Geometry> initialValue() {
				return new QueryCache<>(Constants.QUERY_CACHE_SIZE);
			}
		};

		this.indexDir = new File(new File(indexDir), "spatial");
		idCounter = new AtomicInteger();

		dbConfig = new DatabaseConfig();
		dbConfig.setTransactional(true);
		dbConfig.setAllowCreate(true);

		secConfig = new SecondaryConfig();
		secConfig.setTransactional(true);
		secConfig.setAllowCreate(true);
		secConfig.setSortedDuplicates(false);
	}

	/** {@inheritDoc} */
	@Override
	public final QueryCache<Geometry> getQueryCache() {
		return cache.get();
	}

	private void initializeEnvironment() throws SpatialIndexException {
		try {
			indexDir.mkdirs();

			EnvironmentConfig envConfig = new EnvironmentConfig();
			envConfig.setTransactional(true);
			envConfig.setAllowCreate(true);
			//envConfig.setCacheMode(CacheMode.DEFAULT);
			envConfig.setCachePercent(20);
			envConfig.setSharedCache(true);
			envConfig
			.setConfigParam(EnvironmentConfig.CHECKPOINTER_BYTES_INTERVAL,
				"40000000"); // 40M
			envConfig.setConfigParam(EnvironmentConfig.LOG_FILE_MAX, "10000000"); // 10M
			envConfig.setConfigParam(EnvironmentConfig.CLEANER_EXPUNGE, "true");

			env = new Environment(indexDir, envConfig);

			openDB();
		} catch (EnvironmentLockedException ex) {
			throw new SpatialIndexException(this, "Error while initializing environment", ex);
		} catch (DatabaseException ex) {
			throw new SpatialIndexException(this, "Error while initializing maps", ex);
		}
	}

	private void createMaps() {
		EntryBinding<NodeKey> nodeKeyBinding = new SerialBinding<>(catalog, NodeKey.class);
		EntryBinding<NodeData> nodeDataBinding = new SerialBinding<>(catalog, NodeData.class);
		EntryBinding<Integer> nodeBinding = new SerialBinding<>(catalog, Integer.class);

		nodes = new StoredSortedMap<>(db, nodeKeyBinding, nodeDataBinding, true);
		idNodes = new StoredSortedMap<>(nodeDb, nodeBinding, nodeDataBinding, true);

		Integer maxInt = idNodes.lastKey();
		if (null == maxInt) {
			maxInt = 0;
		} else {
			maxInt = maxInt + 1;
		}
		idCounter.set(maxInt);

		size = db.count();
	}

	private void closeDB() throws SpatialIndexException {
		try {
			if (nodeDb != null) {
				nodeDb.close();
			}
			if (db != null) {
				db.close();
			}
			if (catalog != null) {
				catalog.close();
			}
			if (catalogDb != null) {
				catalogDb.close();
			}
		} catch (DatabaseException ex) {
			throw new SpatialIndexException(this, "Error closing databases", ex);
		}
	}

	private void closeEnvironment() {
		if (env != null) {
			try {
				env.cleanLog();
				env.close();
			} catch (DatabaseException ex) {
				throw new SpatialIndexException(this, "Error closing environment", ex);
			}
		}
	}

	private void openDB() throws SpatialIndexException {
		try {
			Transaction t = env.beginTransaction(null, null);
			catalogDb = env.openDatabase(t, CATALOG_DB, dbConfig);
			catalog = new StoredClassCatalog(catalogDb);

			db = env.openDatabase(t, DATA_DB, dbConfig);

			secConfig.setKeyCreator(new NodeIdKeyCreator(catalog));
			nodeDb = env.openSecondaryDatabase(t, ID_NODE_INDEX, db, secConfig);
			t.commit();
		} catch (Exception ex) {
			throw new SpatialIndexException(this, "Error opening databases", ex);
		}
		createMaps();
	}

	/** {@inheritDoc} */
	@Override
	public final void doClose() throws IndexException {
		synchronized (lock) {
			indexClose();
			closeDB();
			closeEnvironment();
		}
	}

	/**
	 * {@inheritDoc} This calls {@link SpatialIndex#indexOpen()} to open any
	 * resources held by the concrete implementation.
	 */
	@Override
	public final void doOpen() throws IndexException {
		synchronized (lock) {
			initializeEnvironment();
			indexOpen();
		}
	}

	/**
	 * {@inheritDoc} This calls {@link SpatialIndex#indexDelete()} to delete any
	 * resources held by the concrete implementation.
	 */
	@Override
	public final void doDelete() throws IndexException {
		synchronized (lock) {
			indexDelete();
			FileUtil.delete(indexDir);
		}
	}

	/**
	 * {@inheritDoc} This calls {@link SpatialIndex#indexClose()} to close any
	 * resources held by the concrete implementation.
	 */
	@Override
	public final void doClear() throws IndexException {
		synchronized (lock) {
			indexClear();
			closeDB();
			env.truncateDatabase(null, ID_NODE_INDEX, false);
			env.truncateDatabase(null, DATA_DB, false);
			openDB();
			idCounter.set(0);
			hasChanged = true;
		}
	}

	/**
	 * {@inheritDoc} This calls {@link SpatialIndex#indexAdd(Record)} to actually
	 * add the record to the spatial index.
	 *
	 * @throws SpatialIndexException if an error occurs parsing a geometry that
	 *         already exists in the index for the given key.
	 */
	@Override
	protected final boolean doAdd(Record<Geometry> r)
		throws SpatialIndexException {
		return doAdd(r, null);
	}

	private final boolean doAdd(Record<Geometry> r, Transaction t) {
		synchronized (lock) {
			String n = NodeUtil.getStringRepresentation(r.getKey());
			NodeKey key = new NodeKey(n);
			NodeData data = nodes.get(key);

			Geometry previous = null;

			int id = 0;
			if (null != data) {
				id = data.getId();
				try {
					previous = getGeometryRepresentation(data);
				} catch (ParseException e) {
					throw new SpatialIndexException(this, e);
				}

				if (r.getValue().equals(previous)) {
					return false;
				}
			} else {
				id = idCounter.getAndIncrement();
			}
			byte[] extent = WKB_WRITER.get().write(r.getValue());
			data = new NodeData(id, n, extent, (String) r.getValue().getUserData());

			nodes.put(key, data);
			hasChanged = true;
			if (null != previous && !previous.equals(r.getValue())) {
				return indexUpdate(r, GeometryRecord.create(r.getKey(), previous));
			}
			boolean add = indexAdd(r);
			if (add) {
				size++;
			}
			return add;
		}
	}

	/**
	 * Update a record.
	 *
	 * @param r the record to update.
	 * @param previous the previous data.
	 * @return true if the data was updated.
	 * @throws SpatialIndexException if an error occurs.
	 */
	protected boolean indexUpdate(Record<Geometry> r, Record<Geometry> previous)
		throws SpatialIndexException {
		synchronized (lock) {
			boolean remove = indexRemove(previous);
			boolean add = indexAdd(r);
			return remove && add;
		}
	}

	/**
	 * {@inheritDoc} This calls {@link SpatialIndex#indexRemove(Record)} to
	 * actually remove the record from the spatial index.
	 */
	@Override
	protected final boolean doRemove(Record<Geometry> r) {
		synchronized (lock) {
			boolean removed = indexRemove(r);
			if (removed) {
				String n = NodeUtil.getStringRepresentation(r.getKey());
				NodeKey key = new NodeKey(n);
				NodeData value = nodes.remove(key);
				if (null == value) {
					return false;
				}
				size--;
				hasChanged = true;
				return true;
			}
			return false;
		}
	}

	protected boolean isBulkLoading() {
		return bulkLoading;
	}

	protected boolean hasChanged() {
		return hasChanged;
	}

	/** {@inheritDoc} */
	@Override
	protected void doAdd(Iterator<Record<Geometry>> records) throws IndexException {
		synchronized (lock) {
			bulkLoading = true;
			while (records.hasNext()) {
				doAdd(records.next());
			}
			bulkLoading = false;
		}
	}

	/** {@inheritDoc} */
	@Override
	protected void doRemove(Iterator<Record<Geometry>> records) throws IndexException {
		synchronized (lock) {
			while (records.hasNext()) {
				doRemove(records.next());
			}
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @throws SpatialIndexException if an error occurs while parsing the
	 *         geometry from the index.
	 */
	@Override
	public final Record<Geometry> find(Node node) throws SpatialIndexException {
		String n = NodeUtil.getStringRepresentation(node);
		NodeKey key = new NodeKey(n);
		NodeData data = null;
		synchronized (lock) {
			data = nodes.get(key);
		}
		if (null == data) {
			return null;
		}
		Geometry g;
		try {
			g = getGeometryRepresentation(data);
		} catch (ParseException ex) {
			throw new SpatialIndexException(this, ex);
		}
		return GeometryRecord.create(node, g);

	}

	/** {@inheritDoc} */
	@Override
	public final Iterator<Record<Geometry>> doIterator() {
		final Iterator<NodeData> iter = nodes.values().iterator();
		final SpatialIndex index = this;
		ClosableIterator<Record<Geometry>> it = new ClosableIterator<>() {
			@Override
			public void remove() {
			}

			@Override
			public Record<Geometry> next() {
				NodeData data = iter.next();
				Node n = NodeUtil.getNodeRepresentation(data.getNode());
				Geometry g;
				try {
					g = getGeometryRepresentation(data);
				} catch (ParseException ex) {
					throw new SpatialIndexException(index, ex);
				}
				GeometryRecord r = GeometryRecord.create(n, g);
				return r;
			}

			@Override
			public boolean hasNext() {
				return iter.hasNext();
			}

			@Override
			public void close() {
				// iter is a BlockIterator and does not need closing
			}
		};
		return it;
	}

	/** {@inheritDoc} */
	@Override
	public final long doSize() throws IndexException {
		return size;
	}

	/** {@inheritDoc} */
	@Override
	public final RecordFactory<Geometry> getRecordFactory() {
		return profile.getRecordFactory();
	}

	/**
	 * Return all records that intersect with the given geometry. This is a full
	 * intersection test, not just the bounding box.
	 *
	 * @param geometry
	 *           the geometry to test
	 * @return all records that intersect with the geometry.
	 */
	public abstract Iterator<Record<Geometry>> iterator(Geometry geometry,
		Operation operation) throws SpatialIndexException;

	protected abstract long estimate(Geometry geometry, Operation operation) throws SpatialIndexException;

	/** {@inheritDoc} */
	@Override
	public void register(Graph graph, Node graphName) {
		if (INDEX_COUNTER.getAndIncrement() == 0) {
			// register prefixes
			PrefixMap prefixes = profile.getPrefixes();
			if (null != prefixes) {
				PrefixRegistry.getInstance().registerPrefixes(prefixes);
			}
			// register properties
			PropertyFunctionRegistry pfuncRegistry = PropertyFunctionRegistry.get();
			IterablePropertyFunctionFactory pfuncFactory = profile.getPropertyFunctionFactory();

			for (String uri : pfuncFactory) {
				pfuncRegistry.put(uri, pfuncFactory);
			}

			// register functions
			FunctionRegistry fregistry = FunctionRegistry.get();
			IterableFunctionFactory funcFactory = profile.getFunctionFactory();
			if (null != funcFactory) {
				for (String uri : funcFactory) {
					fregistry.put(uri, funcFactory);
				}
			}
		}
	}

	/** {@inheritDoc} */
	@Override
	public void unregister(Graph graph, Node graphName) {
		if (INDEX_COUNTER.decrementAndGet() == 0) {
			// unregister prefixes
			PrefixMap prefixes = profile.getPrefixes();
			if (null != prefixes) {
				PrefixRegistry.getInstance().removePrefixes(prefixes);
			}

			// remove properties
			PropertyFunctionRegistry pfuncRegistry = PropertyFunctionRegistry.get();
			IterablePropertyFunctionFactory pfuncFactory = profile.getPropertyFunctionFactory();

			for (String uri : pfuncFactory) {
				pfuncRegistry.remove(uri);
			}

			// remove functions
			FunctionRegistry funcRegistry = FunctionRegistry.get();
			IterableFunctionFactory funcFactory = profile.getFunctionFactory();
			if (null != funcFactory) {
				for (String uri : funcFactory) {
					funcRegistry.remove(uri);
				}
			}
		}
	}

	/**
	 * Open this instance.
	 *
	 * @throws SpatialIndexException if an error occurs while opening
	 */
	protected abstract void indexOpen() throws SpatialIndexException;

	/**
	 * Delete any resources handled by this instance.
	 *
	 * @throws SpatialIndexException if an error occurs while deleting.
	 */
	protected abstract void indexDelete() throws SpatialIndexException;

	/**
	 * Clear all records.
	 *
	 * @throws SpatialIndexException if an error occurs while clearing.
	 */
	protected abstract void indexClear() throws SpatialIndexException;

	/**
	 * Close this instance.
	 *
	 * @throws SpatialIndexException if an error occurs while clearing.
	 */
	protected abstract void indexClose() throws SpatialIndexException;

	/**
	 * Add a record to the index. If a previous record exists, delete it.
	 *
	 * @param r a record to add.
	 * @return <code>true</code> if the record is added; otherwise
	 *         <code>false</code>.
	 * @throws SpatialIndexException if an error occurs while adding the record.
	 */
	protected abstract boolean indexAdd(Record<Geometry> r)
		throws SpatialIndexException;

	/**
	 * Remove the record from the index.
	 *
	 * @param r a record to delete.
	 * @return <code>true</code> if the record is removed; otherwise
	 *         <code>false</code>.
	 * @throws SpatialIndexException if an error occurs while removing the
	 *         record.
	 */
	protected abstract boolean indexRemove(Record<Geometry> r)
		throws SpatialIndexException;

	/** {@inheritDoc} */
	@Override
	public void flush() throws IndexException {
		synchronized (lock) {
			env.cleanLog();
			env.sync();
			hasChanged = false;
		}
	}
}
