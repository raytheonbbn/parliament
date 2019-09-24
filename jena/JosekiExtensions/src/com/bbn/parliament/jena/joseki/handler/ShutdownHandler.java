// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2010, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.joseki.handler;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jena.jetty.JettyServerCore;
import com.bbn.parliament.jena.joseki.bridge.servlet.ServletErrorResponseException;

public class ShutdownHandler extends AbstractHandler {
	private static Logger _log = LoggerFactory.getLogger(ShutdownHandler.class);

	@Override
	protected Logger getLog() {
		return _log;
	}

	@Override
	public void handleFormURLEncodedRequest(HttpServletRequest req,
		HttpServletResponse resp) throws IOException, ServletErrorResponseException {
		// Request that the server shutdown
		JettyServerCore.getInstance().stopCore();
		sendSuccess("Server shutdown requested.  Check the Windows Service "
			+ "Console to determine when the server has finished shutting down.", resp);
	}

	@Override
	public void handleMultipartFormRequest(HttpServletRequest req,
		HttpServletResponse resp) throws IOException, ServletErrorResponseException {
		throw new ServletErrorResponseException("'multipart/form data' requests "
			+ "are not supported by this handler.");
	}
}
