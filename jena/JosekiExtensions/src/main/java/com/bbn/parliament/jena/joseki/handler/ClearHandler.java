// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.joseki.handler;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jena.joseki.bridge.servlet.ServletErrorResponseException;
import com.bbn.parliament.jena.joseki.bridge.util.HttpServerUtil;
import com.bbn.parliament.jena.joseki.graph.ModelManager;

/** @author sallen */
public class ClearHandler extends AbstractHandler {
	private static final String P_CLEAR_ALL = "clearAll";
	private static final String P_GRAPH = "graph";
	private static final String P_PERFORM_CLEAR = "performClear";

	private static final Logger LOG = LoggerFactory.getLogger(ClearHandler.class);

	/*
	 * (non-Javadoc)
	 * @see com.bbn.parliament.jena.joseki.josekibridge.AbstractHandler#getLog()
	 */
	@Override
	protected Logger getLog() {
		return LOG;
	}

	/*
	 * (non-Javadoc)
	 * @see com.bbn.parliament.jena.joseki.josekibridge.AbstractHandler#handleFormURLEncodedRequest(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void handleFormURLEncodedRequest(HttpServletRequest req,
		HttpServletResponse resp) throws ServletErrorResponseException, IOException {
		String clearAll = HttpServerUtil.getParameter(req, P_CLEAR_ALL, "no");
		String graphName = HttpServerUtil.getParameter(req, P_GRAPH, "");
		String performClear = HttpServerUtil.getParameter(req, P_PERFORM_CLEAR);

		handleRequest(req, resp, clearAll, graphName, performClear);
	}

	/*
	 * (non-Javadoc)
	 * @see com.bbn.parliament.jena.joseki.josekibridge.AbstractHandler#handleMultipartFormRequest(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void handleMultipartFormRequest(HttpServletRequest req,
		HttpServletResponse resp) throws ServletErrorResponseException {
		throw new ServletErrorResponseException(
			"'multipart/form data' requests are not supported by this handler.");
	}

	@SuppressWarnings("static-method")
	protected void handleRequest(HttpServletRequest req, HttpServletResponse resp,
		String clearAllStr, String graphName, String performClearStr)
			throws ServletErrorResponseException, IOException {
		// Default to false, and only set to true if "yes" is passed
		boolean performClear = "yes".equalsIgnoreCase(performClearStr);
		if (!performClear) {
			throw new ServletErrorResponseException("""
				Set the 'performClear' parameter to 'yes' in order to verify that you \
				want to perform this operation.""");
		}

		// Default to false, and only set to true if "yes" is passed
		boolean clearAll = "yes".equalsIgnoreCase(clearAllStr);

		if (clearAll) {
			ModelManager.inst().clearKb();
		} else {
			throw new ServletErrorResponseException(
				"To drop an individual named graph or remove all of its statements, use SPARQL/Update.");
		}

		sendSuccess("Clear operation successful.", resp);
	}
}
