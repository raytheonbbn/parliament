//FILE ADDED BY CODEX CODING AGENT
package com.bbn.parliament.server.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.bbn.parliament.server.exception.BadRequestException;
import com.bbn.parliament.server.exception.DataFormatException;
import com.bbn.parliament.server.exception.MissingGraphException;
import com.bbn.parliament.server.exception.QueryExecutionException;
import com.bbn.parliament.server.exception.TrackableException;
import com.bbn.parliament.server.graph.ModelManager;
import com.bbn.parliament.server.handler.Inserter;
import com.bbn.parliament.server.handler.VerifyOption;
import com.bbn.parliament.server.service.GraphStoreService;
import com.bbn.parliament.server.service.QueryService;
import com.bbn.parliament.server.service.TrackerService;
import com.bbn.parliament.server.service.UpdateService;
import com.bbn.parliament.server.tracker.TrackableInsert;
import com.bbn.parliament.server.tracker.Tracker;
import com.bbn.parliament.server.util.ConcurrentRequestController;
import com.bbn.parliament.server.util.ConcurrentRequestLock;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class LegacyUiController {
	private static final String URL_ENCODED = MediaType.APPLICATION_FORM_URLENCODED_VALUE;
	private static final String MULTIPART = MediaType.MULTIPART_FORM_DATA_VALUE;

	private final QueryService queryService;
	private final UpdateService updateService;
	private final GraphStoreService graphStoreService;
	private final TrackerService trackerService;

	public LegacyUiController(QueryService queryService, UpdateService updateService,
			GraphStoreService graphStoreService, TrackerService trackerService) {
		this.queryService = queryService;
		this.updateService = updateService;
		this.graphStoreService = graphStoreService;
		this.trackerService = trackerService;
	}

	@GetMapping(value = "/sparql", params = "query")
	public ResponseEntity<StreamingResponseBody> sparqlGet(
			@RequestParam("query") String query,
			@RequestParam(value = "format", required = false) String format,
			@RequestParam(value = "output", required = false) String output,
			@RequestParam(value = "display", required = false) String display,
			@RequestHeader HttpHeaders headers,
			HttpServletRequest request) throws BadRequestException {
		return queryService.doQuery(query, null, null, legacyQueryFormat(format, output, display),
			headers, request);
	}

	@PostMapping(value = "/sparql", consumes = URL_ENCODED, params = "query")
	public ResponseEntity<StreamingResponseBody> sparqlPost(
			@RequestParam("query") String query,
			@RequestParam(value = "format", required = false) String format,
			@RequestParam(value = "output", required = false) String output,
			@RequestParam(value = "display", required = false) String display,
			@RequestHeader HttpHeaders headers,
			HttpServletRequest request) throws BadRequestException {
		return queryService.doQuery(query, null, null, legacyQueryFormat(format, output, display),
			headers, request);
	}

	@PostMapping(value = "/sparql", consumes = URL_ENCODED, params = "update")
	public ResponseEntity<String> sparqlUpdate(
			@RequestParam("update") String update,
			HttpServletRequest request) throws BadRequestException, QueryExecutionException {
		updateService.doUpdate(update, null, null, request);
		return html("Update executed.");
	}

	@PostMapping(value = "/bulk/export", consumes = URL_ENCODED)
	public ResponseEntity<StreamingResponseBody> legacyExport(
			@RequestParam(value = "graph", required = false, defaultValue = "") String graphUri,
			@RequestParam(value = "dataFormat", required = false, defaultValue = "RDF/XML") String dataFormat,
			@RequestParam(value = "exportAll", required = false, defaultValue = "no") String exportAll,
			HttpServletRequest request) throws DataFormatException {
		return graphStoreService.doLegacyExport(graphUri, dataFormat, exportAll, request);
	}

	@PostMapping(value = "/bulk/flush")
	public ResponseEntity<String> flush() {
		ModelManager.inst().flushKb();
		return html("Data flushed.");
	}

	@PostMapping(value = "/bulk/insert", consumes = URL_ENCODED)
	public ResponseEntity<String> insertText(
			@RequestParam(value = "graph", required = false, defaultValue = "") String graphUri,
			@RequestParam(value = "dataFormat", required = false, defaultValue = "TURTLE") String dataFormat,
			@RequestParam(value = "statements", required = false, defaultValue = "") String statements,
			HttpServletRequest request)
			throws TrackableException, DataFormatException, MissingGraphException, IOException {
		String requestor = getRequestor(request);
		Inserter inserter = Inserter.newGraphInserter(blankToNull(graphUri),
			legacyDataFormat(dataFormat), "statements", VerifyOption.VERIFY, null,
			() -> new java.io.ByteArrayInputStream(statements.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
		long count = runInsert(List.of(inserter), requestor);
		return html("Inserted %d statements.".formatted(count));
	}

	@PostMapping(value = "/bulk/insert", consumes = MULTIPART)
	public ResponseEntity<String> insertMultipart(
			@RequestParam(value = "graph", required = false, defaultValue = "") String graphUri,
			@RequestParam(value = "dataFormat", required = false, defaultValue = "AUTO") String dataFormat,
			@RequestParam(value = "import", required = false, defaultValue = "no") String importRepository,
			@RequestPart(value = "statements", required = false) MultipartFile[] files,
			HttpServletRequest request)
			throws TrackableException, DataFormatException, MissingGraphException, IOException {
		if (files == null || files.length == 0) {
			return ResponseEntity.badRequest().body("No upload was supplied.");
		}

		List<Inserter> inserters = new ArrayList<>(files.length);
		if ("yes".equalsIgnoreCase(importRepository)) {
			inserters.add(Inserter.newRepositoryInserter(null, () -> getMultipartInputStream(files[0])));
		} else {
			for (MultipartFile file : files) {
				inserters.add(Inserter.newGraphInserter(blankToNull(graphUri),
					legacyDataFormat(dataFormat), file.getOriginalFilename(),
					VerifyOption.VERIFY, null, () -> getMultipartInputStream(file)));
			}
		}

		long count = runInsert(inserters, getRequestor(request));
		return html("Inserted %d statements.".formatted(count));
	}

	@ResponseBody
	@GetMapping(value = "/tracker", produces = MediaType.APPLICATION_JSON_VALUE)
	public StreamingResponseBody getTrackables() {
		return trackerService.getTrackables();
	}

	@PostMapping(value = "/tracker", params = "id")
	public ResponseEntity<String> cancelTrackable(@RequestParam("id") String id)
			throws TrackableException {
		trackerService.cancelTrackable(id);
		return html("Cancelled query %s.".formatted(id));
	}

	private static long runInsert(List<Inserter> inserters, String requestor)
			throws TrackableException, DataFormatException, MissingGraphException, IOException {
		TrackableInsert trackable = Tracker.getInstance().createInsert(inserters, requestor);
		try (ConcurrentRequestLock lock = ConcurrentRequestController.getWriteLock()) {
			@SuppressWarnings("unused") int intentionallyUnused = lock.hashCode();
			trackable.run();
			return trackable.getNumStatements();
		}
	}

	private static String legacyQueryFormat(String format, String output, String display) {
		if (StringUtils.isNotBlank(format)) {
			return format;
		}
		if (StringUtils.isNotBlank(output)) {
			return output;
		}
		if ("csv".equalsIgnoreCase(display) || "json".equalsIgnoreCase(display)
			|| "xml".equalsIgnoreCase(display)) {
			return display;
		}
		return "xml";
	}

	private static String legacyDataFormat(String dataFormat) {
		if (StringUtils.isBlank(dataFormat) || "auto".equalsIgnoreCase(dataFormat)) {
			return "auto";
		}
		return switch (dataFormat.strip().toUpperCase(Locale.ROOT)) {
			case "TURTLE" -> "text/turtle";
			case "N3" -> "text/n3";
			case "N-TRIPLES", "NTRIPLES" -> "application/n-triples";
			case "RDF/XML", "RDFXML" -> "application/rdf+xml";
			default -> dataFormat;
		};
	}

	private static String blankToNull(String value) {
		return StringUtils.isBlank(value) ? null : value;
	}

	private static InputStream getMultipartInputStream(MultipartFile file) {
		try {
			return file.getInputStream();
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private static String getRequestor(HttpServletRequest request) {
		var host = request.getHeader(HttpHeaders.HOST);
		if (host == null) {
			host = request.getRemoteHost();
		}
		int port = request.getRemotePort();
		String user = request.getRemoteUser();
		return StringUtils.isBlank(user)
			? "%s:%d".formatted(host, port)
			: "%s:%d (%s)".formatted(host, port, user);
	}

	private static ResponseEntity<String> html(String message) {
		return ResponseEntity.status(HttpStatus.OK)
			.contentType(MediaType.TEXT_HTML)
			.body("""
				<!doctype html>
				<html lang="en">
				<head><meta charset="utf-8"><title>Parliament</title></head>
				<body><p>%s</p><p><a href="/">Back to Parliament</a></p></body>
				</html>
				""".formatted(message));
	}
}
