package com.bbn.parliament.spring.boot.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.HttpEntity;

import java.io.ByteArrayInputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.bbn.parliament.spring.boot.service.GraphStoreService;
import com.bbn.parliament.spring.boot.service.QueryService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for Spring Boot Server. Routes HTTP requests from /parliament/sparql to appropriate request method.
 *
 * @author pwilliams
 */
@RestController
public class GraphStoreController {

	private static final String ENDPOINT = "/parliament/graphstore";
	private static final String DEFAULT_GRAPH = null;
	
	private static final Logger LOG = LoggerFactory.getLogger(QueryService.class);
	
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
	public void sparqlGraphPUT(
			@RequestHeader(value = "Content-Type") String contentType, 
			@RequestParam(value = "graph") String graphURI,
			HttpEntity<byte[]> requestEntity, 
			HttpServletRequest res, HttpServletResponse resp) {
		graphStoreService.doPut(contentType, graphURI, requestEntity, res, resp);
	}

	@PutMapping(value = ENDPOINT, params = "default")
	public void sparqlGraphDefaultPUT(
			@RequestHeader(value = "Content-Type") String contentType, 
			@RequestParam(value = "default") String defaultGraph, 
			HttpEntity<byte[]> requestEntity, 
			HttpServletRequest res, HttpServletResponse resp) {
		sparqlGraphPUT(contentType, DEFAULT_GRAPH, requestEntity, res, resp);
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
	public void sparqlGraphPOST(
			@RequestHeader(value = "Content-Type") String contentType, 
			@RequestParam(value = "graph") String graphURI,
			HttpEntity<byte[]> requestEntity, 
			HttpServletRequest res, HttpServletResponse resp) {
		graphStoreService.doPost(contentType, graphURI, requestEntity, res, resp);
	}

	@PostMapping(value = ENDPOINT, params = "default")
	public void sparqlGraphDefaultPOST(
			@RequestHeader(value = "Content-Type") String contentType, 
			@RequestParam(value = "default") String defaultGraph, 
			HttpEntity<byte[]> requestEntity, 
			HttpServletRequest res, HttpServletResponse resp) {
		sparqlGraphPOST(contentType, DEFAULT_GRAPH, requestEntity, res, resp);
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
