// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.joseki.bridge.servlet;

import javax.servlet.http.HttpServletResponse;

/**
 * @author iemmons
 */
public class ServletErrorResponseException extends Exception {
	private static final long serialVersionUID = 8601712748837148746L;

	private int _statusCode;

	public ServletErrorResponseException(String message) {
		this(HttpServletResponse.SC_BAD_REQUEST, message);
	}

	public ServletErrorResponseException(String format, Object... args) {
		this(HttpServletResponse.SC_BAD_REQUEST, String.format(format, args));
	}

	public ServletErrorResponseException(Throwable cause, String message) {
		this(HttpServletResponse.SC_BAD_REQUEST, message, cause);
	}

	public ServletErrorResponseException(Throwable cause, String format, Object... args) {
		this(HttpServletResponse.SC_BAD_REQUEST, String.format(format, args), cause);
	}

	public ServletErrorResponseException(int statusCode, String message) {
		this(statusCode, null, message);
	}

	public ServletErrorResponseException(int statusCode, String format, Object... args) {
		this(statusCode, null, format, args);
	}

	public ServletErrorResponseException(int statusCode, Throwable cause, String message) {
		super(message, cause);
		_statusCode = statusCode;
	}

	public ServletErrorResponseException(int statusCode, Throwable cause, String format, Object... args) {
		super(String.format(format, args), cause);
		_statusCode = statusCode;
	}

	public int getStatusCode() {
		return _statusCode;
	}
}
