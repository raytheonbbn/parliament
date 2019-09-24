// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
/**
 *
 */

package com.bbn.parliament.jena.joseki.bridge.servlet;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.bbn.parliament.jena.joseki.bridge.ParliamentBridge;
import com.bbn.parliament.jena.joseki.bridge.util.HttpServerUtil;
import com.bbn.parliament.jena.joseki.handler.ClearHandler;
import com.bbn.parliament.jena.joseki.handler.ExportHandler;
import com.bbn.parliament.jena.joseki.handler.FlushHandler;
import com.bbn.parliament.jena.joseki.handler.InsertHandler;

/** @author sallen */
public class BulkServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final int URI_LIMIT = 8 * 1024;
	protected static final Logger LOG = LoggerFactory.getLogger(BulkServlet.class);

	private boolean _initAttempted = false;
	private ServletConfig _servletConfig = null;

	public BulkServlet() {
		this("BulkServlet");
	}

	public BulkServlet(String string) {
		LOG.info("-------- {}", string);
	}

	@Override
	public void init() throws ServletException {
		super.init();
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		// It seems that if the servlet fails to initialize the first time, init can be
		// called again.  (It has been observed in Tomcat log files but not explained.)
		if (_initAttempted) {
			LOG.warn("Re-initialization of servlet attempted");
		} else {

			LOG.info("Initializing BulkServlet");

			_initAttempted = true;
			_servletConfig = config;
			super.init(_servletConfig);

			// Set the temp directory.  We know ParliamentBridge is initialized already
			// because the servlet container's web.xml specifies that the BulkServlet
			// should be initialized after the ParliamentServlet.
			ParliamentBridge bridge = ParliamentBridge.getInstance();
			File tmpDir = bridge.getConfiguration().getTmpDir();
			int threshold = bridge.getConfiguration().getDeferredFileOutputStreamThreshold();
			HttpServerUtil.init(tmpDir, threshold);
		}
	}

	@Override
	public void destroy() {
		super.destroy();
		LOG.debug("Servlet \"{}\" destroyed", _servletConfig.getServletName());
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {
		// We don't allow operations via GET, so let superclass handle any requests...
		super.doGet(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {
		try {
			String uri = req.getRequestURI() ;
			if (uri.length() > URI_LIMIT) {
				throw new ServletErrorResponseException(HttpServletResponse.SC_REQUEST_URI_TOO_LONG,
					"Request URI exceeds the limit of %1$d characters", URI_LIMIT);
			} else {
				String svcUri = getServiceUri(uri, req);
				if ("insert".equals(svcUri)) {
					InsertHandler handler = new InsertHandler();
					if (HttpServerUtil.isMultipartContent(req)) {
						LOG.info("Insert operation (multipart/form-data) from {}",
							req.getRemoteAddr());
						handler.handleMultipartFormRequest(req, resp);
					} else {
						LOG.info("Insert operation (x-www-form-urlencoded) from {}",
							req.getRemoteAddr());
						handler.handleFormURLEncodedRequest(req, resp);
					}
				} else if ("delete".equals(svcUri)) {
					LOG.info("Delete operation from {}", req.getRemoteAddr());
					throw new ServletErrorResponseException("Bulk delete is not supported.  "
						+ "Use SPARQL/Update to remove statements.");
				} else if ("clear".equals(svcUri)) {
					LOG.info("Clear operation from {}", req.getRemoteAddr());
					ClearHandler handler = new ClearHandler();
					handler.handleFormURLEncodedRequest(req, resp);
				} else if ("flush".equals(svcUri)) {
					LOG.info("Flush operation from {}", req.getRemoteAddr());
					FlushHandler handler = new FlushHandler();
					handler.handleFormURLEncodedRequest(req, resp);
				} else if ("export".equals(svcUri)) {
					LOG.info("Export operation from {}", req.getRemoteAddr());
					ExportHandler handler = new ExportHandler();
					handler.handleFormURLEncodedRequest(req, resp);
				} else {
					throw new ServletErrorResponseException("Operation does not exist: %1$s", svcUri);
				}
			}
		} catch (ServletErrorResponseException ex) {
			LOG.warn("Bad request", ex);
			resp.sendError(ex.getStatusCode(), ex.getMessage());
		} catch (Exception ex) {
			LOG.error("Internal server error", ex);
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage());
		}
	}

	private static String getServiceUri(String uri, HttpServletRequest req)
	{
		String svcUri = uri;
		String contextPath = req.getContextPath();
		String servletPath = req.getServletPath();

		if (contextPath != null && contextPath.length() > 0) {
			svcUri = svcUri.substring(contextPath.length());
		}

		if (servletPath != null && servletPath.length() > 0) {
			svcUri = svcUri.substring(servletPath.length());
		}

		while (svcUri.startsWith("/")) {
			svcUri = svcUri.substring(1) ;
		}

		return svcUri.toLowerCase();
	}
}
