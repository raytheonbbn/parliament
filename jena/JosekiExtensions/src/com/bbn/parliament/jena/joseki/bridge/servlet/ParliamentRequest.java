// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2014-2015, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.joseki.bridge.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.joseki.Joseki;
import org.joseki.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QueryParseException;
import com.hp.hpl.jena.query.Syntax;

public class ParliamentRequest extends Request {
	private static enum RequestType { QUERY, UPDATE, OTHER }

	public static final String CONTENT_TYPE_QUERY	= "application/sparql-query";
	public static final String CONTENT_TYPE_UPDATE	= "application/sparql-update";
	public static final String CONTENT_TYPE_FORM		= "application/x-www-form-urlencoded";

	public static final String PARAM_QUERY				= "query";
	public static final String PARAM_UPDATE			= "update";

	private static Logger log = LoggerFactory.getLogger(ParliamentRequest.class);

	private HttpServletRequest _httpReq;
	private RequestType _requestType;
	private String _sparqlStmt;

	public ParliamentRequest(HttpServletRequest httpRequest, String uri, String opType)
		throws IOException {
		super(uri, null);
		_httpReq = httpRequest;
		_requestType = RequestType.OTHER;
		_sparqlStmt = null;

		// params => request items
		setParam(Joseki.OPERATION, opType);
		for (Map.Entry<String, String[]> entry : _httpReq.getParameterMap().entrySet()) {
			String key = entry.getKey();
			for (String value : entry.getValue()) {
				setParam(key, value);
			}
		}

		String method = _httpReq.getMethod().toUpperCase();
		log.trace("Request method = {}", method);

		if ("GET".equalsIgnoreCase(method)) {
			if (containsParam(PARAM_QUERY)) {
				_requestType = RequestType.QUERY;
				_sparqlStmt = getParam(PARAM_QUERY);
				log.trace("Request:  query by GET ({})", _sparqlStmt.length());
			}
		} else if ("POST".equalsIgnoreCase(method)) {
			String contentType = getMediaType(_httpReq);
			log.trace("Request contentType = {}", contentType);

			if (CONTENT_TYPE_QUERY.equalsIgnoreCase(contentType)) {
				_requestType = RequestType.QUERY;
				_sparqlStmt = getRequestContent(_httpReq);
				log.trace("Request:  query by non-form POST ({})", _sparqlStmt.length());
			} else if (CONTENT_TYPE_UPDATE.equalsIgnoreCase(contentType)) {
				_requestType = RequestType.UPDATE;
				_sparqlStmt = getRequestContent(_httpReq);
				log.trace("Request:  update by non-form POST ({})", _sparqlStmt.length());
			} else if (CONTENT_TYPE_FORM.equalsIgnoreCase(contentType)) {
				if (containsParam(PARAM_QUERY)) {
					_requestType = RequestType.QUERY;
					_sparqlStmt = getParam(PARAM_QUERY);
					log.trace("Request:  query by form-based POST ({})", _sparqlStmt.length());
				} else if (containsParam(PARAM_UPDATE)) {
					_requestType = RequestType.UPDATE;
					_sparqlStmt = getParam(PARAM_UPDATE);
					log.trace("Request:  update by form-based POST ({})", _sparqlStmt.length());
				} else {
					log.warn("Request:  form-based POST contains neither query nor update parameters");
				}
			}
		}
	}

	/** Answers the question, "Is this request a SPARQL query of type SELECT or ASK?" */
	public static boolean isSelectOrAskQuery(ServletRequest req) throws IOException {
		boolean result = false;
		if (req instanceof HttpServletRequest httpReq) {
			String method = httpReq.getMethod().toUpperCase();
			String contentType = getMediaType(httpReq);

			String queryStr = null;
			if ("GET".equalsIgnoreCase(method)) {
				log.trace("ParliamentRequest.isSelectOrAskQuery:  Processing GET request");
				queryStr = httpReq.getParameter(PARAM_QUERY);
			} else if ("POST".equalsIgnoreCase(method)) {
				if (CONTENT_TYPE_QUERY.equalsIgnoreCase(contentType)) {
					log.trace("ParliamentRequest.isSelectOrAskQuery:  Processing non-form POST query");
					queryStr = getRequestContent(httpReq);
				} else if (CONTENT_TYPE_FORM.equalsIgnoreCase(contentType)) {
					log.trace("ParliamentRequest.isSelectOrAskQuery:  Processing form-based POST request");
					queryStr = httpReq.getParameter(PARAM_QUERY);
				} else {
					log.trace("ParliamentRequest.isSelectOrAskQuery:  Processing other (not applicable) request type");
				}
			}

			try {
				if (queryStr != null && !queryStr.isEmpty()) {
					log.trace("ParliamentRequest.isSelectOrAskQuery:  inspecting query string ([{}])", queryStr.length());
					Query query = QueryFactory.create(queryStr, Syntax.syntaxARQ);
					result = (query != null) && (query.isSelectType() || query.isAskType());
				} else {
					log.trace("ParliamentRequest.isSelectOrAskQuery:  no query string to inspect");
				}
			} catch (QueryParseException ex) {
				// We can safely ignore this exception, because a proper error
				// response will be generated and sent to the client later.  At
				// this point we can just declare that it isn't a select or ask
				// query, because it isn't a legal query at all.
			}
		}
		return result;
	}

	public static String getMediaType(ServletRequest req) {
		return (req instanceof HttpServletRequest httpReq)
			? getMediaType(httpReq)
			: null;
	}

	public static String getMediaType(HttpServletRequest req) {
		String ct = req.getContentType();
		if (ct != null) {
			int semiIndex = ct.indexOf(';');
			if (semiIndex != -1) {
				ct = ct.substring(0, semiIndex).strip();
			}
		}
		return ct;
	}

	private static String getRequestContent(HttpServletRequest httpReq) throws IOException {
		try (BufferedReader rdr = httpReq.getReader()) {
			return rdr.lines().collect(Collectors.joining(System.lineSeparator()));
		}
	}

	public String getRequestor() {
		String host = _httpReq.getRemoteHost();
		if (host == null || host.isEmpty()) {
			host = _httpReq.getRemoteAddr();
		}
		String user = _httpReq.getRemoteUser();
		return (user == null || user.isEmpty())
			? host
			: "%1$s (%2$s)".formatted(host, user);
	}

	public boolean isSparqlQuery() {
		return _requestType == RequestType.QUERY;
	}

	public boolean isSparqlUpdate() {
		return _requestType == RequestType.UPDATE;
	}

	public String getSparqlStmt() {
		return _sparqlStmt;
	}

	public HttpServletRequest getHttpReq() {
		return _httpReq;
	}
}
