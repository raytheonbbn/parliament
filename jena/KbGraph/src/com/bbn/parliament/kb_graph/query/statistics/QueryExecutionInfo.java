package com.bbn.parliament.kb_graph.query.statistics;

import org.apache.jena.query.Query;

public class QueryExecutionInfo {
	private long creationTime;
	private Query query;

	public QueryExecutionInfo(Query query) {
		this.query = query;
		this.creationTime = System.currentTimeMillis();
	}

	public long getCreationTime() {
		return creationTime;
	}

	public Query getQuery() {
		return query;
	}

	public long getAge() {
		return System.currentTimeMillis() - creationTime;
	}
}
