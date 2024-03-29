package com.bbn.parliament.jena;

import java.util.List;
import java.util.stream.Collectors;

import com.bbn.parliament.jena.graph.KbGraph;
import com.bbn.parliament.jena.graph.KbGraphFactory;
import com.bbn.parliament.jena.graph.KbGraphStore;
import com.bbn.parliament.jena.joseki.client.StreamUtil;
import com.hp.hpl.jena.graph.Node;

public class TestingDataset {
	public static final Node NAMED_GRAPH_URI = Node.createURI("http://example.org/testgraph");

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
			.filter(node -> !node.getURI().equals(KbGraphStore.MASTER_GRAPH))
			.collect(Collectors.toList());
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
		return getNamedGraph(Node.createURI(graphUri));
	}
}
