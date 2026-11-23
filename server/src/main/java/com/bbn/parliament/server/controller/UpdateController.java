package com.bbn.parliament.server.controller;

import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bbn.parliament.server.exception.BadRequestException;
import com.bbn.parliament.server.exception.QueryExecutionException;
import com.bbn.parliament.server.service.UpdateService;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Controller for Spring Boot Server. Routes HTTP requests from
 * /parliament/sparql to appropriate request method.
 *
 * @author pwilliams
 */
@RestController
public class UpdateController {
	private static final String ENDPOINT = "/parliament/update";
	private static final String URL_ENCODED = "application/x-www-form-urlencoded";
	private static final String SPARQL_UPDATE = "application/sparql-update";

	private final UpdateService updateService;

	@Autowired
	public UpdateController(UpdateService service) {
		updateService = Objects.requireNonNull(service, "service");
	}

	@PostMapping(value = ENDPOINT, consumes = URL_ENCODED, params = "update")
	public void sparqlURLEncodeUpdatePOST(
		@RequestParam(value = "update") String update,
		@RequestParam(value = "default-graph-uri", required = false) List<String> defaultGraphUris,
		@RequestParam(value = "named-graph-uri", required = false) List<String> namedGraphUris,
		HttpServletRequest request) throws BadRequestException, QueryExecutionException {

		updateService.doUpdate(update, defaultGraphUris, namedGraphUris, request);
	}

	@PostMapping(value = ENDPOINT, consumes = SPARQL_UPDATE)
	public void sparqlDirectUpdatePOST(
		@RequestBody String update,
		@RequestParam(value = "default-graph-uri", required = false) List<String> defaultGraphUris,
		@RequestParam(value = "named-graph-uri", required = false) List<String> namedGraphUris,
		HttpServletRequest request) throws BadRequestException, QueryExecutionException {

		updateService.doUpdate(update, defaultGraphUris, namedGraphUris, request);
	}
}
