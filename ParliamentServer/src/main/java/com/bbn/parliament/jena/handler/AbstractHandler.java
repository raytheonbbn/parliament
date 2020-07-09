// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.handler;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import com.bbn.parliament.jena.graph.KbGraphStore;
import com.bbn.parliament.jena.bridge.servlet.ServletErrorResponseException;

/**
 * AbstractHandler that provides some common methods for subclasses.
 *
 * @author sallen
 */
public abstract class AbstractHandler {
	protected static final String MASTER_GRAPH_BASENAME = KbGraphStore.MASTER_GRAPH_DIR;
	protected static final String OLD_MASTER_GRAPH_BASENAME = KbGraphStore.OLD_MASTER_GRAPH_DIR;
	protected static final String DEFAULT_GRAPH_BASENAME = "Default Graph";

	/** Get request parameters (x-www-form-urlencoded) */
	public abstract void handleFormURLEncodedRequest(HttpServletRequest req,
		HttpServletResponse resp) throws IOException, ServletErrorResponseException;

	/** Get request parameters (multipart/form-data) */
	public abstract void handleMultipartFormRequest(HttpServletRequest req,
		HttpServletResponse resp) throws IOException, ServletErrorResponseException;

	/** Gets the logger */
	protected abstract Logger getLog();

	/** Sends an <tt>OK</tt> response with the supplied message. */
	protected static void sendSuccess(String msg, HttpServletResponse resp) throws IOException {
		final int status = HttpServletResponse.SC_OK;
		resp.setStatus(status);
		try (PrintWriter wtr = resp.getWriter()) {
			wtr.format("<html>%n"
				+ "<head>%n"
				+ "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=ISO-8859-1\"/>%n"
				+ "<title>OK %1$d %2$s</title>%n"
				+ "</head>%n"
				+ "<body>%n"
				+ "<h2>HTTP OK: %1$d</h2>%n"
				+ "<pre>%2$s</pre>%n"
				+ "</body>%n"
				+ "</html>%n",
				status, msg);
		}
	}
}
