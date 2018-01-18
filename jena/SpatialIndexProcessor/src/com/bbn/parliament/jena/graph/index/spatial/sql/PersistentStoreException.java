// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.graph.index.spatial.sql;

/** @author Robert Battle */
public class PersistentStoreException extends Exception {

	private static final long serialVersionUID = 1887660080864194263L;

	public PersistentStoreException(String tableName) {
		super(String.format("Table: %s", tableName));
	}

	public PersistentStoreException(String tableName, String message) {
		super(String.format("Table: %s, %s", tableName, message));
		// TODO Auto-generated constructor stub
	}

	public PersistentStoreException(String tableName, Throwable cause) {
		super(String.format("Table: %s", tableName), cause);
		// TODO Auto-generated constructor stub
	}

	public PersistentStoreException(String tableName, String message, Throwable cause) {
		super(String.format("Table: %s, %s", tableName, message), cause);
		// TODO Auto-generated constructor stub
	}
}
