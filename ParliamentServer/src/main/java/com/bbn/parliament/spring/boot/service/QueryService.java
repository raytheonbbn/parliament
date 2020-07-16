
package com.bbn.parliament.spring.boot.service;

import javax.servlet.http.HttpServletRequest;

import com.bbn.parliament.jena.bridge.ActionRouter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;

import java.io.OutputStream;

public class QueryService {
	
	private static final Logger LOG = LoggerFactory.getLogger(QueryService.class);

	public static String doCommon (String query, HttpServletRequest request) {
		
		String host = getRequestor(request);
		ActionRouter router = new ActionRouter();
		
		try {
			ResultSet result = router.execQuery(query, host);
			//LOG.info(ResultSetFormatter.asText(result)); //caution, low performance
			return ResultSetFormatter.asXMLString(result);
		}
		catch(Exception e) {
			return null;
		}
	}
	
	public static void doStream (String query, HttpServletRequest request, OutputStream out) {
		
		String host = getRequestor(request);
		ActionRouter router = new ActionRouter();

		try {
			router.execQuery(query, host, out);
			//LOG.info(ResultSetFormatter.asText(result)); //caution, low performance
			//return ResultSetFormatter.asXMLString(result);
		}
		catch(Exception e) {

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
