package com.bbn.parliament.spring_boot.controller;

import java.io.File;
import java.util.List;
import java.util.Objects;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
import com.bbn.parliament.jena.bridge.ParliamentBridgeConfiguration;
import com.bbn.parliament.jena.bridge.ParliamentBridgeException;
import com.bbn.parliament.jena.bridge.util.HttpServerUtil;
import com.bbn.parliament.jena.exception.BadRequestException;
import com.bbn.parliament.spring_boot.service.QueryService;

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

	private final QueryService queryService;

	@Value("${spring.application.name}")
	private String appName;

	@Value("${multipart.location}")
	private String multipartLocation;

	@Value("${parliament.bridge.config}")
	private String parliamentBridgeConfigFile;

	public QueryController(QueryService service) {
		queryService = Objects.requireNonNull(service, "service");
	}

	@PostConstruct
	public void initBridge() {
		LOG.info("spring.application.name set to '{}'", appName);
		LOG.info("multipart.location set to '{}'", multipartLocation);
		LOG.info("parliament.bridge.config set to '{}'", parliamentBridgeConfigFile);
		try {
			LOG.info("Initializing Parliament Bridge ...");
			File tempDir = getTempDir();
			LOG.info("Temp dir set to '{}'", tempDir.getPath());
			ParliamentBridge.initialize(parliamentBridgeConfigFile, tempDir);
			LOG.info("Parliament Bridge initialization finished");
		} catch (ParliamentBridgeException ex) {
			LOG.error("Unable to initialize ParliamentBridge:", ex);
			throw new IllegalStateException("Error while initializing ParliamentBridge", ex);
		}

		ParliamentBridgeConfiguration bridgeConfig = ParliamentBridge.getInstance()
			.getConfiguration();
		HttpServerUtil.init(
			bridgeConfig.getTmpDir(),
			bridgeConfig.getDeferredFileOutputStreamThreshold());
	}

	@SuppressWarnings("static-method")
	@PreDestroy
	public void destroy() {
		LOG.info("Shutting down parliament servlet from QueryController");
		ParliamentBridge.getInstance().stop();
	}

	private File getTempDir() {
		File result = null;
		if (!StringUtils.isBlank(multipartLocation)) {
			result = new File(multipartLocation.strip());
			if (!result.exists() || !result.isDirectory()) {
				result = null;
			}
		}
		return (result == null)
			? new File(System.getProperty("java.io.tmpdir", "."))
			: result;
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
