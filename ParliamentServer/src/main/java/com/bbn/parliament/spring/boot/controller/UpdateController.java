package com.bbn.parliament.spring.boot.controller;

import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bbn.parliament.spring.boot.service.UpdateService;

import javax.servlet.http.HttpServletRequest;

/**
 * Controller for Spring Boot Server. Routes HTTP requests from /parliament/sparql to appropriate request method.
 *
 * @author pwilliams
 */
@RestController
public class UpdateController {

	private static final String ENDPOINT = "/parliament/update";
	private static final String URL_ENCODED = "application/x-www-form-urlencoded";
	private static final String SPARQL_UPDATE = "application/sparql-update";
	
	@PostMapping(value = ENDPOINT, consumes = URL_ENCODED, params = "update")
	public void sparqlURLEncodeUpdatePOST(
			@RequestParam(value = "update") String update,
			@RequestParam(value = "using-graph-uri", defaultValue = "") List<String> defaultGraphURI,
			@RequestParam(value = "using-named-graph-uri", defaultValue = "") List<String> namedGraphURI,
			HttpServletRequest request) {

		//return String.format("POST Success! Update: %s", update);
		UpdateService.doUpdate(update, request);
	}


	@PostMapping(value = ENDPOINT, consumes = SPARQL_UPDATE)
	public void sparqlDirectUpdatePOST(
			@RequestParam(value = "using-graph-uri", defaultValue = "") List<String> defaultGraphURI,
			@RequestParam(value = "using-named-graph-uri", defaultValue = "") List<String> namedGraphURI,
			@RequestBody String update,
			HttpServletRequest request) {

		//return String.format("POST Success! Update: %s", update);
		UpdateService.doUpdate(update, request);
	}
}
