package com.bbn.parliament.spring.boot.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.hp.hpl.jena.shared.NotFoundException;


@ResponseStatus(value=HttpStatus.NOT_FOUND, reason="The specified graph was not found")
public class NoGraphException extends NotFoundException {
	private static final long serialVersionUID = 1L;

	public NoGraphException() {
		super("empty");
	}

	public NoGraphException(String message) {
		super(message);
	}
}
