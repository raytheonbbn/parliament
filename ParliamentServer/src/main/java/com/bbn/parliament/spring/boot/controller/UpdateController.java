package com.bbn.parliament.spring.boot.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.bbn.parliament.spring.boot.service.UpdateService;


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

	@Autowired
	private UpdateService updateService;

	@PostMapping(value = ENDPOINT, consumes = URL_ENCODED, params = "update")
	public void sparqlURLEncodeUpdatePOST(
			@RequestParam(value = "update") String update,
			@RequestParam(value = "using-graph-uri", defaultValue = "") List<String> defaultGraphURI,
			@RequestParam(value = "using-named-graph-uri", defaultValue = "") List<String> namedGraphURI,
			HttpServletRequest request) {

		if (defaultGraphURI.size() > 0 || namedGraphURI.size() > 0) {
			throw new BadRequestException();
		}
		//return String.format("POST Success! Update: %s", update);
		try {
			updateService.doUpdate(update, request);
		} catch (Exception e) {
			throw new InternalServerException();
		}
	}


	@PostMapping(value = ENDPOINT, consumes = SPARQL_UPDATE)
	public void sparqlDirectUpdatePOST(
			@RequestParam(value = "using-graph-uri", defaultValue = "") List<String> defaultGraphURI,
			@RequestParam(value = "using-named-graph-uri", defaultValue = "") List<String> namedGraphURI,
			@RequestBody String update,
			HttpServletRequest request) {

		if (defaultGraphURI.size() > 0 || namedGraphURI.size() > 0) {
			throw new BadRequestException();
		}
		//return String.format("POST Success! Update: %s", update);
		try {
			updateService.doUpdate(update, request);
		} catch (Exception e) {
			throw new InternalServerException();
		}
	}

	@ResponseStatus(value=HttpStatus.BAD_REQUEST, reason="Specifying graph separately not supported. Specify graph within SPARQL query string")
	public class BadRequestException extends RuntimeException {}

	@ResponseStatus(value=HttpStatus.INTERNAL_SERVER_ERROR, reason="Error occured while processing")
	public class InternalServerException extends RuntimeException {}
}
