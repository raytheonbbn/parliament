// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2015, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.client.jena;

public class ParliamentClientException extends Exception {
	private static final long serialVersionUID = 1L;

	public ParliamentClientException(String message) {
		super(message);
	}

	public ParliamentClientException(String format, Object... args) {
		super(format.formatted(args));
	}

	public ParliamentClientException(Throwable cause, String message) {
		super(message, cause);
	}

	public ParliamentClientException(Throwable cause, String format, Object... args) {
		super(format.formatted(args), cause);
	}
}
