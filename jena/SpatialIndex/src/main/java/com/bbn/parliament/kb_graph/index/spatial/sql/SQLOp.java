// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.kb_graph.index.spatial.sql;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import com.bbn.parliament.kb_graph.index.spatial.Operation;

/** @author Robert Battle */
public class SQLOp {
	private String iteratorQuery;
	private String estimateSelectivityQuery;
	private String estimateSelectivityInverseQuery;
	private String inverseQuery;
	private PersistentStore store;
	private boolean inverseOperation;
	private Operation operation;

	public SQLOp(PersistentStore store) {
		this.store = store;
	}

	public PersistentStore getStore() {
		return store;
	}

	public SQLOp(PersistentStore store, String iteratorQuery,
		String inverseQuery, String estimateSelectivity,
		String estimateSelectivityInverse, boolean inverseOperation,
		Operation operation, String tableName) {
		this.store = store;
		this.iteratorQuery = iteratorQuery;
		this.inverseQuery = inverseQuery;
		this.estimateSelectivityQuery = estimateSelectivity.formatted(tableName);
		this.estimateSelectivityInverseQuery = estimateSelectivityInverse.formatted(tableName);
		this.inverseOperation = inverseOperation;
		this.operation = operation;
	}

	public void setInverseOperation(boolean inverseOperation) {
		this.inverseOperation = inverseOperation;
	}

	public boolean isInverseOperation() {
		return inverseOperation;
	}

	public void setIteratorQuery(String iteratorQuery) {
		this.iteratorQuery = iteratorQuery;
	}

	public void setEstimateSelectivityQuery(String estimateSelectivityQuery) {
		this.estimateSelectivityQuery = estimateSelectivityQuery;
	}

	public void setEstimateSelectivityBoundQuery(
		String estimateSelectivityBoundQuery) {
		this.estimateSelectivityInverseQuery = estimateSelectivityBoundQuery;
	}

	public void runCommand(String command) throws PersistentStoreException, SQLException {
		try (
			Connection c = store.getConnection();
			Statement stmt = c.createStatement();
		) {
			stmt.execute(command);
		}
	}

	public String getIteratorQuery() {
		return iteratorQuery;
	}

	public String getInverseIteratorQuery() {
		return inverseQuery;
	}

	public String getEstimateSelectivity() {
		return estimateSelectivityQuery;
	}

	public String getEstimateSelectivityInverse(){
		return estimateSelectivityInverseQuery;
	}

	public Operation getOperation() {
		return operation;
	}
}
