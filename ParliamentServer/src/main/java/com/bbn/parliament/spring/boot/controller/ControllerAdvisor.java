package com.bbn.parliament.spring.boot.controller;

import java.io.IOException;
import java.time.LocalDateTime;
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

import com.bbn.parliament.jena.bridge.tracker.TrackableException;
import com.bbn.parliament.jena.exception.ArchiveException;
import com.bbn.parliament.jena.exception.DataFormatException;
import com.bbn.parliament.jena.exception.MissingGraphException;
import com.hp.hpl.jena.shared.JenaException;

@ControllerAdvice
public class ControllerAdvisor extends ResponseEntityExceptionHandler {

	private static final Logger LOG = LoggerFactory.getLogger(ControllerAdvisor.class);

	@ExceptionHandler(JenaException.class)
	public ResponseEntity<Object> handleJenaException(JenaException e, WebRequest req) {
		Map<String, Object> body = new LinkedHashMap<>();
		fillBody(body, HttpStatus.BAD_REQUEST, "Error while parsing the query/data", req);
		LOG.warn("", e);
		return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(ArchiveException.class)
	public ResponseEntity<Object> handleArchiveException(ArchiveException e, WebRequest req) {
		Map<String, Object> body = new LinkedHashMap<>();
		fillBody(body, HttpStatus.BAD_REQUEST, "Error while parsing archive", req);
		LOG.warn("", e);
		return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(DataFormatException.class)
	public ResponseEntity<Object> handleDataFormatException(DataFormatException e, WebRequest req) {
		Map<String, Object> body = new LinkedHashMap<>();
		fillBody(body, HttpStatus.BAD_REQUEST, "Unsupported data format", req);
		LOG.warn("", e);
		return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<Object> handleBadRequestException(BadRequestException e, WebRequest req) {
		Map<String, Object> body = new LinkedHashMap<>();
		fillBody(body, HttpStatus.BAD_REQUEST, "Parliament does not support specifying graph(s) outside the SPARQL statement", req);
		LOG.warn("", e);
		return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(UnsupportedEndpointException.class)
	public ResponseEntity<Object> handleUnsupportedEndpointException(UnsupportedEndpointException e, WebRequest req) {
		Map<String, Object> body = new LinkedHashMap<>();
		fillBody(body, HttpStatus.METHOD_NOT_ALLOWED, "Parliament does not support this endpoint", req);
		LOG.warn("", e);
		return new ResponseEntity<>(body, HttpStatus.METHOD_NOT_ALLOWED);
	}

	@ExceptionHandler(MissingGraphException.class)
	public ResponseEntity<Object> handleMissingGraphException(MissingGraphException e, WebRequest req) {
		Map<String, Object> body = new LinkedHashMap<>();
		fillBody(body, HttpStatus.NOT_FOUND, "Specified graph was not found", req);
		LOG.warn("", e);
		return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
	}

	@ExceptionHandler(RuntimeException.class)
	public ResponseEntity<Object> handleRuntimeException(RuntimeException e, WebRequest req) {
		Map<String, Object> body = new LinkedHashMap<>();
		fillBody(body, HttpStatus.INTERNAL_SERVER_ERROR, "Parliament encountered an error while processing the request", req);
		LOG.warn("", e);
		return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@ExceptionHandler(TrackableException.class)
	public ResponseEntity<Object> handleTrackableException(TrackableException e, WebRequest req) {
		Map<String, Object> body = new LinkedHashMap<>();
		fillBody(body, HttpStatus.INTERNAL_SERVER_ERROR, "Parliament encountered an error with Trackable", req);
		LOG.warn("", e);
		return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@ExceptionHandler(IOException.class)
	public ResponseEntity<Object> handleIOException(IOException e, WebRequest req) {
		Map<String, Object> body = new LinkedHashMap<>();
		fillBody(body, HttpStatus.INTERNAL_SERVER_ERROR, "Parliament encountered an error during IO operations", req);
		LOG.warn("", e);
		return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	public void fillBody(Map<String, Object> map, HttpStatus status, String message, WebRequest req) {
		map.put("timestamp", LocalDateTime.now());
		map.put("status", status.value());
		map.put("error", status.getReasonPhrase());
		map.put("message", message);
		map.put("path", req.getDescription(false).substring(4));
	}
}
