package com.bbn.parliament.jena.exception;

public class UnsupportedEndpointException extends UnsupportedOperationException {
	private static final long serialVersionUID = 1L;

	public UnsupportedEndpointException(String format, Object... args) {
		super(String.format(format, args));
	}

	public UnsupportedEndpointException(Throwable cause, String format, Object... args) {
		super(String.format(format, args), cause);
	}
}
