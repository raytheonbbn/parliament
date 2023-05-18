// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2010, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.joseki.bridge.servlet;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.joseki.Request;
import org.joseki.http.Servlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jena.joseki.bridge.ParliamentBridge;
import com.bbn.parliament.jena.joseki.bridge.ParliamentBridgeException;

public class ParliamentServlet extends Servlet {
	private static final long serialVersionUID = 1L;

	private static final Logger LOG = LoggerFactory.getLogger(ParliamentServlet.class);

	protected ParliamentBridge _server;

	public ParliamentServlet() {
		this(ParliamentServlet.class.getName());
	}

	public ParliamentServlet(String name) {
		super(name);
	}

	/** {@inheritDoc} */
	@Override
	public void init(ServletConfig config) throws ServletException {
		LOG.info("Initializing parliament servlet");

		String modelConfFile = config.getInitParameter("parliament.config");

		try {
			ParliamentBridge.initialize(modelConfFile, getTempDir(config));
		} catch (ParliamentBridgeException ex) {
			LOG.error("Error while initializing ParliamentBridge", ex);
			throw new ServletException("Error while initializing ParliamentBridge", ex);
		}
		super.init(config);
	}

	private static File getTempDir(ServletConfig config) {
		File result = null;
		if (config != null) {
			ServletContext ctx = config.getServletContext();
			if (ctx != null) {
				Object attrValue = ctx.getAttribute("javax.servlet.context.tempdir");
				if (attrValue != null && attrValue instanceof File fileAttr) {
					result = fileAttr;
				}
			}
		}
		return result;
	}

	@Override
	public void destroy() {
		LOG.info("Shutting down parliament servlet");
		ParliamentBridge.getInstance().stop();

		super.destroy();
	}

	@Override
	protected Request setupRequest(String serviceURI,
		HttpServletRequest httpRequest, String opType) throws IOException {

		if (LOG.isDebugEnabled()) {
			String paramMap = httpRequest.getParameterMap().entrySet().stream()
				.map(e -> "      %1$s: %2$s%n".formatted(e.getKey(),
					Arrays.stream(e.getValue()).collect(Collectors.joining("|"))))
				.collect(Collectors.joining());
			LOG.debug(
				"HTTP request info:{}   Method: {}{}   Content type: {}{}   Accept: {}{}   Query string: {}{}   Parameters: {}{}",
				System.lineSeparator(), httpRequest.getMethod(),
				System.lineSeparator(), httpRequest.getContentType(),
				System.lineSeparator(), httpRequest.getHeader("Accept"),
				System.lineSeparator(), httpRequest.getQueryString(),
				System.lineSeparator(), System.lineSeparator(), paramMap);
		}

		return new ParliamentRequest(httpRequest, serviceURI, opType);
	}

	/**
	 * This is overridden because the default implementation limits the content type
	 * of the request to ParliamentRequest.CONTENT_TYPE_FORM, which is too restrictive
	 * for the SPARQL protocol in its most modern form.
	 */
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
		String ct = ParliamentRequest.getMediaType(httpRequest);
		if (ct == null
			|| ct.equalsIgnoreCase(ParliamentRequest.CONTENT_TYPE_FORM)
			|| ct.equalsIgnoreCase(ParliamentRequest.CONTENT_TYPE_QUERY)
			|| ct.equalsIgnoreCase(ParliamentRequest.CONTENT_TYPE_UPDATE)) {
			doCommon(httpRequest, httpResponse);
		} else {
			try {
				String msg = """
					Unrecognized request content type "%1$s". Must be %2$s, %3$s, or %4$s"""
					.formatted(ct,
						ParliamentRequest.CONTENT_TYPE_FORM,
						ParliamentRequest.CONTENT_TYPE_QUERY,
						ParliamentRequest.CONTENT_TYPE_UPDATE);
				LOG.warn(msg);
				httpResponse.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, msg);
			} catch (IOException ex) {
				LOG.error("Error while sending HTTP response code 415 (unsupported media type)", ex);
			}
		}
	}
}
