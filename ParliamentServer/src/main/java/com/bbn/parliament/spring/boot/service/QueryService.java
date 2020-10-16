package com.bbn.parliament.spring.boot.service;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.bbn.parliament.jena.exception.BadRequestException;
import com.bbn.parliament.jena.handler.QueryHandler;

@Component("queryService")
public class QueryService {
	@SuppressWarnings("static-method")
	public ResponseEntity<StreamingResponseBody> doQuery(String query, List<String> defaultGraphUris,
		List<String> namedGraphUris, String format, HttpHeaders headers,
		HttpServletRequest request) throws BadRequestException {

		if ((defaultGraphUris != null && defaultGraphUris.size() > 0)
			|| (namedGraphUris != null && namedGraphUris.size() > 0)) {
			throw new BadRequestException();
		}

		List<AcceptableMediaType> acceptList = ServiceUtil.getAcceptList(format, headers);
		String requestor = ServiceUtil.getRequestor(headers, request);
		QueryHandler handler = new QueryHandler(query, acceptList, requestor);
		String contentType = handler.getContentType().getPrimaryMediaType();
		return ResponseEntity.status(HttpStatus.OK)
			.contentType(ServiceUtil.mediaTypeFromString(contentType))
			.body(handler::handleRequest);
	}
}
