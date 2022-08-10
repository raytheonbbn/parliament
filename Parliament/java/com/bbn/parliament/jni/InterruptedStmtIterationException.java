// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2010, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jni;

public class InterruptedStmtIterationException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public InterruptedStmtIterationException() {
	}

	public InterruptedStmtIterationException(String message) {
		super(message);
	}

	public InterruptedStmtIterationException(String fmt, Object... args) {
		super(fmt.formatted(args));
	}

	public InterruptedStmtIterationException(Throwable cause) {
		super(cause);
	}

	public InterruptedStmtIterationException(Throwable cause, String message) {
		super(message, cause);
	}

	public InterruptedStmtIterationException(Throwable cause, String fmt, Object... args) {
		super(fmt.formatted(args), cause);
	}
}
