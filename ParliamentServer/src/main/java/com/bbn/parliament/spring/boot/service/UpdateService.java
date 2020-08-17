package com.bbn.parliament.spring.boot.service;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;

import com.bbn.parliament.jena.handler.UpdateHandler;

@Component("updateService")
public class UpdateService {
	@SuppressWarnings("static-method")
	public void doUpdate(String update, HttpServletRequest request) throws Exception {
		String requestor = QueryService.getRequestor(request);
		UpdateHandler handler = new UpdateHandler();
		handler.handleRequest(update, requestor);
	}
}
