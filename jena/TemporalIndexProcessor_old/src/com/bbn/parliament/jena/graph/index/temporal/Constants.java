package com.bbn.parliament.jena.graph.index.temporal;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;

public class Constants {
	public static final String TIME_NS = "http://www.w3.org/2006/time#";

	public static final Node DATE_TIME_INTERVAL = createURI("DateTimeInterval");
	public static final Node PROPER_INTERVAL = createURI("ProperInterval");
	public static final Node DATE_TIME = createURI("xsdDateTime");
	public static final Node INTERVAL_STARTED_BY = createURI("intervalStartedBy");
	public static final Node INTERVAL_FINISHED_BY = createURI("intervalFinishedBy");

	private static Node createURI(String name) {
		return NodeFactory.createURI(TIME_NS + name);
	}

	public static final Node[] VALID_TYPES = { PROPER_INTERVAL, DATE_TIME_INTERVAL };

	public static final String INDEX_TYPE = "indexType";
	public static final String INDEX_PERSISTENT = "bdb";
	public static final String INDEX_MEMORY = "mem";

	public static final String ALWAYS_USE_FIRST = "alwaysUseFirst";

	public static final int QUERY_CACHE_SIZE = 100;
}
