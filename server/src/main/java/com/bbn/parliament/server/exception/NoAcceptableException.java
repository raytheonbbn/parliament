package com.bbn.parliament.server.exception;

import java.util.Arrays;
import java.util.stream.Collectors;

import com.bbn.parliament.server.service.AcceptableMediaType;
import com.bbn.parliament.server.service.QueryResultCategory;

public class NoAcceptableException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public NoAcceptableException(QueryResultCategory category) {
		super(Arrays.stream(AcceptableMediaType.values())
			.filter(mt -> mt.getCategory() == category)
			.map(mt -> mt.getQueryStringFormat())
			.collect(Collectors.joining(
				", ",
				"Parliament can return this response in only these formats: ",
				"")));
	}
}
