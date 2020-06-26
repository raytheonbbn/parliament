package com.bnn.parliament.spring.boot;


import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.*;


/**
 * Controller for Spring Boot Server. Routes HTTP requests from /parliament/sparql to appropriate request method.
 * 
 * @author pwilliams
 *
 */
@RestController
public class Controller {
	
	private static final String ENDPOINT = "/parliament/sparql";
	private static final String URL_ENCODED = "application/x-www-form-urlencoded";
	private static final String SPARQL_QUERY = "application/sparql-query";

	
	@GetMapping(ENDPOINT)
	public String sparqlGET(
			@RequestParam(value = "query") String query, 
			@RequestParam(value = "default-graph-uri", defaultValue = "") List<String> defaultGraphURI,
			@RequestParam(value = "named-graph-uri", defaultValue = "") List<String> namedGraphURI) {
		
		return String.format("GET Success! Testing changes Query: %1s, %2s", query, defaultGraphURI.toString());
	}
	
	// Spring does not allow the use of @RequestBody when using URL_ENCODED, so we must use @RequestParam
	@PostMapping(value = ENDPOINT, consumes = URL_ENCODED)
	public String sparqlPOST(@RequestParam Map<String, String> requestBody) {
		
		
		return String.format("POST Success! Map: %s", requestBody.toString());
	}
	
	@PostMapping(value = ENDPOINT, consumes = SPARQL_QUERY)
	public String sparqlPOST(
			@RequestParam(value = "default-graph-uri", defaultValue = "") List<String> defaultGraphURI,
			@RequestParam(value = "named-graph-uri", defaultValue = "") List<String> namedGraphURI,
			@RequestBody String query) {
		
		
		return String.format("POST Success! Query: %s", query);
	}

}
