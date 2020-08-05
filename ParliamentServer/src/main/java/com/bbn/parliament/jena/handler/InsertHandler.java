// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.handler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.web.multipart.MultipartFile;

import com.bbn.parliament.jena.bridge.ActionRouter;
import com.bbn.parliament.jena.bridge.servlet.ServletErrorResponseException;
import com.bbn.parliament.jena.bridge.tracker.TrackableException;
import com.bbn.parliament.jena.bridge.tracker.TrackableInsert;
import com.bbn.parliament.jena.bridge.tracker.Tracker;
import com.bbn.parliament.jena.bridge.util.LogUtil;
import com.bbn.parliament.jena.handler.Inserter.IInputStreamProvider;


/** @author sallen */
public class InsertHandler extends AbstractHandler {
	public static final String P_STATEMENTS = "statements";
	public static final String P_BASE = "base";
	public static final String P_FORMAT = "dataFormat";
	public static final String P_GRAPH = "graph";
	public static final String P_VERIFY = "verifyData";
	public static final String P_IMPORT = "import";

	// Use one logger.
	private static Logger _log = LoggerFactory.getLogger(InsertHandler.class);

	@Override
	protected Logger getLog() {
		return _log;
	}

	public void handleRequest(String contentType, String graphURI, String remoteAddr, HttpEntity<byte[]> requestEntity, HttpServletResponse resp)
		throws IOException, ServletErrorResponseException {
		String verifyString = "yes";
		String importString = "no";
		if (graphURI == null) {
			graphURI = "";
		}

		IInputStreamProvider strmPrvdr = null;
		strmPrvdr = new IInputStreamProvider() {
			@Override
			public InputStream getInputStream() throws IOException {
				return new ByteArrayInputStream(requestEntity.getBody());
			}
		};

		long numStatements = handleRequest(resp, graphURI, strmPrvdr, contentType, null, verifyString, importString, null, remoteAddr);
		sendSuccess(numStatements, resp);
	}


	public void handleFileRequest(String contentType, String graphURI, String remoteAddr, MultipartFile[] files, HttpServletResponse resp)
			throws IOException, ServletErrorResponseException {
			String verifyString = "yes";
			String importString = "no";
			long numStatements = 0;

			if (graphURI == null) {
				graphURI = "";
			}

			for (int i = 0; i < files.length; i++) {
				MultipartFile file = files[i];
				String filename = file.getOriginalFilename();
				String dataFormat = file.getContentType();

				IInputStreamProvider strmPrvdr = new IInputStreamProvider() {
					@Override
					public InputStream getInputStream() throws IOException {
						return file.getInputStream();
					}
				};
				numStatements += handleRequest(resp, graphURI, strmPrvdr, dataFormat, null, verifyString, importString, filename, remoteAddr);
			}
			sendSuccess(numStatements, resp);
		}

	@SuppressWarnings("static-method")
	protected long handleRequest(HttpServletResponse resp,
		String graphName, IInputStreamProvider strmPrvdr, String dataFormat,
		String base, String verifyString, String importString, String filename, String remoteAddr)
			throws IOException, ServletErrorResponseException {

		if (strmPrvdr == null) {
			throw new ServletErrorResponseException("RDF data is missing");
		}

		long numStatements = -1;

		Inserter inserter = new Inserter(graphName, strmPrvdr, dataFormat, base, verifyString, importString, filename);
		TrackableInsert ti = Tracker.getInstance().createInsert(inserter, remoteAddr);

		ActionRouter.getWriteLock();
		try {
			ti.run();
			numStatements = ti.getInserter().getNumStatements();
		}
		catch(TrackableException e) {
			if (e.getCause() instanceof ServletErrorResponseException) {
				throw (ServletErrorResponseException)e.getCause();
			} else {
				throw new IOException("Error while running insert\n\n" + LogUtil.getExceptionInfo(e), e);
			}
		} finally {
			ActionRouter.releaseWriteLock();
		}

		return numStatements;
	}

	protected void sendSuccess(long numStatements, HttpServletResponse resp) throws IOException {
		String msg = "Insert operation successful.";
		if (numStatements == 1) {
			msg = "Insert operation successful.  1 statement added.";
		}
		else if (numStatements >= 0) {
			msg = String.format(
				"Insert operation successful.  %1$d statements added.",
				numStatements);
		}
		sendSuccess(msg, resp);
	}
}
