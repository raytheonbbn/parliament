package com.bbn.parliament.spring.boot.controller;

import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


/**
 * Controller for Spring Boot Server. Routes HTTP requests from /parliament/sparql to appropriate request method.
 *
 * @author pwilliams
 */
@RestController
public class UpdateController {

	private static final String ENDPOINT = "/parliament/update";
	private static final String URL_ENCODED = "application/x-www-form-urlencoded";
	private static final String SPARQL_UPDATE = "application/sparql-update";
	
	@PostMapping(value = ENDPOINT, consumes = URL_ENCODED, params = "update")
	public String sparqlURLEncodeUpdatePOST(
			@RequestParam(value = "update") String update,
			@RequestParam(value = "using-graph-uri", defaultValue = "") List<String> defaultGraphURI,
			@RequestParam(value = "using-named-graph-uri", defaultValue = "") List<String> namedGraphURI) {

		return String.format("POST Success! Update: %s", update);
	}


	@PostMapping(value = ENDPOINT, consumes = SPARQL_UPDATE)
	public String sparqlDirectUpdatePOST(
			@RequestParam(value = "using-graph-uri", defaultValue = "") List<String> defaultGraphURI,
			@RequestParam(value = "using-named-graph-uri", defaultValue = "") List<String> namedGraphURI,
			@RequestBody String update) {

		return String.format("POST Success! Update: %s", update);
	}
}
