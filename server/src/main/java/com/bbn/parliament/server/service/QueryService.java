package com.bbn.parliament.server.service;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.bbn.parliament.server.exception.BadRequestException;
import com.bbn.parliament.server.handler.QueryHandler;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class QueryService {
	@SuppressWarnings("static-method")
	public ResponseEntity<StreamingResponseBody> doQuery(String query, List<String> defaultGraphUris,
		List<String> namedGraphUris, String format, HttpHeaders headers,
		HttpServletRequest request) throws BadRequestException {

		if ((defaultGraphUris != null && defaultGraphUris.size() > 0)
			|| (namedGraphUris != null && namedGraphUris.size() > 0)) {
			throw new BadRequestException("Parliament does not support explicit graph URI parameters");
		}

		List<AcceptableMediaType> acceptList = ServiceUtil.getAcceptList(format, headers);
		String requestor = ServiceUtil.getRequestor(request);
		QueryHandler handler = new QueryHandler(query, acceptList, requestor);
		return ResponseEntity.status(HttpStatus.OK)
			.contentType(ServiceUtil.getSpringMediaType(handler.getContentType()))
			.body(handler::handleRequest);
	}
}
