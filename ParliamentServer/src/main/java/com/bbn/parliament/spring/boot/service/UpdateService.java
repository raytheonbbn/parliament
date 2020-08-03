package com.bbn.parliament.spring.boot.service;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.bbn.parliament.jena.bridge.ActionRouter;


@Component("updateService")
public class UpdateService {

	private static final Logger LOG = LoggerFactory.getLogger(QueryService.class);

	@Autowired
	private ActionRouter actionRouter;

	public void doUpdate(String update, HttpServletRequest request) throws Exception {
		String host = getRequestor(request);

		try {
			actionRouter.execUpdate(update, host);
		} catch (Exception e) {
			LOG.info(e.toString());
			throw new Exception();
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
