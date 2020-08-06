package com.bbn.parliament.spring.boot.service;

import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.bbn.parliament.jena.bridge.ActionRouter;
import com.bbn.parliament.jena.exception.QueryExecutionException;

@Component("queryService")
public class QueryService {
	@Autowired
	private ActionRouter actionRouter;

	public void doQuery(String query, HttpServletRequest request, OutputStream out)
			throws QueryExecutionException {
		String requestor = getRequestor(request);
		actionRouter.execQuery(query, requestor, out);
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
