package com.bbn.parliament.jena.graph.index.spatial.sql.postgres;

import static org.junit.jupiter.api.Assertions.fail;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.locationtech.jts.geom.Geometry;

import com.bbn.parliament.jena.graph.index.IndexFactory;
import com.bbn.parliament.jena.graph.index.spatial.SpatialIndex;
import com.bbn.parliament.jena.graph.index.spatial.SpatialIndexFactory;
import com.bbn.parliament.jena.graph.index.spatial.SpatialIndexTestMethods;
import com.bbn.parliament.jena.graph.index.spatial.sql.PersistentStore;
import com.bbn.parliament.jena.graph.index.spatial.sql.PersistentStoreException;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;

public class PostgresIndexTestMethods extends SpatialIndexTestMethods {
	@Override
	protected IndexFactory<SpatialIndex, Geometry> getIndexFactory() {
		SpatialIndexFactory f = new SpatialIndexFactory();
		f.configure(PostgresPropertyFactory.create());
		return f;
	}

	@Override
	protected boolean checkDeleted(SpatialIndex index, Graph graph, Node graphName) {
		return super.checkDeleted(index, graph, graphName) & checkSQLDeleted(index);
	}

	private static boolean checkSQLDeleted(SpatialIndex index) {
		String tableName = ((PostgresIndex) index).getTableName();
		boolean contains = false;
		try (Connection c = PersistentStore.getInstance().getConnection()) {
			// check if spatial table exists
			String sql1 = String.format("SELECT relname FROM pg_class WHERE relname = '%1$s'", tableName);
			try (
				Statement stmt = c.createStatement();
				ResultSet rs = stmt.executeQuery(sql1);
			) {
				contains |= rs.next();
			}

			// check if node index exists
			String sql2 = String.format(
				"SELECT indexname from pg_indexes where tablename = '%1$s' AND indexname NOT IN ('%1$s', '%1$s_pkey')",
				tableName);
			try (
				Statement stmt = c.createStatement();
				ResultSet rs = stmt.executeQuery(sql2);
			) {
				contains |= rs.next();
			}
		} catch (PersistentStoreException | SQLException ex) {
			fail(ex.getMessage());
		}
		return !contains;
	}
}
