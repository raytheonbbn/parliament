package com.bbn.parliament.spring_boot.controller;

import java.io.IOException;
import java.util.Objects;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.bbn.parliament.jena.bridge.tracker.TrackableException;
import com.bbn.parliament.jena.exception.DataFormatException;
import com.bbn.parliament.jena.exception.MissingGraphException;
import com.bbn.parliament.jena.exception.QueryExecutionException;
import com.bbn.parliament.jena.exception.UnsupportedEndpointException;
import com.bbn.parliament.spring_boot.service.DropGraphOption;
import com.bbn.parliament.spring_boot.service.GraphStoreService;

/**
 * Controller for Spring Boot Server. Routes HTTP requests from
 * /parliament/graphstore to appropriate request method.
 *
 * @author pwilliams
 */
@RestController
public class GraphStoreController {
	private static final String ENDPOINT = "/parliament/graphstore";
	public static final String DEFAULT_GRAPH = null;

	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(GraphStoreController.class);

	private final GraphStoreService graphStoreService;

	public GraphStoreController(GraphStoreService service) {
		graphStoreService = Objects.requireNonNull(service, "service");
	}

	//HEAD mapping automatically supported by GET mapping
	@GetMapping(value = ENDPOINT, params = "default")
	public ResponseEntity<StreamingResponseBody> getDefaultGraph(
		@RequestParam(value = "default") String defaultGraph,
		@RequestParam(value = "format", required = false) String format,
		@RequestHeader HttpHeaders headers,
		HttpServletRequest request) {

		return graphStoreService.doGetGraph(DEFAULT_GRAPH, format, headers, request);
	}

	//HEAD mapping automatically supported by GET mapping
	@GetMapping(value = ENDPOINT, params = "graph")
	public ResponseEntity<StreamingResponseBody> getNamedGraph(
		@RequestParam(value = "graph") String graphUri,
		@RequestParam(value = "format", required = false) String format,
		@RequestHeader HttpHeaders headers,
		HttpServletRequest request) {

		return graphStoreService.doGetGraph(graphUri, format, headers, request);
	}

	@DeleteMapping(value = ENDPOINT, params = "default")
	public void deleteDefaultGraph(
		@RequestParam(value = "default") String defaultGraph,
		@RequestHeader HttpHeaders headers,
		HttpServletRequest request)
		throws QueryExecutionException, MissingGraphException {

		graphStoreService.doDeleteGraph(DEFAULT_GRAPH, headers, request, DropGraphOption.NOISY);
	}

	@DeleteMapping(value = ENDPOINT, params = "graph")
	public void deleteNamedGraph(
		@RequestParam(value = "graph") String graphUri,
		@RequestHeader HttpHeaders headers,
		HttpServletRequest request)
		throws QueryExecutionException, MissingGraphException {

		graphStoreService.doDeleteGraph(graphUri, headers, request, DropGraphOption.NOISY);
	}

	@PostMapping(value = ENDPOINT, params = "default")
	public ResponseEntity<String> insertIntoDefaultGraph(
		@RequestHeader(value = "Content-Type") String contentType,
		@RequestParam(value = "default") String defaultGraph,
		@RequestHeader HttpHeaders headers,
		HttpServletRequest request,
		HttpEntity<byte[]> requestEntity)
		throws TrackableException, DataFormatException, MissingGraphException, IOException {

		return graphStoreService.doInsertIntoGraph(contentType, DEFAULT_GRAPH, headers, request, requestEntity);
	}

	@PostMapping(value = ENDPOINT, params = "graph")
	public ResponseEntity<String> insertIntoNamedGraph(
		@RequestHeader(value = "Content-Type") String contentType,
		@RequestParam(value = "graph") String graphUri,
		@RequestHeader HttpHeaders headers,
		HttpServletRequest request,
		HttpEntity<byte[]> requestEntity)
		throws TrackableException, DataFormatException, MissingGraphException, IOException {

		return graphStoreService.doInsertIntoGraph(contentType, graphUri, headers, request, requestEntity);
	}

	@PostMapping(value = ENDPOINT, params = "default", consumes = "multipart/form-data")
	public ResponseEntity<String> insertIntoDefaultGraph(
		@RequestParam(value = "default") String defaultGraph,
		@RequestHeader HttpHeaders headers,
		HttpServletRequest request,
		@RequestPart(value = "file") MultipartFile[] files)
		throws TrackableException, DataFormatException, MissingGraphException, IOException {

		return graphStoreService.doInsertIntoGraph(DEFAULT_GRAPH, headers, request, files);
	}

	@PostMapping(value = ENDPOINT, params = "graph", consumes = "multipart/form-data")
	public ResponseEntity<String> insertIntoNamedGraph(
		@RequestParam(value = "graph") String graphUri,
		@RequestHeader HttpHeaders headers,
		HttpServletRequest request,
		@RequestPart(value = "file") MultipartFile[] files)
		throws TrackableException, DataFormatException, MissingGraphException, IOException {

		return graphStoreService.doInsertIntoGraph(graphUri, headers, request, files);
	}

	@PutMapping(value = ENDPOINT, params = "default")
	public ResponseEntity<String> replaceDefaultGraph(
		@RequestHeader(value = "Content-Type") String contentType,
		@RequestParam(value = "default") String defaultGraph,
		@RequestHeader HttpHeaders headers,
		HttpServletRequest request,
		HttpEntity<byte[]> requestEntity) throws QueryExecutionException,
		TrackableException, DataFormatException, MissingGraphException, IOException {

		return graphStoreService.doReplaceGraph(contentType, DEFAULT_GRAPH, headers, request, requestEntity);
	}

	@PutMapping(value = ENDPOINT, params = "graph")
	public ResponseEntity<String> replaceNamedGraph(
		@RequestHeader(value = "Content-Type") String contentType,
		@RequestParam(value = "graph") String graphUri,
		@RequestHeader HttpHeaders headers,
		HttpServletRequest request,
		HttpEntity<byte[]> requestEntity) throws QueryExecutionException,
		TrackableException, DataFormatException, MissingGraphException, IOException {

		return graphStoreService.doReplaceGraph(contentType, graphUri, headers, request, requestEntity);
	}

	@PutMapping(value = ENDPOINT, params = "default", consumes = "multipart/form-data")
	public ResponseEntity<String> replaceDefaultGraph(
		@RequestHeader(value = "Content-Type") String contentType,
		@RequestParam(value = "default") String defaultGraph,
		@RequestHeader HttpHeaders headers,
		HttpServletRequest request,
		@RequestPart(value = "file") MultipartFile[] files) throws QueryExecutionException,
		TrackableException, DataFormatException, MissingGraphException, IOException {

		return graphStoreService.doReplaceGraph(contentType, DEFAULT_GRAPH, headers, request, files);
	}

	@PutMapping(value = ENDPOINT, params = "graph", consumes = "multipart/form-data")
	public ResponseEntity<String> replaceNamedGraph(
		@RequestHeader(value = "Content-Type") String contentType,
		@RequestParam(value = "graph") String graphUri,
		@RequestHeader HttpHeaders headers,
		HttpServletRequest request,
		@RequestPart(value = "file") MultipartFile[] files) throws QueryExecutionException,
		TrackableException, DataFormatException, MissingGraphException, IOException {

		return graphStoreService.doReplaceGraph(contentType, graphUri, headers, request, files);
	}

	@SuppressWarnings("static-method")
	@PatchMapping(value = ENDPOINT, params = "default")
	public void updateDefaultGraph(@RequestParam(value = "default") String defaultGraph) {
		throw new UnsupportedEndpointException("The PATCH method is unsupported on this "
			+ "endpoint. Please use the SPARQL Update endpoint instead.");
	}

	@SuppressWarnings("static-method")
	@PatchMapping(value = ENDPOINT, params = "graph")
	public void updateNamedGraph(@RequestParam(value = "graph") String graphUri) {
		throw new UnsupportedEndpointException("The PATCH method is unsupported on this "
			+ "endpoint. Please use the SPARQL Update endpoint instead.");
	}
}
