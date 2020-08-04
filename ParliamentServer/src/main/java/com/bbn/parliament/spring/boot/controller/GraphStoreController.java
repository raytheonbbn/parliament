package com.bbn.parliament.spring.boot.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.bbn.parliament.jena.graph.ModelManager;
import com.bbn.parliament.spring.boot.service.GraphStoreService;

/**
 * Controller for Spring Boot Server. Routes HTTP requests from /parliament/sparql to appropriate request method.
 *
 * @author pwilliams
 */
@RestController
public class GraphStoreController {

	private static final String ENDPOINT = "/parliament/graphstore";
	private static final String DEFAULT_GRAPH = null;

	private static final Logger LOG = LoggerFactory.getLogger(GraphStoreController.class);

	@Autowired
	GraphStoreService graphStoreService;


	//HEAD mapping automatically supported by GET mapping
	@GetMapping(value = ENDPOINT, params = "graph")
	public void sparqlGraphGET(@RequestParam(value = "graph") String graphURI, HttpServletRequest req, HttpServletResponse resp) {
		if (graphURI == null || ModelManager.inst().containsModel(graphURI)) {
			try {
				graphStoreService.doGet(graphURI, req, resp);
			} catch (Exception e) {
				throw new InternalServerException();
			}
		}
		else {
			throw new NoGraphException();
		}
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
		try {
			graphStoreService.doPut(contentType, graphURI, requestEntity, res, resp);
		} catch (Exception e) {
			throw new InternalServerException();
		}
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
	public void sparqlGraphDELETE(@RequestParam(value = "graph") String graphURI) {
		if (graphURI == null || ModelManager.inst().containsModel(graphURI)) {
			try {
				graphStoreService.doDelete(graphURI);
			} catch (Exception e) {
				throw new InternalServerException();
			}
		}
		else {
			throw new NoGraphException();
		}
	}

	@DeleteMapping(value = ENDPOINT, params = "default")
	public void sparqlGraphDefaultDELETE() {
		sparqlGraphDELETE(DEFAULT_GRAPH);
	}

	@PostMapping(value = ENDPOINT, params = "graph")
	public void sparqlGraphPOST(
			@RequestHeader(value = "Content-Type") String contentType,
			@RequestParam(value = "graph") String graphURI,
			HttpEntity<byte[]> requestEntity,
			HttpServletRequest res, HttpServletResponse resp) {
		try {
			graphStoreService.doPost(contentType, graphURI, requestEntity, res, resp);
		} catch (Exception e) {
			throw new InternalServerException();
		}
	}

	@PostMapping(value = ENDPOINT, params = "default")
	public void sparqlGraphDefaultPOST(
			@RequestHeader(value = "Content-Type") String contentType,
			@RequestParam(value = "default") String defaultGraph,
			HttpEntity<byte[]> requestEntity,
			HttpServletRequest res, HttpServletResponse resp) {
		sparqlGraphPOST(contentType, DEFAULT_GRAPH, requestEntity, res, resp);
	}

	//file
	@PostMapping(value = ENDPOINT, params = "graph", consumes = "multipart/form-data")
	public void sparqlGraphFilePOST(
			@RequestHeader(value = "Content-Type") String contentType,
			@RequestParam(value = "graph") String graphURI,
			@RequestPart(value = "file") MultipartFile[] files,
			HttpServletRequest res, HttpServletResponse resp) {
		try {
			graphStoreService.doFilePost(contentType, graphURI, files, res, resp);
		} catch (Exception e) {
			throw new InternalServerException();
		}
	}

	//file
	@PostMapping(value = ENDPOINT, params = "default", consumes = "multipart/form-data")
	public void sparqlGraphDefaultFilePOST(
			@RequestHeader(value = "Content-Type") String contentType,
			@RequestParam(value = "default") String defaultGraph,
			@RequestPart(value = "file") MultipartFile[] files,
			HttpServletRequest res, HttpServletResponse resp) {
		sparqlGraphFilePOST(contentType, DEFAULT_GRAPH, files, res, resp);
	}

	@PatchMapping(value = ENDPOINT, params = "graph")
	public void sparqlGraphPATCH(@RequestParam(value = "graph") String graphURI, HttpServletRequest req, HttpServletResponse resp) {
		throw new UnsupportedEndpointException();
	}

	@PatchMapping(value = ENDPOINT, params = "default")
	public void sparqlGraphDefaultPATCH(@RequestParam(value = "default") String defaultGraph, HttpServletRequest req, HttpServletResponse resp) {
		sparqlGraphPATCH(DEFAULT_GRAPH, req, resp);
	}
	
	@ResponseStatus(value=HttpStatus.NOT_FOUND, reason="Specified graph not found")
	public class NoGraphException extends RuntimeException {}
}
