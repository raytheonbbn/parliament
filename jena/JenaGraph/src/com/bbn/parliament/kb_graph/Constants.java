package com.bbn.parliament.kb_graph;

import org.apache.jena.sparql.util.Symbol;

public class Constants {
	public static final String NAMESPACE = "http://parliament.semwebcentral.org/";
	public static final String PFUNCTION_NAMESPACE = NAMESPACE + "pfunction#";
	public static final String SYMBOL_PREFIX = NAMESPACE + "symbol#";

	/** Symbol for storing a cancel query flag in a query execution context. */
	public static final Symbol CANCEL_QUERY_FLAG_SYMBOL = createSymbol("cancelled_query");
	public static final Symbol TREE_WIDTH_OPTIMIZATION = createSymbol("tree_width_optimization");
	public static final Symbol DYNAMIC_OPTIMIZATION = createSymbol("dynamic_optimization");
	public static final Symbol DEFAULT_OPTIMIZATION = createSymbol("default_optimization");
	public static final Symbol UPDATED_STATIC_OPTIMIZATION = createSymbol("updated_static_optimization");

	private static Symbol createSymbol(String name) {
		return Symbol.create(SYMBOL_PREFIX + name);
	}
}
