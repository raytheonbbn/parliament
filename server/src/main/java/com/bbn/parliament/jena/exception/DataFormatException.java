package com.bbn.parliament.jena.exception;

public class DataFormatException extends Exception {
	private static final long serialVersionUID = 1L;

	public DataFormatException(String format, Object... args) {
		super(format.formatted(args));
	}

	public DataFormatException(Throwable cause, String format, Object... args) {
		super(format.formatted(args), cause);
	}

	public DataFormatException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
