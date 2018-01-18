package com.bbn.parliament.jena.graph.index.spatial.geosparql.function;

import com.hp.hpl.jena.query.QueryExecException;

/** @author rbattle */
public class GeoSPARQLFunctionException extends QueryExecException {
	private static final long serialVersionUID = 1L;

	public GeoSPARQLFunctionException() {
	}

	public GeoSPARQLFunctionException(Throwable cause) {
		super(cause);
	}

	public GeoSPARQLFunctionException(String msg) {
		super(msg);
	}

	public GeoSPARQLFunctionException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
