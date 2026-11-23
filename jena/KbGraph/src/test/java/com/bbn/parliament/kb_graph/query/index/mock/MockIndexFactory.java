package com.bbn.parliament.kb_graph.query.index.mock;

import java.util.Properties;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;

import com.bbn.parliament.kb_graph.index.IndexFactory;

public class MockIndexFactory extends IndexFactory<MockIndex, Integer> {
	public MockIndexFactory() {
		super("Mock");
	}

	@Override
	public void configure(Properties configuration) {
	}

	@Override
	public MockIndex createIndex(Graph graph, Node graphName, String indexDir) {
		return new MockIndex();
	}
}
