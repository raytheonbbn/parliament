// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.core.jni;

public class NativeCodeException extends Exception {
	private static final long serialVersionUID = 1L;

	public NativeCodeException() {
		super();
	}

	public NativeCodeException(String message) {
		super(message);
	}

	public NativeCodeException(Throwable cause) {
		super(cause);
	}

	public NativeCodeException(String message, Throwable cause) {
		super(message, cause);
	}
}
