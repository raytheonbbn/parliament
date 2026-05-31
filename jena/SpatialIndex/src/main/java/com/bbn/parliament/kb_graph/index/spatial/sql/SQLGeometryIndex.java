// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.kb_graph.index.spatial.sql;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.kb_graph.index.spatial.Constants;
import com.bbn.parliament.kb_graph.index.spatial.Profile;
import com.bbn.parliament.kb_graph.index.spatial.SpatialIndex;
import com.bbn.parliament.kb_graph.index.spatial.SpatialIndexException;

/** @author Robert Battle */
public abstract class SQLGeometryIndex extends SpatialIndex {
	protected static Logger LOG = LoggerFactory.getLogger(SQLGeometryIndex.class);

	protected PersistentStore store;
	protected String tableName;
	private String jdbcUrl;
	private String userName;
	private String password;

	@SuppressWarnings("this-escape")
	public SQLGeometryIndex(Profile profile, Properties configuration,
			String cleanGraphName, String indexDir) {
		super(profile, configuration, indexDir);
		store = null;
		tableName = cleanGraphName;
		jdbcUrl = configuration.getProperty(Constants.JDBC_URL);
		userName = configuration.getProperty(Constants.USERNAME);
		password = configuration.getProperty(Constants.PASSWORD);

		if (jdbcUrl == null || jdbcUrl.isBlank()) {
			throw new SpatialIndexException(this,
				"Property '%1$s' must be set.".formatted(Constants.JDBC_URL));
		}
	}

	protected abstract void indexOpenSQL();

	public String getTableName() {
		return tableName;
	}

	@Override
	protected void indexOpen() {
		store = PersistentStore.getInstance();
		store.initialize(jdbcUrl, userName, password, configuration);
		indexOpenSQL();
	}
}
