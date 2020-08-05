package com.bbn.parliament.spring.boot.controller;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.bbn.parliament.jena.bridge.servlet.ServletErrorResponseException;

@ControllerAdvice
public class ControllerAdvisor extends ResponseEntityExceptionHandler {

	private static final Logger LOG = LoggerFactory.getLogger(ControllerAdvisor.class);

	@ExceptionHandler(IOException.class)
	public ResponseEntity<Object> handleIOException(IOException e, WebRequest req) {
		Map<String, Object> map = new LinkedHashMap<>();
		LOG.info(e.toString());
		return new ResponseEntity<>(map, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(ServletErrorResponseException.class)
	public ResponseEntity<Object> handleServletErrorResponseException(ServletErrorResponseException e, WebRequest req) {
		Map<String, Object> map = new LinkedHashMap<>();
		LOG.info(e.toString());
		return new ResponseEntity<>(map, HttpStatus.INTERNAL_SERVER_ERROR);
	}



	//@ExceptionHandler(Exception.class)
	//public void handleGeneralException(Exception e, WebRequest request) {
	//	throw new InternalServerException();
	//}
}
