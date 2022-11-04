package com.bbn.parliament.jena.graph.index.spatial.geosparql.function;

import org.apache.jena.graph.Node;

public class UnsupportedUnitsException extends GeoSPARQLFunctionException {
	private static final long serialVersionUID = 1L;

	public UnsupportedUnitsException(Node units) {
		super("%s is not supported for this function".formatted(units));
	}
}
