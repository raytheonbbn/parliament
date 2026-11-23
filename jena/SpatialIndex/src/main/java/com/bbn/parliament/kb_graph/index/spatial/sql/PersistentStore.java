// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.kb_graph.index.spatial.sql;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author Robert Battle */
public class PersistentStore {
	private static Logger LOG = LoggerFactory.getLogger(PersistentStore.class);

	private static class PersistentStoreHolder {
		public static final PersistentStore INSTANCE = new PersistentStore();
	}

	public static PersistentStore getInstance() {
		return PersistentStoreHolder.INSTANCE;
	}

	private DataSource ds;
	private ConnectionFactory connFactory;
	private PoolableConnectionFactory poolableFactory;
	private ObjectPool connectionPool;
	private boolean closed;
	private boolean initialized;
	private Object lock = new Object();

	private PersistentStore() {
		initialized = false;
		closed = true;
	}

	public void initialize(String jdbcUrl, String userName, String password,
		Properties p) {
		synchronized (lock) {
			if (initialized) {
				return;
			}
			GenericObjectPool.Config config = new GenericObjectPool.Config();

			config.maxActive = Integer.parseInt(p.getProperty("maxActive", "30"));
			config.maxIdle = Integer.parseInt(p.getProperty("maxIdle", "5"));
			config.maxWait = Integer.parseInt(p.getProperty("maxWait", "20000"));
			config.testOnReturn = Boolean.parseBoolean(p
				.getProperty("testOnReturn", Boolean.FALSE.toString()));
			config.testOnBorrow = Boolean.parseBoolean(p
				.getProperty("testOnBorrow", Boolean.FALSE.toString()));
			config.testWhileIdle = Boolean.parseBoolean(p
				.getProperty("testWhileIdle", Boolean.TRUE.toString()));
			config.timeBetweenEvictionRunsMillis = Integer.parseInt(p
				.getProperty("timeBetweenEvictionRunsMillis", "500"));
			config.minEvictableIdleTimeMillis = Integer.parseInt(p
				.getProperty("minEvictableIdleTimeMillis", "20000"));
			config.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_BLOCK;

			connectionPool = new GenericObjectPool(null, config);
			connFactory = new DriverManagerConnectionFactory(jdbcUrl, userName,
				password);
			poolableFactory = new PoolableConnectionFactory(connFactory,
				connectionPool,
				null, null, false,
				true);

			ds = new PoolingDataSource(connectionPool);
			closed = false;
			initialized = true;
		}
	}

	public Connection getConnection() throws PersistentStoreException {
		Connection con = null;
		try {
			con = ds.getConnection();
		} catch (SQLException e) {
			LOG.error("Could not get a connection", e);
			LOG.error("{} active, {} idle", getNumActive(), getNumIdle());
			throw new PersistentStoreException("Could not get a connection", e);
		}
		return con;
	}

	public int getNumActive() {
		return poolableFactory.getPool().getNumActive();
	}

	public int getNumIdle() {
		return poolableFactory.getPool().getNumIdle();
	}

	public void close() {
		synchronized (lock) {
			if (closed) {
				return;
			}
			try {
				LOG.debug("{} active, {} idle", getNumActive(), getNumIdle());
				poolableFactory.getPool().close();
				closed = true;
			} catch (Exception e) {
				LOG.error("Exception: ", e);
			}
		}
	}

	/** Closes the given connection if it is not null. */
	public static void close(Connection s) {
		if (s == null) {
			return;
		}
		try {
			if (!s.isClosed()) {
				s.close();
			}
		} catch (SQLException e) {
			LOG.error("Error while closing connection", e);
		}
	}
}
