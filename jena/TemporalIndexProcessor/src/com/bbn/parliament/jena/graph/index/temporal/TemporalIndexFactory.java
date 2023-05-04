package com.bbn.parliament.jena.graph.index.temporal;

import java.util.Properties;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;

import com.bbn.parliament.jena.graph.index.temporal.bdb.PersistentTemporalIndex;
import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalExtent;
import com.bbn.parliament.jena.graph.index.temporal.memory.MemoryTemporalIndex;
import com.bbn.parliament.kb_graph.index.IndexFactory;
import com.bbn.parliament.kb_graph.query.PrefixRegistry;

/** @author rbattle */
public class TemporalIndexFactory extends IndexFactory<TemporalIndex, TemporalExtent> {
	private static final String LABEL = "Temporal Index";

	private Properties configuration;

	public TemporalIndexFactory() {
		super(LABEL);
		configuration = new Properties();
		configuration.put(Constants.INDEX_TYPE, Constants.INDEX_PERSISTENT);
		PrefixRegistry.getInstance().registerPrefix("time", Constants.OT_NS);
	}

	/** {@inheritDoc} */
	@Override
	public TemporalIndex createIndex(Graph graph, Node graphName, String indexDir) {
		Properties props = new Properties();
		props.putAll(configuration);
		String indexType = props.getProperty(Constants.INDEX_TYPE);

		TemporalIndex index = null;
		if (Constants.INDEX_PERSISTENT.equals(indexType)) {
			index = new PersistentTemporalIndex(graph, props, indexDir);
		} else {
			index = new MemoryTemporalIndex(graph, props);
		}

		return index;
	}

	/**{@inheritDoc} */
	@Override
	public void configure(Properties additionalConfiguration) {
		this.configuration.putAll(additionalConfiguration);
	}

	/** {@inheritDoc} */
	@Override
	public String getLabel() {
		return LABEL;
	}
}
