package com.bbn.parliament.jena.graph.index.temporal;

import java.util.Properties;

import com.bbn.parliament.jena.graph.KbGraph;
import com.bbn.parliament.jena.graph.KbGraphFactory;
import com.bbn.parliament.jena.graph.KbGraphStore;
import com.bbn.parliament.jena.graph.index.IndexFactoryRegistry;
import com.bbn.parliament.jena.graph.index.IndexManager;
import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalRecordFactory;
import com.bbn.parliament.jena.joseki.client.StreamUtil;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.shared.impl.JenaParameters;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;

public class TemporalTestServer implements AutoCloseable {
	public static final Property INSTANT_PF = ResourceFactory.createProperty(Constants.PT_AS_INSTANT.getURI());
	public static final Property INTERVAL_PF = ResourceFactory.createProperty(Constants.PT_AS_INTERVAL.getURI());

	public static final String COMMON_PREFIXES = """
			prefix rdf:  <%1$s>
			prefix rdfs: <%2$s>
			prefix owl:  <%3$s>
			prefix xsd:  <%4$s>
			prefix snap: <http://www.ifomis.org/bfo/1.0/snap#>
			prefix span: <http://www.ifomis.org/bfo/1.0/span#>
			prefix time: <%5$s>
			prefix pt:   <%6$s>
			""".formatted(RDF.getURI(), RDFS.getURI(), OWL.getURI(), XSD.getURI(),
				Constants.OT_NS, Constants.PT_NS);

	private KbGraph graph;
	private KbGraphStore graphStore;
	private Model model;
	private TemporalIndexFactory indexFactory;

	private TemporalIndex index;
	private TemporalRecordFactory recordFactory;

	// Call from @BeforeAll
	public TemporalTestServer() {
		JenaParameters.enableEagerLiteralValidation = true;
		graph = KbGraphFactory.createDefaultGraph();
		graphStore = new KbGraphStore(graph);
		graphStore.initialize();
		model = ModelFactory.createModelForGraph(graph);
		model.removeAll();

		Properties properties = new Properties();
		properties.put(Constants.INDEX_TYPE, Constants.INDEX_PERSISTENT);
		indexFactory = new TemporalIndexFactory();
		indexFactory.configure(properties);
		IndexFactoryRegistry.getInstance().register(indexFactory);

		index = null;
		recordFactory = null;
	}

	// Call from @AfterAll
	@Override
	public void close() {
		model.removeAll();
		model.close();
		graphStore.clear();
	}

	// Call from @BeforeEach
	public final void setupIndex() {
		model.removeAll();
		index = createIndexForGraph(graph, null);
		recordFactory = index.getRecordFactory();

		StreamUtil.asStream(graphStore.listGraphNodes())
			.filter(graphName -> !KbGraphStore.MASTER_GRAPH.equals(graphName.getURI()))
			.forEach(graphName -> createIndexForGraph(graphStore.getGraph(graphName), graphName));
	}

	private TemporalIndex createIndexForGraph(Graph g, Node gName) {
		TemporalIndex result = indexFactory.createIndex(g, gName);
		IndexManager.getInstance().register(g, gName, indexFactory, result);
		result.open();
		result.clear();
		return result;
	}

	// Call from @AfterEach
	public final void removeIndex() {
		IndexManager.getInstance().unregister(graph, null, index);
	}

	public Model getModel() {
		return model;
	}

	public Dataset getDataset() {
		return graphStore.toDataset();
	}

	public TemporalRecordFactory getRecordFactory() {
		return recordFactory;
	}

	public TemporalIndex getIndex() {
		return index;
	}
}
