package com.bbn.parliament.spring.boot.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bbn.parliament.jena.bridge.ActionRouter;



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

	@GetMapping(value = ENDPOINT, params = "query")
	public String sparqlGET(
			@RequestParam(value = "query") String query,
			@RequestParam(value = "default-graph-uri", defaultValue = "") List<String> defaultGraphURI,
			@RequestParam(value = "named-graph-uri", defaultValue = "") List<String> namedGraphURI) {
		
		String tempHost = "fake";
		
		ActionRouter router = new ActionRouter();
		
		try {
			router.execQuery(query, tempHost);
		}
		catch(Exception e) {
			
		}

		return String.format("GET Success! Testing changes Query: %1s, %2s", query, defaultGraphURI.toString());
	}

	// Spring does not allow the use of @RequestBody when using URL_ENCODED, so we must use @RequestParam
	@PostMapping(value = ENDPOINT, consumes = URL_ENCODED, params = "query")
	public String sparqlURLEncodeQueryPOST(
			@RequestParam(value = "query") String query,
			@RequestParam(value = "default-graph-uri", defaultValue = "") List<String> defaultGraphURI,
			@RequestParam(value = "named-graph-uri", defaultValue = "") List<String> namedGraphURI) {

		return String.format("POST Success! Query: %s", query);
	}

	@PostMapping(value = ENDPOINT, consumes = SPARQL_QUERY)
	public String sparqlDirectQueryPOST(
			@RequestParam(value = "default-graph-uri", defaultValue = "") List<String> defaultGraphURI,
			@RequestParam(value = "named-graph-uri", defaultValue = "") List<String> namedGraphURI,
			@RequestBody String query) {

		return String.format("POST Success! Query: %s", query);
	}


}
