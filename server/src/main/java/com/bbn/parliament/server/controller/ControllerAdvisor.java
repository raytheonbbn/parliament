package com.bbn.parliament.server.controller;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.jena.query.QueryParseException;
import org.apache.jena.shared.JenaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.bbn.parliament.server.exception.BadRequestException;
import com.bbn.parliament.server.exception.DataFormatException;
import com.bbn.parliament.server.exception.MissingGraphException;
import com.bbn.parliament.server.exception.NoAcceptableException;
import com.bbn.parliament.server.exception.QueryExecutionException;
import com.bbn.parliament.server.exception.TrackableException;
import com.bbn.parliament.server.exception.UnsupportedEndpointException;

/*
 * Note that the base class, ResponseEntityExceptionHandler, has a generic handler
 * method (first argument types as Exception) that is annotated to handle a sizable
 * list of exception classes. If you implement a handler here that handles one of
 * those classes, you will get a "multiple exception handlers defined" error that
 * prevents the application context from initializing.
 *
 * The easy solution is to not handle any of those exceptions here. Harder, but
 * possible is to override the method in the base class.
 */
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
	@ExceptionHandler(TrackableException.class)
	public ResponseEntity<Object> handle(TrackableException ex, WebRequest req) {
		return buildResponse(ex, req, HttpStatus.INTERNAL_SERVER_ERROR,
			"Parliament encountered an error with Trackable");
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
		return buildResponse(ex, req, HttpStatus.NOT_ACCEPTABLE,
			"Parliament does not support the requested media type(s)");
	}

	private static ResponseEntity<Object> buildResponse(Throwable t, WebRequest req,
		HttpStatus status, String messageFmt, Object... args) {
		LOG.warn("Converting exception to HTTP error:", t);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("exception", t.getClass().getName());
		body.put("exception-message", t.getMessage());
		body.put("path", req.getDescription(false).substring(4));
		body.put("status", "%1$d %2$s".formatted(status.value(), status.getReasonPhrase()));
		body.put("timestamp", LocalDateTime.now());
		body.put("message", messageFmt.formatted(args));
		return new ResponseEntity<>(body, status);
	}
}
