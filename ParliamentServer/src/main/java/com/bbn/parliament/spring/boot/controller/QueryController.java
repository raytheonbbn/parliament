package com.bbn.parliament.spring.boot.controller;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.bbn.parliament.jena.bridge.ParliamentBridge;
import com.bbn.parliament.jena.bridge.util.HttpServerUtil;
import com.bbn.parliament.spring.boot.service.QueryService;




/**
 * Controller for Spring Boot Server. Routes HTTP requests from /parliament/sparql to appropriate request method.
 *
 * @author pwilliams
 */
@RestController
public class QueryController {
	private static final String ENDPOINT = "/parliament/sparql";
	private static final String URL_ENCODED = "application/x-www-form-urlencoded";
	private static final String SPARQL_QUERY = "application/sparql-query";
	private static final String DEFAULT_GRAPH = null;

	private static final Logger LOG = LoggerFactory.getLogger(QueryController.class);

	@Autowired
	private QueryService queryService;


	@GetMapping(value = ENDPOINT, params = "query")
	public StreamingResponseBody sparqlGET(
			@RequestParam(value = "query") String query,
			@RequestParam(value = "default-graph-uri", defaultValue = "") List<String> defaultGraphURI,
			@RequestParam(value = "named-graph-uri", defaultValue = "") List<String> namedGraphURI, HttpServletRequest request) throws Exception {

		if (defaultGraphURI.size() > 0 || namedGraphURI.size() > 0) {
			throw new BadRequestException();
		}

		return new StreamingResponseBody() {
			@Override
			public void writeTo(OutputStream out) throws IOException {
				try {
					queryService.doStream(query, request, out);
				} catch(Exception e) {
					throw new IOException(e);
				}
			}
		};
	}

	@PostMapping(value = ENDPOINT, consumes = URL_ENCODED, params = "query")
	public StreamingResponseBody sparqlURLEncodeQueryPOST(
			@RequestParam(value = "query") String query,
			@RequestParam(value = "default-graph-uri", defaultValue = "") List<String> defaultGraphURI,
			@RequestParam(value = "named-graph-uri", defaultValue = "") List<String> namedGraphURI, HttpServletRequest request) throws Exception {

		if (defaultGraphURI.size() > 0 || namedGraphURI.size() > 0) {
			throw new BadRequestException();
		}

		return new StreamingResponseBody() {
			@Override
			public void writeTo(OutputStream out) throws IOException {
				try {
					queryService.doStream(query, request, out);
				} catch(Exception e) {
					throw new IOException(e);
				}
			}
		};
	}

	@PostMapping(value = ENDPOINT, consumes = SPARQL_QUERY)
	public StreamingResponseBody sparqlDirectQueryPOST(
			@RequestParam(value = "default-graph-uri", defaultValue = "") List<String> defaultGraphURI,
			@RequestParam(value = "named-graph-uri", defaultValue = "") List<String> namedGraphURI,
			@RequestBody String query, HttpServletRequest request) throws Exception {

		if (defaultGraphURI.size() > 0 || namedGraphURI.size() > 0) {
			throw new BadRequestException();
		}

		return new StreamingResponseBody() {
			@Override
			public void writeTo(OutputStream out) throws IOException {
				try {
					queryService.doStream(query, request, out);
				} catch(Exception e) {
					throw new IOException(e);
				}
			}
		};
	}

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
}
