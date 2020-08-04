package com.bbn.parliament.spring.boot.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value=HttpStatus.NOT_IMPLEMENTED, reason="Parliament does not support this operation")
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
