package com.bbn.parliament.kb_graph.index.temporal;

import java.util.Properties;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;

import com.bbn.parliament.kb_graph.index.IndexFactory;
import com.bbn.parliament.kb_graph.index.temporal.bdb.PersistentTemporalIndex;
import com.bbn.parliament.kb_graph.index.temporal.extent.TemporalExtent;
import com.bbn.parliament.kb_graph.index.temporal.memory.MemoryTemporalIndex;
import com.bbn.parliament.kb_graph.query.PrefixRegistry;

/** @author rbattle */
public class TemporalIndexFactory extends IndexFactory<TemporalIndex, TemporalExtent> {
	private static final String LABEL = "Temporal Index";

	private Properties config;

	public TemporalIndexFactory() {
		super(LABEL);
		config = new Properties();
		config.put(Constants.INDEX_TYPE, Constants.INDEX_PERSISTENT);
		PrefixRegistry.getInstance().registerPrefix("time", Constants.TIME_NS);
	}

	/** {@inheritDoc} */
	@Override
	public TemporalIndex createIndex(Graph graph, Node graphName, String indexDir) {
		Properties props = new Properties();
		props.putAll(config);
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
	public void configure(Properties configuration) {
		config.putAll(configuration);
	}

	/** {@inheritDoc} */
	@Override
	public String getLabel() {
		return LABEL;
	}
}