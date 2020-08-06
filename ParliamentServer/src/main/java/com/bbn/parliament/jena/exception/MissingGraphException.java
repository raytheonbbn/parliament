package com.bbn.parliament.jena.exception;

public class MissingGraphException extends Exception {
	private static final long serialVersionUID = 1L;

	public MissingGraphException() {
	}

	public MissingGraphException(String message) {
		super(message);
	}

	public MissingGraphException(Throwable cause) {
		super(cause);
	}

	public MissingGraphException(String message, Throwable cause) {
		super(message, cause);
	}

	public MissingGraphException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
