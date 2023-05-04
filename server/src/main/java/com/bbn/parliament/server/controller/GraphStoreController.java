package com.bbn.parliament.server.controller;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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

import com.bbn.parliament.server.exception.DataFormatException;
import com.bbn.parliament.server.exception.MissingGraphException;
import com.bbn.parliament.server.exception.QueryExecutionException;
import com.bbn.parliament.server.exception.TrackableException;
import com.bbn.parliament.server.exception.UnsupportedEndpointException;
import com.bbn.parliament.server.service.DropGraphOption;
import com.bbn.parliament.server.service.GraphOperation;
import com.bbn.parliament.server.service.GraphStoreService;

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

	private final GraphStoreService graphStoreService;

	@Autowired
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
		HttpServletRequest request)
		throws QueryExecutionException, MissingGraphException {

		graphStoreService.createDropGraph(GraphOperation.DROP, DEFAULT_GRAPH,
			DropGraphOption.NOISY, request);
	}

	@DeleteMapping(value = ENDPOINT, params = "graph")
	public void deleteNamedGraph(
		@RequestParam(value = "graph") String graphUri,
		HttpServletRequest request)
		throws QueryExecutionException, MissingGraphException {

		graphStoreService.createDropGraph(GraphOperation.DROP, graphUri,
			DropGraphOption.NOISY, request);
	}

	@PostMapping(value = ENDPOINT, params = "default")
	public ResponseEntity<String> insertIntoDefaultGraph(
		@RequestHeader(HttpHeaders.CONTENT_TYPE) String contentType,
		@RequestParam(value = "default") String defaultGraph,
		HttpServletRequest request)
		throws TrackableException, DataFormatException, MissingGraphException, IOException,
		QueryExecutionException {

		return graphStoreService.doGraphInsertOrReplace(DEFAULT_GRAPH, HttpMethod.POST,
			contentType, request);
	}

	@PostMapping(value = ENDPOINT, params = "graph")
	public ResponseEntity<String> insertIntoNamedGraph(
		@RequestHeader(HttpHeaders.CONTENT_TYPE) String contentType,
		@RequestParam(value = "graph") String graphUri,
		HttpServletRequest request)
		throws TrackableException, DataFormatException, MissingGraphException, IOException,
		QueryExecutionException {

		return graphStoreService.doGraphInsertOrReplace(graphUri, HttpMethod.POST,
			contentType, request);
	}

	@PostMapping(value = ENDPOINT, params = "default", consumes = "multipart/form-data")
	public ResponseEntity<String> insertIntoDefaultGraph(
		@RequestParam(value = "default") String defaultGraph,
		HttpServletRequest request,
		@RequestPart(value = "file") MultipartFile[] files)
		throws TrackableException, DataFormatException, MissingGraphException, IOException,
		QueryExecutionException {

		return graphStoreService.doGraphInsertOrReplace(DEFAULT_GRAPH, HttpMethod.POST,
			request, files);
	}

	@PostMapping(value = ENDPOINT, params = "graph", consumes = "multipart/form-data")
	public ResponseEntity<String> insertIntoNamedGraph(
		@RequestParam(value = "graph") String graphUri,
		HttpServletRequest request,
		@RequestPart(value = "file") MultipartFile[] files)
		throws TrackableException, DataFormatException, MissingGraphException, IOException,
		QueryExecutionException {

		return graphStoreService.doGraphInsertOrReplace(graphUri, HttpMethod.POST,
			request, files);
	}

	@PutMapping(value = ENDPOINT, params = "default")
	public ResponseEntity<String> replaceDefaultGraph(
		@RequestHeader(HttpHeaders.CONTENT_TYPE) String contentType,
		@RequestParam(value = "default") String defaultGraph,
		HttpServletRequest request) throws QueryExecutionException,
		TrackableException, DataFormatException, MissingGraphException, IOException {

		return graphStoreService.doGraphInsertOrReplace(DEFAULT_GRAPH, HttpMethod.PUT,
			contentType, request);
	}

	@PutMapping(value = ENDPOINT, params = "graph")
	public ResponseEntity<String> replaceNamedGraph(
		@RequestHeader(HttpHeaders.CONTENT_TYPE) String contentType,
		@RequestParam(value = "graph") String graphUri,
		HttpServletRequest request) throws QueryExecutionException,
		TrackableException, DataFormatException, MissingGraphException, IOException {

		return graphStoreService.doGraphInsertOrReplace(graphUri, HttpMethod.PUT,
			contentType, request);
	}

	@PutMapping(value = ENDPOINT, params = "default", consumes = "multipart/form-data")
	public ResponseEntity<String> replaceDefaultGraph(
		@RequestHeader(name = HttpHeaders.CONTENT_TYPE, required = false) Optional<String> contentType,
		@RequestParam(value = "default") String defaultGraph,
		HttpServletRequest request,
		@RequestPart(value = "file") MultipartFile[] files) throws QueryExecutionException,
		TrackableException, DataFormatException, MissingGraphException, IOException {

		return graphStoreService.doGraphInsertOrReplace(DEFAULT_GRAPH, HttpMethod.PUT,
			request, files);
	}

	@PutMapping(value = ENDPOINT, params = "graph", consumes = "multipart/form-data")
	public ResponseEntity<String> replaceNamedGraph(
		@RequestHeader(name = HttpHeaders.CONTENT_TYPE, required = false) Optional<String> contentType,
		@RequestParam(value = "graph") String graphUri,
		HttpServletRequest request,
		@RequestPart(value = "file") MultipartFile[] files) throws QueryExecutionException,
		TrackableException, DataFormatException, MissingGraphException, IOException {

		return graphStoreService.doGraphInsertOrReplace(graphUri, HttpMethod.PUT,
			request, files);
	}

	@SuppressWarnings("static-method")
	@PatchMapping(value = ENDPOINT, params = "default")
	public void updateDefaultGraph(@RequestParam(value = "default") String defaultGraph) {
		throw new UnsupportedEndpointException("""
			The PATCH method is unsupported on this endpoint. Please use the \
			SPARQL Update endpoint instead.""");
	}

	@SuppressWarnings("static-method")
	@PatchMapping(value = ENDPOINT, params = "graph")
	public void updateNamedGraph(@RequestParam(value = "graph") String graphUri) {
		throw new UnsupportedEndpointException("""
			The PATCH method is unsupported on this endpoint. Please use the \
			SPARQL Update endpoint instead.""");
	}
}
