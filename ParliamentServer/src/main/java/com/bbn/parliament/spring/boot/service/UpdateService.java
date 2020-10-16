package com.bbn.parliament.spring.boot.service;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import com.bbn.parliament.jena.exception.BadRequestException;
import com.bbn.parliament.jena.exception.QueryExecutionException;
import com.bbn.parliament.jena.handler.UpdateHandler;

@Component("updateService")
public class UpdateService {
	@SuppressWarnings("static-method")
	public void doUpdate(String update, List<String> defaultGraphUris,
		List<String> namedGraphUris, HttpHeaders headers, HttpServletRequest request)
		throws BadRequestException, QueryExecutionException {

		if ((defaultGraphUris != null && defaultGraphUris.size() > 0)
			|| (namedGraphUris != null && namedGraphUris.size() > 0)) {
			throw new BadRequestException();
		}

		String requestor = ServiceUtil.getRequestor(headers, request);

		new UpdateHandler().handleRequest(update, requestor);
	}
}
