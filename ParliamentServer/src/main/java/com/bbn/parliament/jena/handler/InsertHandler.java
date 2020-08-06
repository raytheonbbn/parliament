// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.handler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.function.Supplier;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.web.multipart.MultipartFile;

import com.bbn.parliament.jena.bridge.ActionRouter;
import com.bbn.parliament.jena.bridge.tracker.TrackableException;
import com.bbn.parliament.jena.bridge.tracker.TrackableInsert;
import com.bbn.parliament.jena.bridge.tracker.Tracker;
import com.bbn.parliament.jena.exception.DataFormatException;
import com.bbn.parliament.jena.exception.MissingGraphException;
import com.bbn.parliament.jena.exception.QueryExecutionException;

public class InsertHandler extends AbstractHandler {
	// Use one logger.
	private static Logger LOG = LoggerFactory.getLogger(InsertHandler.class);

	@Override
	protected Logger getLog() {
		return LOG;
	}

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

	private static long handleRequest(HttpServletResponse resp, String graphName,
			String dataFormat, String filename, String remoteAddr,
			Supplier<InputStream> strmSupplier) throws QueryExecutionException {
		Objects.requireNonNull(strmSupplier, "strmSupplier");

		final String verifyString = "yes";
		final String importString = "no";
		long numStatements = -1;

		Inserter inserter = new Inserter(graphName, strmSupplier, dataFormat, null,
			verifyString, importString, filename);
		TrackableInsert ti = Tracker.getInstance().createInsert(inserter, remoteAddr);

		ActionRouter.getWriteLock();
		try {
			ti.run();
			numStatements = ti.getInserter().getNumStatements();
		} catch (TrackableException | DataFormatException | MissingGraphException
				| IOException ex) {
			throw new QueryExecutionException("Error while executing insert", ex);
		} finally {
			ActionRouter.releaseWriteLock();
		}

		return numStatements;
	}

	private static void sendSuccess(long numStatements, HttpServletResponse resp)
			throws IOException {
		sendSuccess(resp, "Inserted %1$d statements.", numStatements);
	}
}
