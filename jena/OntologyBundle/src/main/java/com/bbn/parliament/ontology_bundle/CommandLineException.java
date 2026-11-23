// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2023, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.ontology_bundle;

class CommandLineException extends Exception {
	private static final long serialVersionUID = 1L;

	public CommandLineException() {
	}

	public CommandLineException(String format, Object... args) {
		super(String.format(format, args));
	}

	public CommandLineException(Throwable cause, String format, Object... args) {
		super(String.format(format, args), cause);
	}
}
