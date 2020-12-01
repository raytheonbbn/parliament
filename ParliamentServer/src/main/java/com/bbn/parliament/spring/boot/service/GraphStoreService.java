package com.bbn.parliament.spring.boot.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.bbn.parliament.jena.bridge.tracker.TrackableException;
import com.bbn.parliament.jena.exception.DataFormatException;
import com.bbn.parliament.jena.exception.MissingGraphException;
import com.bbn.parliament.jena.exception.NoAcceptableException;
import com.bbn.parliament.jena.exception.QueryExecutionException;
import com.bbn.parliament.jena.graph.ModelManager;
import com.bbn.parliament.jena.handler.GraphExportHandler;
import com.bbn.parliament.jena.handler.InsertHandler;
import com.bbn.parliament.jena.handler.UpdateHandler;

@Service
public class GraphStoreService {
	@SuppressWarnings("static-method")
	public ResponseEntity<StreamingResponseBody> doGetGraph(String graphUri, String format,
		HttpHeaders headers, HttpServletRequest request) {

		AcceptableMediaType contentType = chooseMediaType(format, headers);
		String serverName = ServiceUtil.getRequestor(headers, request);
		GraphExportHandler handler = new GraphExportHandler(contentType, serverName, graphUri);
		return ResponseEntity.status(HttpStatus.OK)
			.contentType(ServiceUtil.mediaTypeFromString(contentType.getPrimaryMediaType()))
			.header("Content-Disposition", handler.getContentDisposition())
			.body(handler::handleRequest);
	}

	@SuppressWarnings("static-method")
	public void doDeleteGraph(String graphUri, HttpHeaders headers, HttpServletRequest request,
		DropGraphOption dropGraphOption)
		throws QueryExecutionException, MissingGraphException {

		String updateStmt = null;
		String option = (dropGraphOption == DropGraphOption.SILENT)
			? "SILENT" : "";
		if (graphUri == null || graphUri.isEmpty()) {
			updateStmt = "DROP %1$s DEFAULT ;".formatted(option);
		} else if (ModelManager.inst().containsModel(graphUri)) {
			updateStmt = "DROP %1$s GRAPH <%2s> ;".formatted(option, graphUri);
		} else {
			throw new MissingGraphException("Named graph <%1$s> does not exist".formatted(graphUri));
		}

		if (updateStmt != null) {
			String serverName = ServiceUtil.getRequestor(headers, request);
			new UpdateHandler().handleRequest(updateStmt, serverName);
		}
	}

	@SuppressWarnings("static-method")
	public ResponseEntity<String> doInsertIntoGraph(String contentType, String graphUri,
		HttpHeaders headers, HttpServletRequest request, HttpEntity<byte[]> requestEntity)
		throws TrackableException, DataFormatException, MissingGraphException, IOException {

		String serverName = ServiceUtil.getRequestor(headers, request);
		long numStatements = new InsertHandler().handleRequest(graphUri, contentType, null,
			serverName, () -> new ByteArrayInputStream(requestEntity.getBody()));

		return createInsertResponse(numStatements);
	}

	@SuppressWarnings("static-method")
	public ResponseEntity<String> doInsertIntoGraph(String graphUri, HttpHeaders headers,
		HttpServletRequest request, MultipartFile[] files)
		throws TrackableException, DataFormatException, MissingGraphException, IOException {

		String serverName = ServiceUtil.getRequestor(headers, request);
		InsertHandler handler = new InsertHandler();
		long numStatements = 0;
		for (MultipartFile file : files) {
			numStatements += handler.handleRequest(graphUri, file.getContentType(),
				file.getOriginalFilename(), serverName,
				() -> getMultipartInputStream(file));
		}
		return createInsertResponse(numStatements);
	}

	public ResponseEntity<String> doReplaceGraph(String contentType, String graphUri,
		HttpHeaders headers, HttpServletRequest request, HttpEntity<byte[]> requestEntity)
		throws QueryExecutionException, TrackableException, DataFormatException,
		MissingGraphException, IOException {

		doDeleteGraph(graphUri, headers, request, DropGraphOption.SILENT);
		return doInsertIntoGraph(contentType, graphUri, headers, request, requestEntity);
	}

	public ResponseEntity<String> doReplaceGraph(String contentType, String graphUri,
		HttpHeaders headers, HttpServletRequest request, MultipartFile[] files)
		throws QueryExecutionException, TrackableException, DataFormatException,
		MissingGraphException, IOException {

		doDeleteGraph(graphUri, headers, request, DropGraphOption.SILENT);
		return doInsertIntoGraph(graphUri, headers, request, files);
	}

	private static InputStream getMultipartInputStream(MultipartFile file) {
		try {
			return file.getInputStream();
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	/** Sends an <tt>OK</tt> response with the number of inserted statements in the message. */
	private static ResponseEntity<String> createInsertResponse(long numStatements) {
		HttpStatus status = HttpStatus.OK;
		final Charset charSet = StandardCharsets.UTF_8;
		MediaType responseContentType = new MediaType(MediaType.TEXT_HTML, charSet);
		String body = """
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
			""".formatted(status.value(), status.name(), numStatements, charSet.name());
		return ResponseEntity.status(status)
			.contentType(responseContentType)
			.body(body);
	}

	private static AcceptableMediaType chooseMediaType(String format, HttpHeaders headers) {
		List<AcceptableMediaType> acceptList = ServiceUtil.getAcceptList(format, headers);
		return acceptList.stream()
			.filter(mt -> mt.getCategory() == QueryResultCategory.RDF)
			.findFirst()
			.orElseThrow(() -> new NoAcceptableException(QueryResultCategory.RESULT_SET));
	}
}
