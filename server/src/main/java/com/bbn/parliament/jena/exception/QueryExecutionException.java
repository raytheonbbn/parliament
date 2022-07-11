package com.bbn.parliament.jena.exception;

public class QueryExecutionException extends Exception {
	private static final long serialVersionUID = 1L;

	public QueryExecutionException(String format, Object... args) {
		super(String.format(format, args));
	}

	public QueryExecutionException(Throwable cause, String format, Object... args) {
		super(String.format(format, args), cause);
	}

	public QueryExecutionException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
