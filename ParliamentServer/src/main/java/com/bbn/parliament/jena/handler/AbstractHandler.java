// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.handler;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

/**
 * AbstractHandler that provides some common methods for subclasses.
 *
 * @author sallen
 */
public abstract class AbstractHandler {
	protected static final String DEFAULT_GRAPH_BASENAME = "Default Graph";

	/** Gets the logger */
	protected abstract Logger getLog();

	/** Sends an <tt>OK</tt> response with the supplied message. */
	protected static void sendSuccess(HttpServletResponse resp, String format, Object... args) throws IOException {
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
