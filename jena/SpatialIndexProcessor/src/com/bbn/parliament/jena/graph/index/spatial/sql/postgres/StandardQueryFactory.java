package com.bbn.parliament.jena.graph.index.spatial.sql.postgres;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jena.graph.index.spatial.Operation;
import com.bbn.parliament.jena.graph.index.spatial.sql.PersistentStore;
import com.bbn.parliament.jena.graph.index.spatial.sql.PersistentStoreException;
import com.bbn.parliament.jena.graph.index.spatial.sql.SQLOp;

public class StandardQueryFactory extends QueryFactory {
	protected static Logger LOG = LoggerFactory.getLogger(StandardQueryFactory.class);

	public static StandardQueryFactory createFactory(PersistentStore store, String tableName) {
		return new StandardQueryFactory(store, tableName);
	}

	protected PersistentStore _store;
	protected Map<Operation, SQLOp> _operators;

	private StandardQueryFactory(PersistentStore store, String tableName) {
		_store = store;
		_operators = new HashMap<>();
		SQLOp part = new SQLOp(_store,
			createQuery(Queries.PART, tableName, false),
			createQuery(Queries.PART, tableName, true),
			String.format(Queries.PART_EST, tableName),
			String.format(Queries.INV_PART_EST, tableName),
			false,
			Operation.RCC_EXT.PART,
			tableName);
		_operators.put(Operation.RCC_EXT.PART, part);
		_operators.put(Operation.RCC8.DC, new SQLOp(_store,
			createQuery(Queries.DISCONNECTED, tableName, false),
			createQuery(Queries.DISCONNECTED, tableName, true),
			String.format(Queries.DISCONNECTED_EST, tableName),
			String.format(Queries.DISCONNECTED_EST, tableName),
			false,
			Operation.RCC8.DC,
			tableName));
		_operators.put(Operation.RCC_EXT.CONNECTED, new SQLOp(_store,
			createQuery(Queries.CONNECTED, tableName, false),
			createQuery(Queries.CONNECTED, tableName, true),
			String.format(Queries.CONNECTED_EST, tableName),
			String.format(Queries.CONNECTED_EST, tableName),
			false,
			Operation.RCC_EXT.CONNECTED,
			tableName));
		_operators.put(Operation.RCC8.EC, new SQLOp(_store,
			createQuery(Queries.EXTERNALLY_CONNECTED, tableName, false),
			createQuery(Queries.EXTERNALLY_CONNECTED, tableName, true),
			String.format(Queries.EXTERNALLY_CONNECTED_EST, tableName),
			String.format(Queries.EXTERNALLY_CONNECTED_EST, tableName),
			false,
			Operation.RCC8.EC,
			tableName));
		_operators.put(Operation.RCC8.EQ, new SQLOp(_store,
			createQuery(Queries.IDENTICAL, tableName, false),
			createQuery(Queries.IDENTICAL, tableName, true),
			String.format(Queries.IDENTICAL_EST, tableName),
			String.format(Queries.IDENTICAL_EST, tableName),
			false,
			Operation.RCC8.EQ,
			tableName));
		_operators.put(Operation.RCC8.NTPPI, new SQLOp(_store,
			createQuery(Queries.NON_TANGENTIAL_PROPER_PART, tableName, true),
			createQuery(Queries.NON_TANGENTIAL_PROPER_PART, tableName, false),
			String.format(Queries.INV_NON_TANGENTIAL_PROPER_PART_EST, tableName),
			String.format(Queries.NON_TANGENTIAL_PROPER_PART_EST, tableName),
			true,
			Operation.RCC8.NTPPI,
			tableName));
		SQLOp invPart = new SQLOp(_store,
			createQuery(Queries.PART, tableName, true),
			createQuery(Queries.PART, tableName, false),
			String.format(Queries.INV_PART_EST, tableName),
			String.format(Queries.PART_EST, tableName),
			true,
			Operation.RCC_EXT.INV_PART,
			tableName);
		_operators.put(Operation.RCC_EXT.INV_PART, invPart);
		_operators.put(Operation.RCC_EXT.INV_PROPER_PART, new SQLOp(_store,
			createQuery(Queries.PROPER_PART, tableName, true),
			createQuery(Queries.PROPER_PART, tableName, false),
			String.format(Queries.INV_PROPER_PART_EST, tableName),
			String.format(Queries.PROPER_PART_EST, tableName),
			true,
			Operation.RCC_EXT.INV_PROPER_PART,
			tableName));
		_operators.put(Operation.RCC8.TPPI, new SQLOp(_store,
			createQuery(Queries.TANGENTIAL_PROPER_PART, tableName, true),
			createQuery(Queries.TANGENTIAL_PROPER_PART, tableName, false),
			String.format(Queries.INV_TANGENTIAL_PROPER_PART_EST, tableName),
			String.format(Queries.TANGENTIAL_PROPER_PART_EST, tableName),
			true,
			Operation.RCC8.TPPI,
			tableName));
		_operators.put(Operation.RCC8.PO, new SQLOp(_store,
			createQuery(Queries.PARTIALLY_OVERLAPS, tableName, false),
			createQuery(Queries.PARTIALLY_OVERLAPS, tableName, true),
			String.format(Queries.PARTIALLY_OVERLAPS_EST, tableName),
			String.format(Queries.PARTIALLY_OVERLAPS_EST, tableName),
			false,
			Operation.RCC8.PO,
			tableName));

		try {
			SQLOp ntpp = new SQLOp(_store,
				createQuery(Queries.NON_TANGENTIAL_PROPER_PART, tableName, false),
				createQuery(Queries.NON_TANGENTIAL_PROPER_PART, tableName, true),
				String.format(Queries.NON_TANGENTIAL_PROPER_PART_EST, tableName),
				String.format(Queries.INV_NON_TANGENTIAL_PROPER_PART_EST, tableName),
				false,
				Operation.RCC8.NTPP,
				tableName);
			ntpp.runCommand(PostgresIndex.readSQLFile("NonTangentialProperPart.sql"));
			_operators.put(Operation.RCC8.NTPP, ntpp);

			SQLOp pp = new SQLOp(_store,
				createQuery(Queries.PROPER_PART, tableName, false),
				createQuery(Queries.PROPER_PART, tableName, true),
				String.format(Queries.PROPER_PART_EST, tableName),
				String.format(Queries.INV_PROPER_PART_EST, tableName),
				false,
				Operation.RCC_EXT.PROPER_PART,
				tableName);
			pp.runCommand(PostgresIndex.readSQLFile("ProperPart.sql"));
			_operators.put(Operation.RCC_EXT.PROPER_PART, pp);

			SQLOp tpp = new SQLOp(_store,
				createQuery(Queries.TANGENTIAL_PROPER_PART, tableName, false),
				createQuery(Queries.TANGENTIAL_PROPER_PART, tableName, true),
				String.format(Queries.TANGENTIAL_PROPER_PART_EST, tableName),
				String.format(Queries.INV_TANGENTIAL_PROPER_PART_EST, tableName),
				false,
				Operation.RCC8.TPP,
				tableName);
			tpp.runCommand(PostgresIndex.readSQLFile("TangentialProperPart.sql"));
			_operators.put(Operation.RCC8.TPP, tpp);
		} catch (SQLException | PersistentStoreException ex) {
			LOG.error("Error while reading SQL file", ex);
		}

		// OGC operators

		_operators.put(Operation.SimpleFeatures.EQUALS, new SQLOp(_store,
			createQuery(Queries.EQUALS, tableName, false),
			createQuery(Queries.EQUALS, tableName, true),
			String.format(Queries.EQUALS_EST, tableName),
			String.format(Queries.EQUALS_EST, tableName),
			false,
			Operation.SimpleFeatures.EQUALS,
			tableName));

		_operators.put(Operation.SimpleFeatures.DISJOINT, new SQLOp(_store,
			createQuery(Queries.DISCONNECTED, tableName, false),
			createQuery(Queries.DISCONNECTED, tableName, true),
			String.format(Queries.DISCONNECTED_EST, tableName),
			String.format(Queries.DISCONNECTED_EST, tableName),
			false,
			Operation.SimpleFeatures.DISJOINT,
			tableName));

		_operators.put(Operation.SimpleFeatures.INTERSECTS, new SQLOp(_store,
			createQuery(Queries.INTERSECTS, tableName, false),
			createQuery(Queries.INTERSECTS, tableName, true),
			String.format(Queries.INTERSECTS_EST, tableName),
			String.format(Queries.INTERSECTS_EST, tableName),
			false,
			Operation.SimpleFeatures.INTERSECTS,
			tableName));

		_operators.put(Operation.SimpleFeatures.TOUCHES, new SQLOp(_store,
			createQuery(Queries.TOUCHES, tableName, false),
			createQuery(Queries.TOUCHES, tableName, true),
			String.format(Queries.TOUCHES_EST, tableName),
			String.format(Queries.TOUCHES_EST, tableName),
			false,
			Operation.SimpleFeatures.TOUCHES,
			tableName));

		_operators.put(Operation.SimpleFeatures.CROSSES, new SQLOp(_store,
			createQuery(Queries.CROSSES, tableName, false),
			createQuery(Queries.CROSSES, tableName, true),
			String.format(Queries.CROSSES_EST, tableName),
			String.format(Queries.CROSSES_EST, tableName),
			false,
			Operation.SimpleFeatures.CROSSES,
			tableName));

		_operators.put(Operation.SimpleFeatures.WITHIN, new SQLOp(_store,
			createQuery(Queries.WITHIN, tableName, false),
			createQuery(Queries.WITHIN, tableName, true),
			String.format(Queries.WITHIN_EST, tableName),
			String.format(Queries.WITHIN_EST, tableName),
			false,
			Operation.SimpleFeatures.WITHIN,
			tableName));

		_operators.put(Operation.SimpleFeatures.OVERLAPS, new SQLOp(_store,
			createQuery(Queries.OVERLAPS, tableName, false),
			createQuery(Queries.OVERLAPS, tableName, true),
			String.format(Queries.OVERLAPS_EST, tableName),
			String.format(Queries.OVERLAPS_EST, tableName),
			false,
			Operation.SimpleFeatures.OVERLAPS,
			tableName));

		_operators.put(Operation.Egenhofer.COVERS, new SQLOp(_store,
			createQuery(Queries.COVERS, tableName, false),
			createQuery(Queries.COVERS, tableName, true),
			String.format(Queries.COVERS_EST, tableName),
			String.format(Queries.COVERS_EST, tableName),
			false,
			Operation.Egenhofer.COVERS,
			tableName));

		_operators.put(Operation.Egenhofer.COVEREDBY, new SQLOp(_store,
			createQuery(Queries.COVERED_BY, tableName, false),
			createQuery(Queries.COVERED_BY, tableName, true),
			String.format(Queries.COVERED_BY_EST, tableName),
			String.format(Queries.COVERED_BY_EST, tableName),
			false,
			Operation.Egenhofer.COVEREDBY,
			tableName));


//		// SWC copies of OGC operators
//
//		_operators.put(Operation.SR_EQUALS, new SQLOp(_store,
//			createQuery(Queries.EQUALS, tableName, false),
//			createQuery(Queries.EQUALS, tableName, true),
//			String.format(Queries.EQUALS_EST, tableName),
//			String.format(Queries.EQUALS_EST, tableName),
//			false,
//			Operation.SR_EQUALS,
//			tableName));
//
//		_operators.put(Operation.SR_DISJOINT, new SQLOp(_store,
//			createQuery(Queries.DISCONNECTED, tableName, false),
//			createQuery(Queries.DISCONNECTED, tableName, true),
//			String.format(Queries.DISCONNECTED_EST, tableName),
//			String.format(Queries.DISCONNECTED_EST, tableName),
//			false,
//			Operation.SR_DISJOINT,
//			tableName));
//
//		_operators.put(Operation.SR_INTERSECTS, new SQLOp(_store,
//			createQuery(Queries.INTERSECTS, tableName, false),
//			createQuery(Queries.INTERSECTS, tableName, true),
//			String.format(Queries.INTERSECTS_EST, tableName),
//			String.format(Queries.INTERSECTS_EST, tableName),
//			false,
//			Operation.SR_INTERSECTS,
//			tableName));
//
//		_operators.put(Operation.SR_TOUCHES, new SQLOp(_store,
//			createQuery(Queries.TOUCHES, tableName, false),
//			createQuery(Queries.TOUCHES, tableName, true),
//			String.format(Queries.TOUCHES_EST, tableName),
//			String.format(Queries.TOUCHES_EST, tableName),
//			false,
//			Operation.SR_TOUCHES,
//			tableName));
//
//		_operators.put(Operation.SR_CROSSES, new SQLOp(_store,
//			createQuery(Queries.CROSSES, tableName, false),
//			createQuery(Queries.CROSSES, tableName, true),
//			String.format(Queries.CROSSES_EST, tableName),
//			String.format(Queries.CROSSES_EST, tableName),
//			false,
//			Operation.SR_CROSSES,
//			tableName));
//
//		_operators.put(Operation.SR_WITHIN, new SQLOp(_store,
//			createQuery(Queries.WITHIN, tableName, false),
//			createQuery(Queries.WITHIN, tableName, true),
//			String.format(Queries.WITHIN_EST, tableName),
//			String.format(Queries.WITHIN_EST, tableName),
//			false,
//			Operation.SR_WITHIN,
//			tableName));
//
//		_operators.put(Operation.SR_OVERLAPS, new SQLOp(_store,
//			createQuery(Queries.OVERLAPS, tableName, false),
//			createQuery(Queries.OVERLAPS, tableName, true),
//			String.format(Queries.OVERLAPS_EST, tableName),
//			String.format(Queries.OVERLAPS_EST, tableName),
//			false,
//			Operation.SR_OVERLAPS,
//			tableName));
//
//		_operators.put(Operation.SR_COVERS, new SQLOp(_store,
//			createQuery(Queries.COVERS, tableName, false),
//			createQuery(Queries.COVERS, tableName, true),
//			String.format(Queries.COVERS_EST, tableName),
//			String.format(Queries.COVERS_EST, tableName),
//			false,
//			Operation.SR_COVERS,
//			tableName));
//
//		_operators.put(Operation.SR_COVERED_BY, new SQLOp(_store,
//			createQuery(Queries.COVERED_BY, tableName, false),
//			createQuery(Queries.COVERED_BY, tableName, true),
//			String.format(Queries.COVERED_BY_EST, tableName),
//			String.format(Queries.COVERED_BY_EST, tableName),
//			false,
//			Operation.SR_COVERED_BY,
//			tableName));
	}

	private static String createQuery(String query, String tableName, boolean inverse) {
		String q = query;
		String one;
		String two;

		if (inverse) {
			one = "ST_SetSRID(?, ?)";
			two = PostgresIndex.GEOMETRY_COLUMN;
		} else {
			one = PostgresIndex.GEOMETRY_COLUMN;
			two = "ST_SetSRID(?, ?)";
		}
		q = String.format(query, tableName, one, two);
		return q;
	}

	@Override
	public SQLOp getOperator(Operation operation) {
		return _operators.get(operation);
	}
}
