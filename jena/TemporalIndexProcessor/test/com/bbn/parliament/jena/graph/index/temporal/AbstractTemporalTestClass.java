package com.bbn.parliament.jena.graph.index.temporal;

import java.util.Iterator;
import java.util.Properties;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jena.graph.KbGraph;
import com.bbn.parliament.jena.graph.KbGraphFactory;
import com.bbn.parliament.jena.graph.KbGraphStore;
import com.bbn.parliament.jena.graph.index.IndexFactoryRegistry;
import com.bbn.parliament.jena.graph.index.IndexManager;
import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalRecordFactory;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.shared.impl.JenaParameters;

public abstract class AbstractTemporalTestClass {

	protected static final Logger LOG = LoggerFactory
			.getLogger(AbstractTemporalTestClass.class);

	protected static final Property INSTANT_PF = ResourceFactory.createProperty(Constants.PT_AS_INSTANT.getURI());
	protected static final Property INTERVAL_PF = ResourceFactory.createProperty(Constants.PT_AS_INTERVAL.getURI());

	protected static final String COMMON_PREFIXES = "PREFIX rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
			+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
			+ "PREFIX owl:  <http://www.w3.org/2002/07/owl#>\n"
			+ "PREFIX xsd:  <http://www.w3.org/2001/XMLSchema#>\n"
			+ "PREFIX snap: <http://www.ifomis.org/bfo/1.0/snap#>\n"
			+ "PREFIX span: <http://www.ifomis.org/bfo/1.0/span#>\n"
			+ "PREFIX time: <"+Constants.OT_NS+">\n"
			+ "PREFIX pt: <"+Constants.PT_NS+">\n";

	protected static KbGraphStore graphStore;
	protected static Model model;
	protected static KbGraph graph;

	@BeforeClass
	public static void setUpKb() {
		JenaParameters.enableEagerLiteralValidation = true;
		graph = KbGraphFactory.createDefaultGraph();
		graphStore = new KbGraphStore(graph);
		graphStore.initialize();
		model = ModelFactory.createModelForGraph(graph);
		model.removeAll();
	}

	@AfterClass
	public static void tearDownKb() {
		model.removeAll();
		model.close();
		graphStore.clear();
	}

	protected TemporalIndex index;
	protected static QueryExecution qExec;
	protected TemporalRecordFactory recordFactory;

	@Before
	public final void setup() {

		Properties properties = new Properties();
		properties.put(Constants.INDEX_TYPE, Constants.INDEX_PERSISTENT);

		TemporalIndexFactory factory = new TemporalIndexFactory();
		factory.configure(properties);
		IndexFactoryRegistry.getInstance().register(factory);

		index = factory.createIndex(graph, null);
		IndexManager.getInstance().register(graph, null, factory, index);
		index.open();
		recordFactory = index.getRecordFactory();
		model.removeAll();
		index.clear();

		for (Iterator<Node> graphNames = graphStore.listGraphNodes(); graphNames.hasNext(); ) {
			Node graphName = graphNames.next();
			if (KbGraphStore.MASTER_GRAPH.equals(graphName.getURI())) {
				continue;
			}
			Graph namedGraph = graphStore.getGraph(graphName);
			TemporalIndex namedIndex = factory.createIndex(namedGraph, graphName);
			IndexManager.getInstance().register(namedGraph, graphName, factory, namedIndex);
			namedIndex.open();
			namedIndex.clear();
		}

		//Method used for any test-specific setup, e.g. for loading test triples into a model
		doSetup();
	}

	public abstract void doSetup();

	@After
	public final void removeIndex() {
		IndexManager.getInstance().unregister(graph, null, index);
	}
}