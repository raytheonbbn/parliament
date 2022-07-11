package com.bbn.parliament.jena.exception;

public class BadRequestException extends Exception {
	private static final long serialVersionUID = 1L;

	public BadRequestException(String format, Object... args) {
		super(String.format(format, args));
	}

	public BadRequestException(Throwable cause, String format, Object... args) {
		super(String.format(format, args), cause);
	}

	public BadRequestException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
