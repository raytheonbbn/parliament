package com.bbn.parliament.spring.boot.controller;

import java.io.File;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.bbn.parliament.jena.bridge.ParliamentBridge;
import com.bbn.parliament.jena.bridge.util.HttpServerUtil;
import com.bbn.parliament.jena.exception.BadRequestException;
import com.bbn.parliament.spring.boot.service.QueryService;

/**
 * Controller for Spring Boot Server. Routes HTTP requests from
 * /parliament/sparql to appropriate request method.
 *
 * @author pwilliams
 */
@RestController
public class QueryController {
	private static final String ENDPOINT = "/parliament/sparql";
	private static final String URL_ENCODED = "application/x-www-form-urlencoded";
	private static final String SPARQL_QUERY = "application/sparql-query";

	private static final Logger LOG = LoggerFactory.getLogger(QueryController.class);

	@Autowired
	private QueryService queryService;

	@SuppressWarnings("static-method")
	@PostConstruct
	public void initBridge() {
		String modelConfFile = "parliament-config.ttl";

		try {
			ParliamentBridge.initialize(modelConfFile);
		} catch (Exception e) {
			LOG.info("Error occured while initializing Parliament: {}", e.toString());
		}
		LOG.info("Parliament Bridge is now initialized");

		ParliamentBridge bridge = ParliamentBridge.getInstance();
		File tmpDir = bridge.getConfiguration().getTmpDir();
		int threshold = bridge.getConfiguration().getDeferredFileOutputStreamThreshold();
		HttpServerUtil.init(tmpDir, threshold);
	}

	@GetMapping(value = ENDPOINT, params = "query")
	public ResponseEntity<StreamingResponseBody> sparqlGET(
		@RequestParam(value = "query") String query,
		@RequestParam(value = "default-graph-uri", required = false) List<String> defaultGraphUris,
		@RequestParam(value = "named-graph-uri", required = false) List<String> namedGraphUris,
		@RequestParam(value = "format", required = false) String format,
		@RequestHeader HttpHeaders headers,
		HttpServletRequest request) throws BadRequestException {

		return queryService.doQuery(query, defaultGraphUris, namedGraphUris, format, headers, request);
	}

	@PostMapping(value = ENDPOINT, consumes = URL_ENCODED, params = "query")
	public ResponseEntity<StreamingResponseBody> sparqlURLEncodeQueryPOST(
		@RequestParam(value = "query") String query,
		@RequestParam(value = "default-graph-uri", required = false) List<String> defaultGraphUris,
		@RequestParam(value = "named-graph-uri", required = false) List<String> namedGraphUris,
		@RequestParam(value = "format", required = false) String format,
		@RequestHeader HttpHeaders headers,
		HttpServletRequest request) throws BadRequestException {

		return queryService.doQuery(query, defaultGraphUris, namedGraphUris, format, headers, request);
	}

	@PostMapping(value = ENDPOINT, consumes = SPARQL_QUERY)
	public ResponseEntity<StreamingResponseBody> sparqlDirectQueryPOST(
		@RequestBody String query,
		@RequestParam(value = "default-graph-uri", required = false) List<String> defaultGraphUris,
		@RequestParam(value = "named-graph-uri", required = false) List<String> namedGraphUris,
		@RequestParam(value = "format", required = false) String format,
		@RequestHeader HttpHeaders headers,
		HttpServletRequest request) throws BadRequestException {

		return queryService.doQuery(query, defaultGraphUris, namedGraphUris, format, headers, request);
	}
}
