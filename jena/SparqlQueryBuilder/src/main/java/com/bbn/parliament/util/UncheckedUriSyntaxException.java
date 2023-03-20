// Copyright (c) 2020 Raytheon BBN Technologies Corp.

package com.bbn.ix.util;

import java.net.URISyntaxException;

/**
 * Unchecked wrapped for a URISyntaxException.
 *
 * @author iemmons
 */
public class UncheckedUriSyntaxException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	/**
	 * Wraps a URISyntaxException
	 *
	 * @param cause The URISyntaxException to wrap
	 */
	public UncheckedUriSyntaxException(URISyntaxException cause) {
		super(cause);
	}

	/**
	 * Wraps a URISyntaxException
	 *
	 * @param message An additional message for the wrapper
	 * @param cause The URISyntaxException to wrap
	 */
	public UncheckedUriSyntaxException(String message, URISyntaxException cause) {
		super(message, cause);
	}

	/**
	 * Wraps a URISyntaxException
	 *
	 * @param message An additional message for the wrapper
	 * @param cause The URISyntaxException to wrap
	 * @param enableSuppression True to enable suppression
	 * @param writableStackTrace True for a writable stack trace
	 */
	public UncheckedUriSyntaxException(String message, URISyntaxException cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
