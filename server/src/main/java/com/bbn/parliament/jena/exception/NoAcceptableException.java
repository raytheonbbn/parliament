package com.bbn.parliament.jena.exception;

import java.util.Arrays;
import java.util.stream.Collectors;

import com.bbn.parliament.spring_boot.service.AcceptableMediaType;
import com.bbn.parliament.spring_boot.service.QueryResultCategory;

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
