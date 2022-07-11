// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2010, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.bridge;

public class ParliamentBridgeException extends Exception {
	private static final long serialVersionUID = 1L;

	public ParliamentBridgeException(String message, Throwable cause) {
		super(message, cause);
	}

	public ParliamentBridgeException(String message) {
		super(message);
	}

	public ParliamentBridgeException(Throwable cause) {
		super(cause);
	}
}
