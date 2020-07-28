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
	public void sparqlGraphGET(@RequestParam(value = "graph") String graphURI, HttpServletRequest req, HttpServletResponse resp) {
		graphStoreService.doGet(graphURI, req, resp);
	}

	@GetMapping(value = ENDPOINT, params = "default")
	public void sparqlGraphDefaultGET(@RequestParam(value = "default") String defaultGraph, HttpServletRequest req, HttpServletResponse resp) {
		sparqlGraphGET(DEFAULT_GRAPH, req, resp);
	}

	@PutMapping(value = ENDPOINT, params = "graph")
	public void sparqlGraphPUT(@RequestParam(value = "graph") String graphURI, HttpServletRequest req, HttpServletResponse resp) {
		graphStoreService.doPut(graphURI, req, resp);
	}

	@PutMapping(value = ENDPOINT, params = "default")
	public void sparqlGraphDefaultPUT(@RequestParam(value = "default") String defaultGraph, HttpServletRequest req, HttpServletResponse resp) {
		sparqlGraphPUT(DEFAULT_GRAPH, req, resp);
	}

	@DeleteMapping(value = ENDPOINT, params = "graph")
	public void sparqlGraphDELETE(@RequestParam(value = "graph") String graphURI, HttpServletRequest req, HttpServletResponse resp) {
		graphStoreService.doDelete(graphURI, req, resp);
	}

	@DeleteMapping(value = ENDPOINT, params = "default")
	public void sparqlGraphDefaultDELETE(HttpServletRequest req, HttpServletResponse resp) {
		sparqlGraphDELETE(DEFAULT_GRAPH, req, resp);
	}

	@PostMapping(value = ENDPOINT, params = "graph")
	public void sparqlGraphPOST(@RequestParam(value = "graph") String graphURI, HttpServletRequest req, HttpServletResponse resp) {
		graphStoreService.doPost(req, resp);
	}

	@PostMapping(value = ENDPOINT, params = "default")
	public void sparqlGraphDefaultPOST(@RequestParam(value = "default") String defaultGraph, HttpServletRequest req, HttpServletResponse resp) {
		sparqlGraphPOST(DEFAULT_GRAPH, req, resp);
	}

	@PatchMapping(value = ENDPOINT, params = "graph")
	public void sparqlGraphPATCH(@RequestParam(value = "graph") String graphURI, HttpServletRequest req, HttpServletResponse resp) {
		graphStoreService.doPatch(req, resp);
	}

	@PatchMapping(value = ENDPOINT, params = "default")
	public void sparqlGraphDefaultPATCH(@RequestParam(value = "default") String defaultGraph, HttpServletRequest req, HttpServletResponse resp) {
		sparqlGraphPATCH(DEFAULT_GRAPH, req, resp);
	}

}
