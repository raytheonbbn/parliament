package com.bbn.parliament.spring.boot.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


/**
 * Controller for Spring Boot Server. Routes HTTP requests from /parliament/sparql to appropriate request method.
 *
 * @author pwilliams
 */
@RestController
public class GraphStoreController {

	private static final String ENDPOINT = "/parliament/graphstore";
	private static final String DEFAULT_GRAPH = null;
	
	
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
