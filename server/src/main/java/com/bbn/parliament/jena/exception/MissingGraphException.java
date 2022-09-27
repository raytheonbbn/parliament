package com.bbn.parliament.jena.exception;

public class MissingGraphException extends Exception {
	private static final long serialVersionUID = 1L;

	public MissingGraphException(String format, Object... args) {
		super(format.formatted(args));
	}

	public MissingGraphException(Throwable cause, String format, Object... args) {
		super(format.formatted(args), cause);
	}

	public MissingGraphException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
