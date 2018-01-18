// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.graph.index.spatial.sql.postgres;

import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.junit.Ignore;

import com.bbn.parliament.jena.graph.index.IndexFactory;
import com.bbn.parliament.jena.graph.index.spatial.AbstractIndexTest;
import com.bbn.parliament.jena.graph.index.spatial.SpatialIndex;
import com.bbn.parliament.jena.graph.index.spatial.SpatialIndexFactory;
import com.bbn.parliament.jena.graph.index.spatial.sql.PersistentStore;
import com.bbn.parliament.jena.graph.index.spatial.sql.PersistentStoreException;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.vividsolutions.jts.geom.Geometry;

/** @author Robert Battle */
@Ignore
public class PostgresIndexTest extends AbstractIndexTest {
	@SuppressWarnings("static-method")
	protected Properties getProperties() {
		return PostgresPropertyFactory.create();
	}

	@Override
	protected IndexFactory<SpatialIndex, Geometry> getIndexFactory() {
		SpatialIndexFactory f = new SpatialIndexFactory();
		f.configure(getProperties());
		return f;
	}

	@Override
	protected boolean checkDeleted(SpatialIndex index, Graph graph, Node graphName) {
		boolean del = super.checkDeleted(index, graph, graphName);
		del &= checkSQLDeleted(index);
		return del;
	}

	private static boolean checkSQLDeleted(SpatialIndex index) {
		PostgresIndex p = (PostgresIndex) index;
		PersistentStore store = PersistentStore.getInstance();
		String tableName = p.getTableName();
		boolean contains = false;
		try (Connection c = store.getConnection()) {
			// check if spatial table exists
			String sql1 = String.format("SELECT relname FROM pg_class WHERE relname = '%1$s'", tableName);
			try (ResultSet rs = c.createStatement().executeQuery(sql1)) {
				contains |= rs.next();
			}

			// check if node index exists
			String sql2 = String.format(
				"SELECT indexname from pg_indexes where tablename = '%1$s' AND indexname NOT IN ('%1$s', '%1$s_pkey')",
				tableName);
			try (ResultSet rs = c.createStatement().executeQuery(sql2)) {
				contains |= rs.next();
			}
		} catch (PersistentStoreException | SQLException ex) {
			fail();
		}
		return !contains;
	}
}
