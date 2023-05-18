package com.bbn.parliament.kb_graph;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;

import com.bbn.parliament.client.StreamUtil;

public class TestingDataset {
	public static final Node NAMED_GRAPH_URI = NodeFactory.createURI("http://example.org/testgraph");

	private KbGraph defaultGraph;
	private KbGraph namedGraph;
	private KbGraphStore dataset;

	// For @BeforeAll
	public TestingDataset() {
		Kb.init();
		defaultGraph = KbGraphFactory.createDefaultGraph();
		namedGraph = KbGraphFactory.createNamedGraph();
		dataset = new KbGraphStore(defaultGraph);
		dataset.initialize();
		dataset.addGraph(NAMED_GRAPH_URI, namedGraph);
	}

	// For @AfterAll
	public void clear() {
		dataset.clear();
	}

	// For @AfterEach
	public void reset() {
		defaultGraph.clear();
		namedGraph.clear();
		List<Node> graphs = StreamUtil.asStream(dataset.listGraphNodes())
			.filter(node -> !node.equals(NAMED_GRAPH_URI))
			.filter(node -> !node.equals(KbGraphStore.MASTER_GRAPH))
			.collect(Collectors.toUnmodifiableList());
		graphs.stream().forEach(node -> dataset.removeGraph(node));
	}

	public KbGraphStore getGraphStore() {
		return dataset;
	}

	public KbGraph getDefaultGraph() {
		return defaultGraph;
	}

	public KbGraph getNamedGraph() {
		return namedGraph;
	}

	public KbGraph getNamedGraph(Node graphUri) {
		if (!dataset.containsGraph(graphUri)) {
			@SuppressWarnings("resource")
			KbGraph newGraph = KbGraphFactory.createNamedGraph();
			dataset.addGraph(graphUri, newGraph);
		}
		return (KbGraph) dataset.getGraph(graphUri);
	}

	public KbGraph getNamedGraph(String graphUri) {
		return getNamedGraph(NodeFactory.createURI(graphUri));
	}
}
