package com.bbn.parliament.jena.graph.index.spatial;

public interface OperationFactory {
	public Operation createOperation(String uri);
	public String[] getURIs();
}
