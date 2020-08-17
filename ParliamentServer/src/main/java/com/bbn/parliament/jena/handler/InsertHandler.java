// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.handler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Supplier;

import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpEntity;
import org.springframework.web.multipart.MultipartFile;

import com.bbn.parliament.jena.bridge.ConcurrentRequestController;
import com.bbn.parliament.jena.bridge.ConcurrentRequestLock;
import com.bbn.parliament.jena.bridge.tracker.TrackableException;
import com.bbn.parliament.jena.bridge.tracker.TrackableInsert;
import com.bbn.parliament.jena.bridge.tracker.Tracker;
import com.bbn.parliament.jena.exception.DataFormatException;
import com.bbn.parliament.jena.exception.MissingGraphException;
import com.bbn.parliament.jena.exception.QueryExecutionException;

public class InsertHandler {
	@SuppressWarnings("static-method")
	public void handleRequest(String contentType, String graphURI, String remoteAddr,
			HttpEntity<byte[]> requestEntity, HttpServletResponse resp)
			throws IOException, QueryExecutionException {
		long numStatements = handleRequest(resp, graphURI, contentType, null, remoteAddr,
			() -> new ByteArrayInputStream(requestEntity.getBody()));
		sendSuccess(numStatements, resp);
	}

	@SuppressWarnings("static-method")
	public void handleFileRequest(String contentType, String graphURI, String remoteAddr,
			MultipartFile[] files, HttpServletResponse resp)
			throws IOException, QueryExecutionException {
		long numStatements = 0;
		for (MultipartFile file : files) {
			numStatements += handleRequest(resp, graphURI, file.getContentType(),
				file.getOriginalFilename(), remoteAddr, () -> getMultipartInputStream(file));
		}
		sendSuccess(numStatements, resp);
	}

	private static InputStream getMultipartInputStream(MultipartFile file) {
		try {
			return file.getInputStream();
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	// Returns the number of statements inserted
	private static long handleRequest(HttpServletResponse resp, String graphName,
			String dataFormat, String filename, String remoteAddr,
			Supplier<InputStream> strmSupplier) throws QueryExecutionException {
		Objects.requireNonNull(strmSupplier, "strmSupplier");

		final String verifyString = "yes";
		final String importString = "no";

		Inserter inserter = new Inserter(graphName, strmSupplier, dataFormat, null,
			verifyString, importString, filename);
		TrackableInsert ti = Tracker.getInstance().createInsert(inserter, remoteAddr);

		try (ConcurrentRequestLock lock = ConcurrentRequestController.getWriteLock()) {
			ti.run();
			return ti.getInserter().getNumStatements();
		} catch (TrackableException | DataFormatException | MissingGraphException | IOException ex) {
			throw new QueryExecutionException("Error while executing insert", ex);
		}
	}

	private static void sendSuccess(long numStatements, HttpServletResponse resp)
			throws IOException {
		sendSuccess(resp, "Inserted %1$d statements.", numStatements);
	}

	/** Sends an <tt>OK</tt> response with the supplied message. */
	private static void sendSuccess(HttpServletResponse resp, String format, Object... args) throws IOException {
		final int status = HttpServletResponse.SC_OK;
		final String charSet = StandardCharsets.UTF_8.name();
		resp.setStatus(status);
		resp.setCharacterEncoding(charSet);
		String msg = String.format(format, args);
		try (PrintWriter wtr = resp.getWriter()) {
			wtr.format("<html>%n"
				+ "<head>%n"
				+ "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=%3$s\"/>%n"
				+ "<title>OK %1$d %2$s</title>%n"
				+ "</head>%n"
				+ "<body>%n"
				+ "<h2>HTTP OK: %1$d</h2>%n"
				+ "<pre>%2$s</pre>%n"
				+ "</body>%n"
				+ "</html>%n",
				status, msg, charSet);
		}
	}
}
