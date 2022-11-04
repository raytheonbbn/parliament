// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.graph.index.spatial.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.jena.graph.Node;
import org.apache.jena.util.iterator.ClosableIterator;
import org.locationtech.jts.geom.Geometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jena.graph.index.Record;
import com.bbn.parliament.jena.graph.index.spatial.GeometryConverter;
import com.bbn.parliament.jena.graph.index.spatial.GeometryRecord;
import com.bbn.parliament.jena.graph.index.spatial.SpatialIndexException;

/** @author Robert Battle */
public class ResultSetIterator implements ClosableIterator<Record<Geometry>> {
	protected static Logger LOG = LoggerFactory.getLogger(ResultSetIterator.class);

	protected Connection conn;
	protected ResultSet resultSet;
	protected String nodeCol;
	protected String geometryCol;
	private boolean hasNextCalled = false;
	private boolean hasNext = true;
	private int pos;

	public ResultSetIterator(Connection c, ResultSet rs, String nodeColumn, String geometryColumn) {
		conn = c;
		resultSet = rs;
		nodeCol = nodeColumn;
		geometryCol = geometryColumn;
		pos = 0;
	}

	/** {@inheritDoc} */
	@Override
	public boolean hasNext() {
		try {
			if (hasNextCalled) {
				return hasNext;
			}

			if (hasNext) {
				hasNextCalled = true;
				hasNext = resultSet.next();
				if (!hasNext) {
					close();
				}
			}
			return hasNext;
		} catch (SQLException e) {
			LOG.error("Error while checking whether result set has next", e);
			close();
			throw new RuntimeException("Error while checking whether result set has next", e);
		}
	}

	/** {@inheritDoc} */
	@Override
	public Record<Geometry> next() {
		Record<Geometry> obj = null;
		if (!hasNext) {
			close();
			throw new RuntimeException("No more results in result set");
		}
		try {
			if (!hasNextCalled && hasNext) {
				hasNext = resultSet.next();
			}
			hasNextCalled = false;
			++pos;
			obj = GeometryRecord.create(getNode(), getGeometry());
		} catch (SQLException e) {
			LOG.error("Error while moving to next row", e);
			close();
			hasNext = false;
			return null;
		}
		return obj;
	}

	public int getPosition() {
		return pos;
	}

	public Node getNode() {
		try {
			return GeometryConverter.getNodeRepresentation(resultSet.getString(nodeCol));
		} catch (SQLException e) {
			LOG.error("Error while moving to next row", e);
			close();
			return null;
		}
	}

	public Geometry getGeometry() {
		try {
			return GeometryConverter.convertSQLGeometry(resultSet.getObject(geometryCol));
		} catch (SQLException e) {
			LOG.error("Error while moving to next row", e);
			close();
			return null;
		} catch (SpatialIndexException e) {
			LOG.error("Error while converting geometry", e);
			close();
			return null;
		}
	}

	/** {@inheritDoc} */
	@Override
	public void remove() {
		throw new UnsupportedOperationException("Can not remove a value");
	}

	/** {@inheritDoc} */
	@Override
	public void close() {
		try {
			resultSet.close();
		} catch (SQLException e) {
			LOG.error("Error while closing result set", e);
		}
		PersistentStore.close(conn);
	}
}
