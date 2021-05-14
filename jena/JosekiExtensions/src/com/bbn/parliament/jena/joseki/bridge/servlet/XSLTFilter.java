// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, 2014-2015, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.joseki.bridge.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This servlet filter will apply XSL transformations on the server instead of the browser
 * if it can locate them on the local http server.
 *
 * @author sallen
 */
public class XSLTFilter implements Filter {
	private static final String METHOD_XFORM = "<?xml version=\"1.0\"?>"
		+ "<xsl:transform version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">"
		+ "	<xsl:output method=\"text\" encoding=\"utf-8\"/>"
		+ "	<xsl:template match=\"/xsl:transform|/xsl:stylesheet\">"
		+ "		<xsl:value-of select=\"xsl:output/@method\"/>"
		+ "	</xsl:template>"
		+ "</xsl:transform>";

	private static final Logger LOG = LoggerFactory.getLogger(XSLTFilter.class);

	private ServletContext _context;
	private IOException _threadIOException;
	private ServletException _threadServletException;
	private RuntimeException _threadRuntimeException;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		_context = filterConfig.getServletContext();
		_threadIOException = null;
		_threadServletException = null;
		_threadRuntimeException = null;
	}

	@Override
	public void destroy() {
		// Do nothing
	}

	@Override
	public void doFilter(final ServletRequest req, final ServletResponse resp, final FilterChain chain)
		throws IOException, ServletException {

		// This will return false even if the query is select or ask, but doesn't parse properly:
		boolean isTransformableQuery = ParliamentRequest.isSelectOrAskQuery(req);

		URL styleURL = null;
		String styleSheet = isTransformableQuery ? req.getParameter("stylesheet") : null;
		if (isTransformableQuery && null != styleSheet && !styleSheet.trim().isEmpty()) {
			try {
				styleURL = _context.getResource(styleSheet);
			} catch (MalformedURLException ex) {
				// user put an URL in maybe?
			}
			if (null == styleURL) {
				try {
					styleURL = new URL(styleSheet);
					if (!urlExists(styleURL)) {
						styleURL = null;
					}
				} catch (MalformedURLException ex) {
					LOG.warn("Malformed stylesheet parameter \"{}\":  {}", styleSheet, ex.getMessage());
				}
			}
		}

		if (isTransformableQuery && null != styleURL) {
			String method = getTransformMethod(styleURL);
			final String contentType = methodToContentType(method);

			final PipedServletResponseWrapper wrapper = new PipedServletResponseWrapper(
				(HttpServletResponse) resp);
			wrapper.run(new Runnable() {
				@Override
				public void run() {
					try (OutputStream os = wrapper.getOutputStream()) {
						chain.doFilter(req, wrapper);

						if ("application/xml".equals(resp.getContentType()) && contentType != null) {
							// We set the content type here because we have to do it after the
							// Joseki code mistakenly sets it to 'application/xml', and that code
							// only executes within the filter chain that executes above.
							LOG.trace("Content-Type for response: {}", contentType) ;
							resp.setContentType(contentType);
						}
					} catch (IOException ex) {
						LOG.error("IOException writing to PipedServletResponseWrapper", ex);
						_threadIOException = ex;
					} catch (ServletException ex) {
						LOG.error("ServletException writing to PipedServletResponseWrapper", ex);
						_threadServletException = ex;
					} catch (RuntimeException ex) {
						LOG.error("RuntimeException writing to PipedServletResponseWrapper", ex);
						_threadRuntimeException = ex;
					}
				}
			});

			LOG.debug("Applying XSL transformation on the server ({})", styleSheet);

			try {
				Transformer transformer = null;
				try (InputStream styleIn = styleURL.openStream()) {
					StreamSource styleSource = new StreamSource(styleIn);
					TransformerFactory transformerFactory = TransformerFactory.newInstance();
					transformer = transformerFactory.newTransformer(styleSource);
					LOG.trace("Created transformer");
				}

				try (InputStream in = wrapper.getInputStream()) {
					StreamSource xmlSource = new StreamSource(in);
					@SuppressWarnings("resource")
					StreamResult result = new StreamResult(resp.getOutputStream());
					transformer.transform(xmlSource, result);
					LOG.trace("Transformed source");
				}

				try {
					wrapper.join();
					LOG.trace("Joined on wrapper");
				} catch (InterruptedException e) {
					LOG.error("Interrupted while joining PipedServletResponseWrapper thread.", e);
					throw new ServletException(e);
				}

				// Check for exceptions from the PipedServletResponseWrapper thread
				if (null != _threadIOException) {
					throw _threadIOException;
				} else if (null != _threadServletException) {
					throw _threadServletException;
				} else if (null != _threadRuntimeException) {
					throw _threadRuntimeException;
				}
			} catch (TransformerException e) {
				LOG.error("Exception during transform", e);
				throw new ServletException(e);
			}
		} else {
			LOG.debug("Passing response through XSLTFilter without applying stylesheet");
			chain.doFilter(req, resp);
		}
	}

	public static boolean urlExists(URL url) {
		boolean toReturn = false;
		try {
			HttpURLConnection.setFollowRedirects(false);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("HEAD");
			toReturn = (con.getResponseCode() == HttpURLConnection.HTTP_OK);
		} catch (Exception e) {
			// Do nothing
		}
		return toReturn;
	}

	private static String methodToContentType(String method) {
		String contentType = null;
		switch (method) {
		case "xml":
			contentType = "application/xml";
			break;
		case "html":
			contentType = "text/html";
			break;
		case "text":
			contentType = "text/csv";
			break;
		}
		return contentType;
	}

	private static String getTransformMethod(URL styleURL) {
		String method = null;
		try {
			Transformer transformer = null;
			try (StringReader styleIn = new StringReader(METHOD_XFORM)) {
				StreamSource styleSource = new StreamSource(styleIn);
				TransformerFactory transformerFactory = TransformerFactory.newInstance();
				transformer = transformerFactory.newTransformer(styleSource);
				LOG.trace("Loaded the method-extractor XSLT");
			}

			try (InputStream in = styleURL.openStream()) {
				StreamSource xmlSource = new StreamSource(in);
				StringWriter wtr = new StringWriter();
				StreamResult result = new StreamResult(wtr);
				transformer.transform(xmlSource, result);
				method = wtr.toString();
				LOG.trace("Extracted the method ('{}') from the XSLT '{}'", method, styleURL);
			}
		} catch (TransformerException | IOException ex) {
			LOG.warn("{} exception while trying to extract method from XSLT:  {}",
				ex.getClass().getName(), ex.getMessage());
		}
		return method;
	}
}
