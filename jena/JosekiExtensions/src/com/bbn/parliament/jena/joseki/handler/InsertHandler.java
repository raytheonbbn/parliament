// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.joseki.handler;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jena.joseki.bridge.ActionRouter;
import com.bbn.parliament.jena.joseki.bridge.servlet.ServletErrorResponseException;
import com.bbn.parliament.jena.joseki.bridge.tracker.TrackableException;
import com.bbn.parliament.jena.joseki.bridge.tracker.TrackableInsert;
import com.bbn.parliament.jena.joseki.bridge.tracker.Tracker;
import com.bbn.parliament.jena.joseki.bridge.util.HttpServerUtil;
import com.bbn.parliament.jena.joseki.bridge.util.LogUtil;
import com.bbn.parliament.jena.joseki.handler.Inserter.IInputStreamProvider;

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

	public void handleRequest(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletErrorResponseException {
		if (HttpServerUtil.isMultipartContent(req)) {
			if (_log.isInfoEnabled()) {
				_log.info("Insert operation (multipart/form-data) from {}",
					req.getRemoteAddr());
			}
			handleMultipartFormRequest(req, resp);
		} else {
			if (_log.isInfoEnabled()) {
				_log.info("Insert operation (x-www-form-urlencoded) from {}",
					req.getRemoteAddr());
			}
			handleFormURLEncodedRequest(req, resp);
		}
	}

	/** Get request parameters (x-www-form-urlencoded) */
	@Override
	public void handleFormURLEncodedRequest(HttpServletRequest req,
		HttpServletResponse resp) throws IOException, ServletErrorResponseException {
		String dataFormat = HttpServerUtil.getParameter(req, P_FORMAT);
		String graphName = HttpServerUtil.getParameter(req, P_GRAPH, "");
		String base = HttpServerUtil.getParameter(req, P_BASE);
		String verifyString = HttpServerUtil.getParameter(req, P_VERIFY, "yes");
		String importString = HttpServerUtil.getParameter(req, P_IMPORT, "no");
		final String statements = HttpServerUtil.getParameter(req, P_STATEMENTS);

		IInputStreamProvider strmPrvdr = null;
		if (null != statements) {
			strmPrvdr = new IInputStreamProvider() {
				@Override
				public InputStream getInputStream() throws IOException {
					return new ByteArrayInputStream(statements.getBytes());
				}
			};
		}

		handleRequest(req, resp, graphName, strmPrvdr, dataFormat, base, verifyString,
			importString, null);
	}

	/** Get request parameters (multipart/form-data) */
	@Override
	public void handleMultipartFormRequest(HttpServletRequest req,
		HttpServletResponse resp) throws IOException, ServletErrorResponseException {
		Map<String, FileItem> fileItemMap = HttpServerUtil.parseMultipartFormRequest(req);
		try {
			String dataFormat = HttpServerUtil.getParameter(fileItemMap, P_FORMAT);
			String graphName = HttpServerUtil.getParameter(fileItemMap, P_GRAPH, "");
			String base = HttpServerUtil.getParameter(fileItemMap, P_BASE);
			String verifyString = HttpServerUtil.getParameter(fileItemMap, P_VERIFY, "yes");
			String importString = HttpServerUtil.getParameter(fileItemMap, P_IMPORT, "no");

			IInputStreamProvider strmPrvdr = null;
			final FileItem fileItem = fileItemMap.get(P_STATEMENTS);
			if (null != fileItem) {
				strmPrvdr = new IInputStreamProvider() {
					@Override
					public InputStream getInputStream() throws IOException {
						return new BufferedInputStream(fileItem.getInputStream());
					}
				};
				String filename = fileItem.getName();
				handleRequest(req, resp, graphName, strmPrvdr, dataFormat, base, verifyString,
					importString, filename);
			}
		} finally {
			// Remove any temporary files we've created
			if (null != fileItemMap) {
				for (FileItem fileItem : fileItemMap.values()) {
					fileItem.delete();
				}
			}
		}
	}

	@SuppressWarnings("static-method")
	protected void handleRequest(HttpServletRequest req, HttpServletResponse resp,
		String graphName, IInputStreamProvider strmPrvdr, String dataFormat,
		String base, String verifyString, String importString, String filename)
			throws IOException, ServletErrorResponseException {

		if (strmPrvdr == null) {
			throw new ServletErrorResponseException("RDF data is missing");
		}

		long numStatements = -1;

		Inserter inserter = new Inserter(graphName, strmPrvdr, dataFormat, base, verifyString, importString, filename);
		TrackableInsert ti = Tracker.getInstance().createInsert(inserter, req.getRemoteAddr());

		ActionRouter.getWriteLock();
		try {
			ti.run();
			numStatements = ti.getInserter().getNumStatements();
		}
		catch(TrackableException e) {
			if (e.getCause() instanceof ServletErrorResponseException) {
				throw (ServletErrorResponseException)e.getCause();
			} else {
				throw new ServletErrorResponseException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e,
					"Error while running insert\n\n" + LogUtil.getExceptionInfo(e));
			}
		} finally {
			ActionRouter.releaseWriteLock();
		}

		String msg = "Insert operation successful.";
		if (numStatements == 1) {
			msg = "Insert operation successful.  1 statement added.";
		} else if (numStatements >= 0) {
			msg = "Insert operation successful.  %1$d statements added.".formatted(numStatements);
		}
		sendSuccess(msg, resp);
	}
}
