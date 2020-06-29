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
	private static final String DEFAULT_GRAPH = "Placeholder Graph URI";

	
	@GetMapping(value = ENDPOINT, params = "query")
	public String sparqlGET(
			@RequestParam(value = "query") String query, 
			@RequestParam(value = "default-graph-uri", defaultValue = "") List<String> defaultGraphURI,
			@RequestParam(value = "named-graph-uri", defaultValue = "") List<String> namedGraphURI) {
		
		return String.format("GET Success! Testing changes Query: %1s, %2s", query, defaultGraphURI.toString());
	}
	
	// Spring does not allow the use of @RequestBody when using URL_ENCODED, so we must use @RequestParam
	@PostMapping(value = ENDPOINT, consumes = URL_ENCODED)
	public String sparqlURLEncodePOST(@RequestParam Map<String, String> requestBody) {
		
		return String.format("POST Success! Map: %s", requestBody.toString());
	}
	
	@PostMapping(value = ENDPOINT, consumes = SPARQL_QUERY)
	public String sparqlDirectPOST(
			@RequestParam(value = "default-graph-uri", defaultValue = "") List<String> defaultGraphURI,
			@RequestParam(value = "named-graph-uri", defaultValue = "") List<String> namedGraphURI,
			@RequestBody String query) {
		
		return String.format("POST Success! Query: %s", query);
	}
	
	//HEAD mapping automatically supported by GET mapping
	@GetMapping(value = ENDPOINT, params = "graph")
	public String sparqlGraphGET(@RequestParam(value = "graph") String graphURI) {
		
		return String.format("placeholder");
	}
	
	@GetMapping(value = ENDPOINT, params = "default")
	public String sparqlGraphDefaultGET(@RequestParam(value = "default") String defaultGraph) {
		return sparqlGraphGET(DEFAULT_GRAPH);
	}
	
	@PutMapping(value = ENDPOINT, params = "graph")
	public String sparqlGraphPUT(@RequestParam(value = "graph") String graphURI, @RequestBody String payload) {
		
		return String.format("The payload is: %s", payload);
	}
	
	@PutMapping(value = ENDPOINT, params = "default")
	public String sparqlGraphDefaultPUT(@RequestParam(value = "default") String defaultGraph, @RequestBody String payload) {
		return sparqlGraphPUT(DEFAULT_GRAPH, payload);
	}
	
	@DeleteMapping(value = ENDPOINT, params = "graph")
	public String sparqlGraphDELETE() {
		
		return String.format("placeholder");
	}
	
	@DeleteMapping(value = ENDPOINT, params = "default")
	public String sparqlGraphDefaultDELETE() {
		
		return String.format("placeholder");
	}
	
	@PostMapping(value = ENDPOINT, params = "graph")
	public String sparqlGraphPOST(@RequestParam(value = "graph") String graphURI, @RequestBody String payload) {
		
		return String.format("The payload is: %s", payload);
	}
	
	@PostMapping(value = ENDPOINT, params = "default")
	public String sparqlGraphDefaultPOST(@RequestParam(value = "default") String defaultGraph, @RequestBody String payload) {
		return sparqlGraphPOST(DEFAULT_GRAPH, payload);
	}
	
	@PatchMapping(value = ENDPOINT, params = "graph")
	public String sparqlGraphPATCH(@RequestParam(value = "graph") String graphURI, @RequestBody String payload) {
		
		return String.format("The payload is: %s", payload);
	}
	
	@PatchMapping(value = ENDPOINT, params = "default")
	public String sparqlGraphDefaultPATCH(@RequestParam(value = "default") String defaultGraph, @RequestBody String payload) {
		return sparqlGraphPATCH(DEFAULT_GRAPH, payload);
	}

}
