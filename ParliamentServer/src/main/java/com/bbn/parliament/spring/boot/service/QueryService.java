
package com.bbn.parliament.spring.boot.service;

import javax.servlet.http.HttpServletRequest;

import com.bbn.parliament.jena.bridge.ActionRouter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;

import java.io.OutputStream;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

@Component("queryService")
public class QueryService {
	
	private static final Logger LOG = LoggerFactory.getLogger(QueryService.class);
	
	@Autowired
	private ActionRouter actionRouter;

	/*
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
	*/
	
	public void doStream (String query, HttpServletRequest request, OutputStream out) {
		String host = getRequestor(request);

		try {
			actionRouter.execQuery(query, host, out);
		}
		catch(Exception e) {
			LOG.info(e.toString());
		}
	}
	
	
	//Taken from ParliamentRequest.java
	private String getRequestor(HttpServletRequest request) { 
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
