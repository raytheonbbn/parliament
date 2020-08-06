package com.bbn.parliament.spring.boot.service;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.bbn.parliament.jena.bridge.ActionRouter;

@Component("updateService")
public class UpdateService {
	@Autowired
	private ActionRouter actionRouter;

	public void doUpdate(String update, HttpServletRequest request) throws Exception {
		String requestor = QueryService.getRequestor(request);
		actionRouter.execUpdate(update, requestor);
	}
}
