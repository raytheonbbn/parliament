package com.bbn.parliament.jena.graph.index.numeric.composite;

import java.util.Properties;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;

import com.bbn.parliament.jena.graph.index.IndexFactory;

/**
 * An <code>IndexFactory</code> for creating {@link CompositeNumericIndex}es.
 *
 * @author rbattle
 */
public class CompositeNumericIndexFactory extends
IndexFactory<CompositeNumericIndex, Number> {

	private static final String LABEL = "Numeric";

	public CompositeNumericIndexFactory() {
		super(LABEL);
	}

	/** {@inheritDoc} */
	@Override
	public void configure(Properties configuration) {
	}

	/** {@inheritDoc} */
	@Override
	public CompositeNumericIndex createIndex(Graph graph, Node graphName,
		String indexDir) {
		CompositeNumericIndex index = new CompositeNumericIndex(graph, graphName,
			indexDir);
		return index;
	}
}
