// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.graph.index.spatial;

import com.bbn.parliament.kb_graph.index.IndexException;

/**
 * An exception that can be thrown in the {@link SpatialIndex}.
 *
 * @author rbattle
 */
public class SpatialIndexException extends IndexException {
	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a new exception with the specified index and detail message.
	 *
	 * @param index the index that caused the error.
	 * @param message the detail message.
	 */
	public SpatialIndexException(SpatialIndex index, String message) {
		super(index, message);
	}

	/**
	 * Constructs a new exception with the specified index and cause cause a
	 * detail message of (cause==null ? null : cause.toString()) (which typically
	 * contains the class and detail message of cause).
	 *
	 * @param index the index that caused the error.
	 * @param cause the cause.
	 */
	public SpatialIndexException(SpatialIndex index, Throwable cause) {
		super(index, cause);
	}

	/**
	 * Constructs a new exception with the specified index, detail message and
	 * cause.
	 *
	 * @param index the index that caused the error.
	 * @param message the detail message.
	 * @param cause the cause.
	 */
	public SpatialIndexException(SpatialIndex index, String message,
		Throwable cause) {
		super(index, message, cause);
	}
}
