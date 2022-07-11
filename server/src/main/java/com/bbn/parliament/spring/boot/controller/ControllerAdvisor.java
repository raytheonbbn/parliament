package com.bbn.parliament.spring.boot.controller;

import java.io.IOException;
import java.io.UncheckedIOException;
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
import com.bbn.parliament.jena.exception.BadRequestException;
import com.bbn.parliament.jena.exception.DataFormatException;
import com.bbn.parliament.jena.exception.MissingGraphException;
import com.bbn.parliament.jena.exception.NoAcceptableException;
import com.bbn.parliament.jena.exception.QueryExecutionException;
import com.bbn.parliament.jena.exception.UnsupportedEndpointException;
import com.hp.hpl.jena.query.QueryParseException;
import com.hp.hpl.jena.shared.JenaException;

//TODO: HttpStatus.REQUEST_TIMEOUT

@ControllerAdvice
public class ControllerAdvisor extends ResponseEntityExceptionHandler {
	private static final Logger LOG = LoggerFactory.getLogger(ControllerAdvisor.class);

	@SuppressWarnings("static-method")
	@ExceptionHandler(JenaException.class)
	public ResponseEntity<Object> handle(JenaException ex, WebRequest req) {
		return buildResponse(ex, req, HttpStatus.BAD_REQUEST,
			"Parliament encountered an error while during query processing");
	}

	@SuppressWarnings("static-method")
	@ExceptionHandler(DataFormatException.class)
	public ResponseEntity<Object> handle(DataFormatException ex, WebRequest req) {
		return buildResponse(ex, req, HttpStatus.BAD_REQUEST,
			"Unsupported data format");
	}

	@SuppressWarnings("static-method")
	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<Object> handle(BadRequestException ex, WebRequest req) {
		return buildResponse(ex, req, HttpStatus.BAD_REQUEST,
			"Parliament does not support specifying graph(s) outside the SPARQL statement");
	}

	@SuppressWarnings("static-method")
	@ExceptionHandler(UnsupportedEndpointException.class)
	public ResponseEntity<Object> handle(UnsupportedEndpointException ex, WebRequest req) {
		return buildResponse(ex, req, HttpStatus.METHOD_NOT_ALLOWED,
			"Parliament does not support this endpoint");
	}

	@SuppressWarnings("static-method")
	@ExceptionHandler(MissingGraphException.class)
	public ResponseEntity<Object> handle(MissingGraphException ex, WebRequest req) {
		return buildResponse(ex, req, HttpStatus.NOT_FOUND,
			"Specified graph was not found");
	}

	@SuppressWarnings("static-method")
	@ExceptionHandler(QueryExecutionException.class)
	public ResponseEntity<Object> handle(QueryExecutionException ex, WebRequest req) {
		return buildResponse(ex, req, HttpStatus.INSUFFICIENT_STORAGE,
			"Specified graph was not found");
	}

	@SuppressWarnings("static-method")
	@ExceptionHandler(RuntimeException.class)
	public ResponseEntity<Object> handle(RuntimeException ex, WebRequest req) {
		return buildResponse(ex, req, HttpStatus.INTERNAL_SERVER_ERROR,
			"Parliament encountered an error while processing the request");
	}

	@SuppressWarnings("static-method")
	@ExceptionHandler(TrackableException.class)
	public ResponseEntity<Object> handle(TrackableException ex, WebRequest req) {
		return buildResponse(ex, req, HttpStatus.INTERNAL_SERVER_ERROR,
			"Parliament encountered an error with Trackable");
	}

	@SuppressWarnings("static-method")
	@ExceptionHandler(IOException.class)
	public ResponseEntity<Object> handle(IOException ex, WebRequest req) {
		return buildResponse(ex, req, HttpStatus.INTERNAL_SERVER_ERROR,
			"Parliament encountered an error during IO operations");
	}

	@SuppressWarnings("static-method")
	@ExceptionHandler(UncheckedIOException.class)
	public ResponseEntity<Object> handle(UncheckedIOException ex, WebRequest req) {
		return buildResponse(ex, req, HttpStatus.INTERNAL_SERVER_ERROR,
			"Parliament encountered an error during IO operations");
	}

	@SuppressWarnings("static-method")
	@ExceptionHandler(QueryParseException.class)
	public ResponseEntity<Object> handle(QueryParseException ex, WebRequest req) {
		return buildResponse(ex, req, HttpStatus.BAD_REQUEST,
			"Parliament is unable to parse the query");
	}

	@SuppressWarnings("static-method")
	@ExceptionHandler(NoAcceptableException.class)
	public ResponseEntity<Object> handle(NoAcceptableException ex, WebRequest req) {
		return buildResponse(ex, req, HttpStatus.NOT_ACCEPTABLE, ex.getMessage());
	}

	private static ResponseEntity<Object> buildResponse(Throwable t, WebRequest req,
			HttpStatus status, String messageFmt, Object... args) {
		LOG.warn("Converting exception to HTTP error:", t);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("exception", t.getClass().getName());
		body.put("exception-message", t.getMessage());
		body.put("path", req.getDescription(false).substring(4));
		body.put("status", status.value());
		body.put("error", status.getReasonPhrase());
		body.put("timestamp", LocalDateTime.now());
		body.put("message", String.format(messageFmt, args));
		return new ResponseEntity<>(body, status);
	}
}
