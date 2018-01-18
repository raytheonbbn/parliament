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

import com.bbn.parliament.jena.joseki.bridge.servlet.ServletErrorResponseException;
import com.bbn.parliament.jena.joseki.graph.ModelManager;

public class FlushHandler extends AbstractHandler {
	private static final Logger LOG = LoggerFactory.getLogger(FlushHandler.class);

	@Override
	protected Logger getLog() {
		return LOG;
	}

	@Override
	public void handleFormURLEncodedRequest(HttpServletRequest req,
		HttpServletResponse resp) throws IOException, ServletErrorResponseException {
		ModelManager.inst().flushKb();
		sendSuccess("Flush operation successful.", resp);
	}

	@Override
	public void handleMultipartFormRequest(HttpServletRequest req,
		HttpServletResponse resp) throws IOException,
	ServletErrorResponseException {
		throw new ServletErrorResponseException("'multipart/form data' requests are not "
			+ "supported by this handler.");
	}
}
