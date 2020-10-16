package com.bbn.parliament.spring.boot.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
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

@Component("graphStoreService")
public class GraphStoreService {
	@SuppressWarnings("static-method")
	public ResponseEntity<StreamingResponseBody> doGetGraph(String graphUri, String format,
		HttpHeaders headers) {

		AcceptableMediaType contentType = chooseMediaType(format, headers);
		String serverName = headers.getHost().getHostString();
		GraphExportHandler handler = new GraphExportHandler(contentType, serverName, graphUri);
		return ResponseEntity.status(HttpStatus.OK)
			.contentType(ServiceUtil.mediaTypeFromString(contentType.getPrimaryMediaType()))
			.header("Content-Disposition", handler.getContentDisposition())
			.body(handler::handleRequest);
	}

	@SuppressWarnings("static-method")
	public void doDeleteGraph(String graphUri, HttpHeaders headers, DropGraphOption dropGraphOption)
		throws QueryExecutionException, MissingGraphException {

		String updateStmt = null;
		String option = (dropGraphOption == DropGraphOption.SILENT)
			? "SILENT" : "";
		if (graphUri == null || graphUri.isEmpty()) {
			updateStmt = String.format("DROP %1$s DEFAULT ;", option);
		} else if (ModelManager.inst().containsModel(graphUri)) {
			updateStmt = String.format("DROP %1$s GRAPH <%2s> ;", option, graphUri);
		} else {
			throw new MissingGraphException(String.format(
				"Named graph <%1$s> does not exist", graphUri));
		}

		if (updateStmt != null) {
			new UpdateHandler().handleRequest(updateStmt, headers.getHost().getHostString());
		}
	}

	@SuppressWarnings("static-method")
	public ResponseEntity<String> doInsertIntoGraph(String contentType, String graphUri,
		HttpHeaders headers, HttpEntity<byte[]> requestEntity)
		throws TrackableException, DataFormatException, MissingGraphException, IOException {

		long numStatements = new InsertHandler().handleRequest(graphUri, contentType, null,
			headers.getHost().getHostString(), () -> new ByteArrayInputStream(requestEntity.getBody()));

		return createInsertResponse(numStatements);
	}

	@SuppressWarnings("static-method")
	public ResponseEntity<String> doInsertIntoGraph(String contentType, String graphUri,
		HttpHeaders headers, MultipartFile[] files)
		throws TrackableException, DataFormatException, MissingGraphException, IOException {

		InsertHandler handler = new InsertHandler();
		long numStatements = 0;
		for (MultipartFile file : files) {
			numStatements += handler.handleRequest(graphUri, file.getContentType(),
				file.getOriginalFilename(), headers.getHost().getHostString(),
				() -> getMultipartInputStream(file));
		}
		return createInsertResponse(numStatements);
	}

	public ResponseEntity<String> doReplaceGraph(String contentType, String graphUri,
		HttpHeaders headers, HttpEntity<byte[]> requestEntity)
		throws QueryExecutionException, TrackableException, DataFormatException,
		MissingGraphException, IOException {

		doDeleteGraph(graphUri, headers, DropGraphOption.SILENT);
		return doInsertIntoGraph(contentType, graphUri, headers, requestEntity);
	}

	public ResponseEntity<String> doReplaceGraph(String contentType, String graphUri,
		HttpHeaders headers, MultipartFile[] files)
		throws QueryExecutionException, TrackableException, DataFormatException,
		MissingGraphException, IOException {

		doDeleteGraph(graphUri, headers, DropGraphOption.SILENT);
		return doInsertIntoGraph(contentType, graphUri, headers, files);
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
		String message = String.format("Inserted %1$d statements.", numStatements);
		String body = String.format("<html>%n"
			+ "<head>%n"
			+ "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=%3$s\"/>%n"
			+ "<title>OK %1$d %2$s</title>%n"
			+ "</head>%n"
			+ "<body>%n"
			+ "<h2>HTTP OK: %1$d</h2>%n"
			+ "<p>%2$s</p>%n"
			+ "</body>%n"
			+ "</html>%n",
			status.value(), message, charSet.name());
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
