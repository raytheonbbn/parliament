package com.bbn.parliament.kb_graph.index.spatial;

public interface OperationFactory {
	public Operation createOperation(String uri);
	public String[] getURIs();
}
