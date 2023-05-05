// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.kb_graph.index.spatial.sql;

/** @author Robert Battle */
public class PersistentStoreException extends Exception {

	private static final long serialVersionUID = 1887660080864194263L;

	public PersistentStoreException(String tableName) {
		super("Table: %s".formatted(tableName));
	}

	public PersistentStoreException(String tableName, String message) {
		super("Table: %s, %s".formatted(tableName, message));
	}

	public PersistentStoreException(String tableName, Throwable cause) {
		super("Table: %s".formatted(tableName), cause);
	}

	public PersistentStoreException(String tableName, String message, Throwable cause) {
		super("Table: %s, %s".formatted(tableName, message), cause);
	}
}
