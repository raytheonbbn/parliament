package com.bbn.parliament.spring.boot.service;

import javax.servlet.http.HttpServletRequest;

import com.bbn.parliament.jena.bridge.ActionRouter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;

public class QueryService {
	
	private static final Logger LOG = LoggerFactory.getLogger(QueryService.class);

	public static String doCommon (String query, HttpServletRequest request) {
		
		String host = getRequestor(request);
		ActionRouter router = new ActionRouter();
		
		
		try {
			ResultSet result = router.execQuery(query, host);
			return ResultSetFormatter.asText(result); //caution, low performance
		}
		catch(Exception e) {
			return "Error! " + e.toString();
		}
	}
	
	
	//Taken from ParliamentRequest.java
	private static String getRequestor(HttpServletRequest request) { 
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
