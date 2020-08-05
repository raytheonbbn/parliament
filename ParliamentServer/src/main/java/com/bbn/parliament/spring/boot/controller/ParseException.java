package com.bbn.parliament.spring.boot.controller;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value=HttpStatus.BAD_REQUEST, reason="Error occured while parsing query/content")
public class ParseException extends IOException {
	private static final long serialVersionUID = 1L;

	public ParseException() {
	}

	public ParseException(String message) {
		super(message);
	}

	public ParseException(Throwable cause) {
		super(cause);
	}

	public ParseException(String message, Throwable cause) {
		super(message, cause);
	}
}
