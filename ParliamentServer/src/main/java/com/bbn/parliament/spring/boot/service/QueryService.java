package com.bbn.parliament.spring.boot.service;

import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;

import com.bbn.parliament.jena.exception.QueryExecutionException;
import com.bbn.parliament.jena.handler.QueryHandler;

@Component("queryService")
public class QueryService {
	@SuppressWarnings("static-method")
	public void doQuery(String query, HttpServletRequest request, OutputStream out)
			throws QueryExecutionException {
		String requestor = getRequestor(request);
		QueryHandler handler = new QueryHandler();
		handler.handleRequest(query, requestor, out);
	}

	static String getRequestor(HttpServletRequest request) {
		String host = request.getRemoteHost();
		if (host == null || host.isEmpty()) {
			host = request.getRemoteAddr();
		}
		String user = request.getRemoteUser();
		return (user == null || user.isEmpty())
			? host
			: String.format("%1$s (%2$s)", host, user);
	}
}
