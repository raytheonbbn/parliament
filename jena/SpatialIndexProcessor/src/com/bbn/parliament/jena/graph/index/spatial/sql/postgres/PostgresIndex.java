// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.graph.index.spatial.sql.postgres;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.Properties;

import org.apache.jena.graph.Node;
import org.locationtech.jts.geom.Geometry;

import com.bbn.parliament.jena.graph.index.spatial.Constants;
import com.bbn.parliament.jena.graph.index.spatial.GeometryConverter;
import com.bbn.parliament.jena.graph.index.spatial.Operation;
import com.bbn.parliament.jena.graph.index.spatial.Profile;
import com.bbn.parliament.jena.graph.index.spatial.SpatialIndexException;
import com.bbn.parliament.jena.graph.index.spatial.sql.PersistentStore;
import com.bbn.parliament.jena.graph.index.spatial.sql.PersistentStoreException;
import com.bbn.parliament.jena.graph.index.spatial.sql.ResultSetIterator;
import com.bbn.parliament.jena.graph.index.spatial.sql.SQLGeometryIndex;
import com.bbn.parliament.kb_graph.index.IndexException;
import com.bbn.parliament.kb_graph.index.Record;

/** @author Robert Battle */
public class PostgresIndex extends SQLGeometryIndex {
	static {
		try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			LOG.error("Could not load Postgres driver");
		}
	}

	// public static final String SPATIAL_TABLE = "spatial";
	public static final String GEOMETRY_COLUMN = "geom";
	public static final String NODE_COLUMN = "node";
	public static final String GEOMETRY_INDEX_SUFFIX = "_gi";
	public static final String NODE_INDEX_SUFFIX = "_ni";

	public static String readSQLFile(String file) {
		String packageName = PostgresIndex.class.getPackage().getName();
		String dir = packageName.replace('.', '/');
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		StringBuilder sb = new StringBuilder();
		try (InputStream functionStream = cl.getResourceAsStream(dir + "/" + file)) {
			for (int c = functionStream.read(); c != -1; c = functionStream.read()) {
				sb.append((char) c);
			}
		} catch (IOException ex) {
			LOG.error("IOException", ex);
		}
		return sb.toString();
	}

	private static String cleanGraphName(String graphName) {
		String toReturn = graphName;
		char[] invalid = { ':', '/', '\\', '.', ',', '#', '-', '+', '%', '(',
			')', '&', '!', '@', '$', '%', '^', '*', '=', '~', '`', '\"', '\'',
			';', '?', '{', '}', '[', ']', '|' };
		for (char inv : invalid) {
			toReturn = toReturn.replace(inv, '_');
		}
		if (toReturn.isEmpty()) {
			toReturn = "default";
		}
		return toReturn;
	}

	private QueryFactory sqlFactory;

	public PostgresIndex(Profile profile, Properties configuration,
		String cleanGraphName, String indexDir) {
		super(profile, configuration, cleanGraphName(cleanGraphName), indexDir);
	}

	public QueryFactory getSQLFactory() {
		return sqlFactory;
	}

	/** {@inheritDoc} */
	@Override
	protected void doInitialize() {

	}

	/** {@inheritDoc} */
	@Override
	public void indexOpenSQL() throws SpatialIndexException {
		createPersistanceTable();
		LOG.debug("Created persistance table");
		sqlFactory = StandardQueryFactory.createFactory(store, tableName);
		LOG.debug("Created factory");
	}

	/** {@inheritDoc} */
	@Override
	protected void indexDelete() throws IndexException {
		// need to be open to get a connection to the db
		if (isClosed()) {
			open();
		}
		try {
			execute(
				"DROP INDEX %1$s%2$s;DROP INDEX %1$s%3$s;DROP TABLE %1$s;",
				tableName, GEOMETRY_INDEX_SUFFIX, NODE_INDEX_SUFFIX);
		} catch (PersistentStoreException | SQLException ex) {
			throw new SpatialIndexException(this, ex);
		}
		close();
	}

	private void createPersistanceTable() throws SpatialIndexException {
		try (
			Connection c = store.getConnection();
			Statement stmt = c.createStatement();
		) {
			// check if spatial table exists
			String sql1 = "SELECT relname FROM pg_class WHERE relname = '%1$s'"
				.formatted(tableName);
			try (ResultSet rs = stmt.executeQuery(sql1)) {
				if (!rs.next()) {
					execute("CREATE TABLE %1$s ( id SERIAL PRIMARY KEY, node VARCHAR(256) )",
						tableName);
				}
			}

			// check if geometry column exists
			String sql2 = """
				SELECT attname FROM pg_attribute, pg_type WHERE
					typname = '%1$s'
					AND attrelid = typrelid
					AND attname NOT IN ('cmin', 'cmax', 'ctid', 'oid', 'tableoid', 'xmin', 'xmax');
				""".formatted(tableName);
			int count = 0;
			try (ResultSet rs = stmt.executeQuery(sql2)) {
				while (rs.next()) {
					++count;
				}
			}
			if (count == 2) {
				execute("SELECT AddGeometryColumn('%1$s','%2$s',%3$d,'GEOMETRY',2)",
					tableName, GEOMETRY_COLUMN, Constants.WGS84_SRID);
				execute("CREATE INDEX %1$s%2$s ON %1$s USING GIST ( %3$s)",
					tableName, GEOMETRY_INDEX_SUFFIX, GEOMETRY_COLUMN);
			}

			// check if node index exists
			String sql3 = """
				SELECT indexname from pg_indexes WHERE tablename = '%1$s'
					AND indexname NOT IN ('%1$s', '%1$s_pkey')
				""".formatted(tableName);
			boolean hasNodeIndex = false;
			try (ResultSet rs = stmt.executeQuery(sql3)) {
				while (rs.next()) {
					String index = rs.getString("indexname");
					if (index.equals(tableName + NODE_INDEX_SUFFIX)) {
						hasNodeIndex = true;
					}
				}
			}
			if (!hasNodeIndex) {
				execute("CREATE UNIQUE INDEX %1$s%2$s ON %1$s USING btree(node)",
					tableName, NODE_INDEX_SUFFIX);
			}
		} catch (PersistentStoreException | SQLException ex) {
			throw new SpatialIndexException(this, ex);
		}
	}

	/** {@inheritDoc} */
	@Override
	protected void indexClear() throws SpatialIndexException {
		try (Connection c = store.getConnection()) {
			execute("DELETE FROM %1$s", tableName);
		} catch (SQLException | PersistentStoreException ex) {
			throw new SpatialIndexException(this, ex);
		}
	}

	/** {@inheritDoc} */
	@SuppressWarnings("resource")
	@Override
	public Iterator<Record<Geometry>> iterator(Geometry geometry, Operation operation) {
		String query = Operation.Helper.isIntersection(operation)
			? Queries.SimpleFeatures.INTERSECTS
			: Queries.SimpleFeatures.DISJOINT;
		query = query.formatted(tableName, "?", GEOMETRY_COLUMN);

		Connection c = null;
		try {
			c = store.getConnection();
			PreparedStatement intersectionIterator = c.prepareStatement(query);
			intersectionIterator.setBytes(1, GeometryConverter.convertGeometry(geometry));
			LOG.debug("Query = {}", intersectionIterator);
			ResultSet rs = intersectionIterator.executeQuery();
			Iterator<Record<Geometry>> it = new ResultSetIterator(c, rs, NODE_COLUMN, GEOMETRY_COLUMN);
			return new Operation.OperationIterator(it, geometry, operation);
		} catch (SQLException | PersistentStoreException ex) {
			LOG.error(ex.getClass().getSimpleName() + ": " + tableName, ex);
			PersistentStore.close(c);
			return null;
		}
	}

	/** {@inheritDoc} */
	@Override
	protected void indexClose() throws IndexException {
	}

	@Override
	protected boolean indexAdd(Record<Geometry> r) throws SpatialIndexException {
		Node node = r.getKey();
		Geometry extent = r.getValue();
		extent.setSRID(Constants.WGS84_SRID);

		// add bounding box to geometry for faster bounding box
		// queries/filters
		String sql =
			"INSERT INTO %1$s (%2$s, %3$s) VALUES (ST_AddBBOX(ST_SetSRID(?, ?)), ?)"
			.formatted(tableName, GEOMETRY_COLUMN, NODE_COLUMN);
		try (
			Connection c = store.getConnection();
			PreparedStatement ps = c.prepareStatement(sql);
			) {
			ps.setBytes(1, GeometryConverter.convertGeometry(extent));
			ps.setInt(2, Constants.WGS84_SRID);
			ps.setString(3, GeometryConverter.getStringRepresentation(node));
			ps.execute();
		} catch (SQLException | PersistentStoreException ex) {
			throw new SpatialIndexException(this, tableName, ex);
		}
		return true;
	}

	@Override
	protected boolean indexUpdate(Record<Geometry> r, Record<Geometry> previous) throws SpatialIndexException {
		Node node = r.getKey();
		Geometry extent = r.getValue();
		extent.setSRID(Constants.WGS84_SRID);

		String sql = "UPDATE %1$s SET %2$s = ST_AddBBOX(ST_SetSRID(?, ?)) WHERE %3$s = ?"
			.formatted(tableName, GEOMETRY_COLUMN, NODE_COLUMN);
		try (
			Connection c = store.getConnection();
			PreparedStatement ps = c.prepareStatement(sql);
			) {
			ps.setBytes(1, GeometryConverter.convertGeometry(extent));
			ps.setInt(2, Constants.WGS84_SRID);
			ps.setString(3, GeometryConverter.getStringRepresentation(node));
			ps.execute();
		} catch (PersistentStoreException | SQLException ex) {
			throw new SpatialIndexException(this, tableName, ex);
		}
		return true;
	}

	@Override
	protected boolean indexRemove(Record<Geometry> r)
		throws SpatialIndexException {
		Node node = r.getKey();
		boolean removed = false;
		try (
			Connection c = store.getConnection();
			PreparedStatement ps = c.prepareStatement(Queries.DELETE.formatted(tableName));
		) {
			ps.setString(1, GeometryConverter.getStringRepresentation(node));
			ps.execute();
			removed = true;
		} catch (PersistentStoreException | SQLException ex) {
			throw new SpatialIndexException(this, tableName, ex);
		}
		return removed;
	}

	/** {@inheritDoc} */
	@SuppressWarnings("resource")
	@Override
	public Iterator<Record<Geometry>> query(Geometry value) throws SpatialIndexException {
		Connection c = null;
		try {
			c = store.getConnection();
			PreparedStatement geometryToNode = c.prepareStatement(String
				.format(Queries.GEOMETRY_TO_NODE_QUERY, tableName));
			geometryToNode.setBytes(1, GeometryConverter.convertGeometry(value));
			geometryToNode.setInt(2, Constants.WGS84_SRID);
			return new ResultSetIterator(c, geometryToNode.executeQuery(), NODE_COLUMN, GEOMETRY_COLUMN);
		} catch (SQLException | PersistentStoreException ex) {
			PersistentStore.close(c);
			String msg = "Could not lookup nodes for: %1$s in table '%2$s'"
				.formatted(GeometryConverter.convertGeometry(value), tableName);
			throw new SpatialIndexException(this, msg, ex);
		}
	}

	@Override
	protected long estimate(Geometry geometry, Operation operation) throws SpatialIndexException {
		// TODO Auto-generated method stub
		return 0;
	}

	private void execute(String statementFormat, Object... args)
		throws PersistentStoreException, SQLException {
		try (
			Connection c = store.getConnection();
			Statement stmt = c.createStatement();
		) {
			stmt.execute(statementFormat.formatted(args));
		}
	}
}
