package com.bbn.parliament.server.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.bbn.parliament.server.exception.BadRequestException;
import com.bbn.parliament.server.exception.QueryExecutionException;
import com.bbn.parliament.server.handler.UpdateHandler;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class UpdateService {
	@SuppressWarnings("static-method")
	public void doUpdate(String update, List<String> defaultGraphUris,
		List<String> namedGraphUris, HttpServletRequest request)
		throws BadRequestException, QueryExecutionException {

		if ((defaultGraphUris != null && defaultGraphUris.size() > 0)
			|| (namedGraphUris != null && namedGraphUris.size() > 0)) {
			throw new BadRequestException("Explicit graph URI parameters are not supported");
		}

		String requestor = ServiceUtil.getRequestor(request);

		new UpdateHandler().handleRequest(update, requestor);
	}
}
