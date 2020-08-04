package com.bbn.parliament.spring.boot.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value=HttpStatus.INTERNAL_SERVER_ERROR, reason="Error occured while processing")
public class InternalServerException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public InternalServerException() {
	}

	public InternalServerException(String message) {
		super(message);
	}

	public InternalServerException(Throwable cause) {
		super(cause);
	}

	public InternalServerException(String message, Throwable cause) {
		super(message, cause);
	}

	public InternalServerException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
