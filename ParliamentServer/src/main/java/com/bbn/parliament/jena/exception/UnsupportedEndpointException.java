package com.bbn.parliament.spring.boot.controller;

public class UnsupportedEndpointException extends UnsupportedOperationException {
	private static final long serialVersionUID = 1L;

	public UnsupportedEndpointException() {
	}

	public UnsupportedEndpointException(String message) {
		super(message);
	}

	public UnsupportedEndpointException(Throwable cause) {
		super(cause);
	}

	public UnsupportedEndpointException(String message, Throwable cause) {
		super(message, cause);
	}
}
