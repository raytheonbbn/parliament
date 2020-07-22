package com.bbn.parliament.spring.boot.service;

import com.bbn.parliament.jena.bridge.ActionRouter;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;


@Component("updateService")
public class UpdateService {
	
	private static final Logger LOG = LoggerFactory.getLogger(QueryService.class);
	
	@Autowired
	private ActionRouter actionRouter;
	
	public void doUpdate(String update, HttpServletRequest request) {
		String host = getRequestor(request);
		
		try {
			actionRouter.execUpdate(update, host);
		} catch (Exception e) {
			LOG.info(e.toString());
		}
	}
	
	
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
