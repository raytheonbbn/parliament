package com.bbn.parliament.spring.boot.service;

import java.util.Objects;

public enum QueryResultCategory {
	/** For construct and describe queries */
	RDF("TURTLE"),

	/** For select and ask queries */
	RESULT_SET("JSON_RESULTS");

	// Must be the string representation of an enumerator from AcceptableMediaType.
	// We choose this odd representation instead of storing an AcceptableMediaType
	// directly because if we did that, we would create a circular dependency
	// between the two enum classes.
	private final String defaultMediaType;

	private QueryResultCategory(String defaultMediaType) {
		this.defaultMediaType = Objects.requireNonNull(defaultMediaType, "defaultMediaType");
	}

	public AcceptableMediaType getDefaultMediaType() {
		return Enum.valueOf(AcceptableMediaType.class, defaultMediaType);
	}
}
