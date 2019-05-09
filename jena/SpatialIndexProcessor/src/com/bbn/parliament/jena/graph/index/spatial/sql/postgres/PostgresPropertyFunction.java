package com.bbn.parliament.jena.graph.index.spatial.sql.postgres;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jena.graph.index.Record;
import com.bbn.parliament.jena.graph.index.spatial.Constants;
import com.bbn.parliament.jena.graph.index.spatial.GeometryConverter;
import com.bbn.parliament.jena.graph.index.spatial.Operation;
import com.bbn.parliament.jena.graph.index.spatial.SpatialIndex;
import com.bbn.parliament.jena.graph.index.spatial.SpatialPropertyFunction;
import com.bbn.parliament.jena.graph.index.spatial.sql.PersistentStore;
import com.bbn.parliament.jena.graph.index.spatial.sql.PersistentStoreException;
import com.bbn.parliament.jena.graph.index.spatial.sql.ResultSetIterator;
import com.bbn.parliament.jena.graph.index.spatial.sql.SQLOp;
import com.bbn.parliament.jena.query.index.operand.OperandFactory;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.pfunction.PropFuncArg;
import com.hp.hpl.jena.sparql.util.IterLib;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class PostgresPropertyFunction extends SpatialPropertyFunction {
	private final static Logger LOG = LoggerFactory.getLogger(PostgresPropertyFunction.class);

	protected SQLOp operand;

	public PostgresPropertyFunction(String uri, Operation op, Class<? extends OperandFactory<Geometry>> opFactoryClass) {
		super(uri, op, opFactoryClass);
	}

	@Override
	public PostgresIndex getIndex() {
		return (PostgresIndex)super.getIndex();
	}

	/** {@inheritDoc} */
	@Override
	public void build(PropFuncArg argSubject, Node predicate,
		PropFuncArg argObject, ExecutionContext context) {
		super.build(argSubject, predicate, argObject, context);
		this.operand = getIndex().getSQLFactory().getOperator(opToExecute);
	}

	@SuppressWarnings("resource")
	@Override
	protected QueryIterator bindVar(Geometry boundExtent, Node unboundVariable,
		SpatialIndex idx, boolean isFirstVarUnbound, ExecutionContext context) {

		String sql = isFirstVarUnbound
			? operand.getIteratorQuery()
			: operand.getInverseIteratorQuery();

		Connection c = null;

		try {
			c = PersistentStore.getInstance().getConnection();
			PreparedStatement operation = c.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE,
				ResultSet.CONCUR_READ_ONLY);
			byte[] bound = GeometryConverter.convertGeometry(boundExtent);
			for (int i = 1; i <= operation.getParameterMetaData().getParameterCount(); i += 2) {
				operation.setBytes(i, bound);
				operation.setInt(i + 1, Constants.WGS84_SRID);
			}

			ResultSet rs = operation.executeQuery();
			Iterator<Record<Geometry>> extents = new ResultSetIterator(c, rs, PostgresIndex.NODE_COLUMN,
				PostgresIndex.GEOMETRY_COLUMN);
			return new SingleGeometryIterator(unboundVariable, extents,
				idx.getQueryCache(), context);
		} catch (SQLException | PersistentStoreException ex) {
			LOG.error(ex.getClass().getSimpleName(), ex);
			PersistentStore.close(c);
			return IterLib.noResults(context);
		}
	}

	@Override
	protected long estimateSelectivity(Geometry extent, boolean isSubject) {
		long count = Long.MAX_VALUE;
		String text = (extent instanceof Point)
			? extent.toText()
			: extent.getEnvelope().toText();
		try (
			Connection c = operand.getStore().getConnection();
			PreparedStatement ps = c.prepareStatement(isSubject
				? operand.getEstimateSelectivityInverse()
				: operand.getEstimateSelectivity());
		) {
			for (int i = 1; i <= ps.getParameterMetaData().getParameterCount(); i += 2) {
				ps.setString(i, text);
				ps.setInt(i + 1, Constants.WGS84_SRID);
			}

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					count = rs.getLong(1);
				}
			}
		} catch (SQLException | PersistentStoreException ex) {
			LOG.error("Exception while estimating selectivity:", ex);
		}
		return count;
	}
}
