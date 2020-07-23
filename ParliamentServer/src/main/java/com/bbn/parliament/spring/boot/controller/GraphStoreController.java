package com.bbn.parliament.spring.boot.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.bbn.parliament.spring.boot.service.GraphStoreService;

import org.springframework.beans.factory.annotation.Autowired;
/**
 * Controller for Spring Boot Server. Routes HTTP requests from /parliament/sparql to appropriate request method.
 *
 * @author pwilliams
 */
@RestController
public class GraphStoreController {

	private static final String ENDPOINT = "/parliament/graphstore";
	private static final String DEFAULT_GRAPH = null;
	
	@Autowired
	GraphStoreService graphStoreService;
	
	
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
	public void sparqlGraphPUT(@RequestParam(value = "graph") String graphURI, @RequestBody String payload,
			HttpServletRequest req, HttpServletResponse resp) {
		
		graphStoreService.doPut(req, resp);
	}

	@PutMapping(value = ENDPOINT, params = "default")
	public void sparqlGraphDefaultPUT(@RequestParam(value = "default") String defaultGraph, @RequestBody String payload,
			HttpServletRequest req, HttpServletResponse resp) {
		sparqlGraphPUT(DEFAULT_GRAPH, payload, req, resp);
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
