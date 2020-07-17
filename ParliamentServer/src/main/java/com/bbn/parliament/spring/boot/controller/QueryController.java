package com.bbn.parliament.spring.boot.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import java.io.OutputStream;
import java.io.IOException;

import javax.annotation.PostConstruct;

import com.bbn.parliament.jena.bridge.ParliamentBridge;

import com.bbn.parliament.spring.boot.service.QueryService;
import com.bnn.parliament.spring.boot.Application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Controller for Spring Boot Server. Routes HTTP requests from /parliament/sparql to appropriate request method.
 *
 * @author pwilliams
 */
@RestController
public class QueryController {
	private static final String ENDPOINT = "/parliament/sparql";
	private static final String URL_ENCODED = "application/x-www-form-urlencoded";
	private static final String SPARQL_QUERY = "application/sparql-query";
	private static final String DEFAULT_GRAPH = null;
	
	private static final Logger LOG = LoggerFactory.getLogger(QueryController.class);

	/*
	@GetMapping(value = ENDPOINT, params = "query")
	public String sparqlGET(
			@RequestParam(value = "query") String query,
			@RequestParam(value = "default-graph-uri", defaultValue = "") List<String> defaultGraphURI,
			@RequestParam(value = "named-graph-uri", defaultValue = "") List<String> namedGraphURI, HttpServletRequest request) {
		
		return QueryService.doCommon(query, request);
		//return String.format("GET Success! Testing changes Query: %1s, %2s", query, defaultGraphURI.toString());
	}
	

	// Spring does not allow the use of @RequestBody when using URL_ENCODED, so we must use @RequestParam
	@PostMapping(value = ENDPOINT, consumes = URL_ENCODED, params = "query")
	public String sparqlURLEncodeQueryPOST(
			@RequestParam(value = "query") String query,
			@RequestParam(value = "default-graph-uri", defaultValue = "") List<String> defaultGraphURI,
			@RequestParam(value = "named-graph-uri", defaultValue = "") List<String> namedGraphURI, HttpServletRequest request) {

		return QueryService.doCommon(query, request);
	}

	@PostMapping(value = ENDPOINT, consumes = SPARQL_QUERY)
	public String sparqlDirectQueryPOST(
			@RequestParam(value = "default-graph-uri", defaultValue = "") List<String> defaultGraphURI,
			@RequestParam(value = "named-graph-uri", defaultValue = "") List<String> namedGraphURI,
			@RequestBody String query, HttpServletRequest request) {

		return QueryService.doCommon(query, request);
	}
	*/
	
	
	@GetMapping(value = ENDPOINT, params = "query")
	public StreamingResponseBody sparqlGET(
			@RequestParam(value = "query") String query,
			@RequestParam(value = "default-graph-uri", defaultValue = "") List<String> defaultGraphURI,
			@RequestParam(value = "named-graph-uri", defaultValue = "") List<String> namedGraphURI, HttpServletRequest request) {
		
		return new StreamingResponseBody() {
			@Override
			public void writeTo(OutputStream out) throws IOException {
				QueryService.doStream(query, request, out);
			}
		};
		//return String.format("GET Success! Testing changes Query: %1s, %2s", query, defaultGraphURI.toString());
	}
	
	@PostMapping(value = ENDPOINT, consumes = URL_ENCODED, params = "query")
	public StreamingResponseBody sparqlURLEncodeQueryPOST(
			@RequestParam(value = "query") String query,
			@RequestParam(value = "default-graph-uri", defaultValue = "") List<String> defaultGraphURI,
			@RequestParam(value = "named-graph-uri", defaultValue = "") List<String> namedGraphURI, HttpServletRequest request) {

		return new StreamingResponseBody() {
			@Override
			public void writeTo(OutputStream out) throws IOException {	
				QueryService.doStream(query, request, out);
			}
		};
	}

	@PostMapping(value = ENDPOINT, consumes = SPARQL_QUERY)
	public StreamingResponseBody sparqlDirectQueryPOST(
			@RequestParam(value = "default-graph-uri", defaultValue = "") List<String> defaultGraphURI,
			@RequestParam(value = "named-graph-uri", defaultValue = "") List<String> namedGraphURI,
			@RequestBody String query, HttpServletRequest request) {

		return new StreamingResponseBody() {
			@Override
			public void writeTo(OutputStream out) throws IOException {
				QueryService.doStream(query, request, out);
			}
		};
	}



	@PostConstruct
	public void initBridge() {

		String modelConfFile = "parliament-config.ttl";

		try {
			ParliamentBridge.initialize(modelConfFile);
		} catch (Exception e) {
			LOG.info("Error occured while initializing Parliament: {}", e.toString());
		}
		LOG.info("Parliament Bridge is now initialized");
	}
}
