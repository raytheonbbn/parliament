package com.bbn.parliament.server.service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.bbn.parliament.server.exception.DataFormatException;
import com.bbn.parliament.server.exception.MissingGraphException;
import com.bbn.parliament.server.exception.NoAcceptableException;
import com.bbn.parliament.server.exception.QueryExecutionException;
import com.bbn.parliament.server.exception.TrackableException;
import com.bbn.parliament.server.graph.ModelManager;
import com.bbn.parliament.server.handler.GraphExportHandler;
import com.bbn.parliament.server.handler.InsertHandler;
import com.bbn.parliament.server.handler.UpdateHandler;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class GraphStoreService {
	private static final String INSERT_RESPONSE_BODY = """
		<html>
			<head>
				<meta http-equiv="Content-Type" content="text/html; charset=%4$s"/>
				<title>%2$s (%1$d) Inserted %3$d statements</title>
			</head>
			<body>
				<h2>HTTP %2$s: %1$d</h2>
				<p>Inserted %3$d statements.</p>
			</body>
		</html>
		""";

	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(GraphStoreService.class);

	@SuppressWarnings("static-method")
	public ResponseEntity<StreamingResponseBody> doGetGraph(String graphUri, String format,
		HttpHeaders headers, HttpServletRequest request) {
		AcceptableMediaType mediaType = chooseMediaType(format, headers);
		String serverName = ServiceUtil.getRequestor(request);
		GraphExportHandler handler = new GraphExportHandler(mediaType, serverName, graphUri);
		return ResponseEntity.status(HttpStatus.OK)
			.contentType(ServiceUtil.getSpringMediaType(mediaType))
			.header("Content-Disposition", handler.getContentDisposition())
			.body(handler::handleRequest);
	}

	@SuppressWarnings("static-method")
	public void createDropGraph(GraphOperation operation, String graphUri,
			DropGraphOption dropOpt, HttpServletRequest request)
			throws QueryExecutionException, MissingGraphException {
		if (operation == GraphOperation.CREATE && StringUtils.isBlank(graphUri)) {
			throw new IllegalArgumentException("Missing graph uri argument");
		}

		if (operation == GraphOperation.DROP					// we are deleting a graph
			&& graphUri != null										// and it's not the default
			&& !ModelManager.inst().containsModel(graphUri)	// and it does not exist
			&& dropOpt == DropGraphOption.NOISY) {				// and we're flagging missing graphs
			throw new MissingGraphException("Named graph <%1$s> does not exist", graphUri);
		}

		var graphSpec = StringUtils.isBlank(graphUri)
			? "default"
			: "graph <%1$s>".formatted(graphUri);
		var updateStmt = "%2$s%3$s %1$s ;".formatted(graphSpec,
			(operation == GraphOperation.CREATE) ? "create" : "drop",
			(dropOpt == DropGraphOption.SILENT) ? " silent" : "");
		var serverName = ServiceUtil.getRequestor(request);
		new UpdateHandler().handleRequest(updateStmt, serverName);
	}

	public ResponseEntity<String> doGraphInsertOrReplace(String graphUri, HttpMethod method,
		String contentType, HttpServletRequest request) throws TrackableException,
		IOException, DataFormatException, MissingGraphException, QueryExecutionException {

		var graphExists = StringUtils.isBlank(graphUri)
			|| ModelManager.inst().containsModel(graphUri);
		String autoGraphUri = null;
		if (method == HttpMethod.POST) {
			if (graphUri != null && graphUri.equals(request.getRequestURL().toString())) {
				autoGraphUri = ModelManager.inst().createAutoNamedGraph();
			}
		} else if (method == HttpMethod.PUT) {
			createDropGraph(GraphOperation.DROP, graphUri, DropGraphOption.SILENT, request);
			if (!StringUtils.isBlank(graphUri)) {
				createDropGraph(GraphOperation.CREATE, graphUri, DropGraphOption.SILENT, request);
			}
		} else {
			throw new IllegalArgumentException("method must be POST or PUT");
		}

		try (var requestBody = new FileBackedRequestBody(request)) {
			var serverName = ServiceUtil.getRequestor(request);
			long numStatements = new InsertHandler().handleRequest(graphUri, contentType,
				null, serverName, () -> requestBody.openBody());

			if (autoGraphUri != null || (method == HttpMethod.PUT && graphExists)) {
				return createInsertResponse(HttpStatus.CREATED, numStatements, autoGraphUri);
			} else {
				var status = (numStatements > 0) ? HttpStatus.OK : HttpStatus.NO_CONTENT;
				return createInsertResponse(status, numStatements, null);
			}
		}
	}

	public ResponseEntity<String> doGraphInsertOrReplace(String graphUri, HttpMethod method,
		HttpServletRequest request, MultipartFile[] files) throws TrackableException,
		IOException, DataFormatException, MissingGraphException, QueryExecutionException {

		var graphExists = StringUtils.isBlank(graphUri)
			|| ModelManager.inst().containsModel(graphUri);
		String autoGraphUri = null;
		if (method == HttpMethod.POST) {
			if (graphUri != null && graphUri.equals(request.getRequestURL().toString())) {
				autoGraphUri = ModelManager.inst().createAutoNamedGraph();
			}
		} else if (method == HttpMethod.PUT) {
			createDropGraph(GraphOperation.DROP, graphUri, DropGraphOption.SILENT, request);
			if (!StringUtils.isBlank(graphUri)) {
				createDropGraph(GraphOperation.CREATE, graphUri, DropGraphOption.SILENT, request);
			}
		} else {
			throw new IllegalArgumentException("method must be POST or PUT");
		}

		var serverName = ServiceUtil.getRequestor(request);
		long numStatements = new InsertHandler().handleRequest(graphUri, files, serverName);

		if (autoGraphUri != null || (method == HttpMethod.PUT && graphExists)) {
			return createInsertResponse(HttpStatus.CREATED, numStatements, autoGraphUri);
		} else {
			var status = (numStatements > 0) ? HttpStatus.OK : HttpStatus.NO_CONTENT;
			return createInsertResponse(status, numStatements, null);
		}
	}

	/** Sends an <tt>OK</tt> response with the number of inserted statements in the message. */
	private static ResponseEntity<String> createInsertResponse(HttpStatus status,
			long numStatements, String autoGraphUri) {
		try {
			var charSet = StandardCharsets.UTF_8;
			var responseContentType = new MediaType(MediaType.TEXT_HTML, charSet);
			String body = INSERT_RESPONSE_BODY.formatted(
				status.value(), status.name(), numStatements, charSet.name());
			var builder = ResponseEntity.status(status)
				.contentType(responseContentType);
			if (StringUtils.isNotBlank(autoGraphUri)) {
				builder = builder.location(new URI(autoGraphUri));
			}
			return builder.body(body);
		} catch (URISyntaxException ex) {
			throw new IllegalStateException("ModelManager.createAutoNamedGraph generated an illegal URI", ex);
		}
	}

	private static AcceptableMediaType chooseMediaType(String format, HttpHeaders headers) {
		List<AcceptableMediaType> acceptList = ServiceUtil.getAcceptList(format, headers);
		return acceptList.stream()
			.filter(mt -> mt.getCategory() == QueryResultCategory.RDF)
			.findFirst()
			.orElseThrow(() -> new NoAcceptableException(QueryResultCategory.RESULT_SET));
	}
}
