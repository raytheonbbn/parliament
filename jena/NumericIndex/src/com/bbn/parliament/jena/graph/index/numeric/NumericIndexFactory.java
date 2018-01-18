/**
 *
 */

package com.bbn.parliament.jena.graph.index.numeric;

import java.io.Serializable;
import java.util.Properties;
import com.bbn.parliament.jena.graph.index.IndexFactory;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;

/**
 * Factory for creating {@link NumericIndex}es.
 *
 * @author rbattle
 */
public abstract class NumericIndexFactory<T extends Number & Serializable & Comparable<T>>
extends IndexFactory<NumericIndex<T>, T> {

	/**
	 * Construct a new instance with the given label.
	 *
	 * @param label the label of the factory.
	 */
	public NumericIndexFactory(String label) {
		super(label);
	}

	/** The predicate to index. */
	protected String predicate;

	/** {@inheritDoc} */
	@Override
	public void configure(Properties configuration) {
		setPredicate(configuration.getProperty(Constants.PROPERTY));
	}

	/**
	 * Set the predicate.
	 *
	 * @param predicate the predicate to index.
	 */
	public void setPredicate(String predicate) {
		this.predicate = predicate;
	}

	/** Index factory for <code>Integer</code>s. */
	public static class IntegerIndexFactory extends NumericIndexFactory<Integer> {

		public IntegerIndexFactory() {
			super("Integer");
		}

		/** {@inheritDoc} */
		@Override
		public NumericIndex<Integer> createIndex(Graph graph, Node graphName,
			String indexDir) {
			return new NumericIndex.IntegerIndex(predicate, indexDir);
		}
	}

	/** Index factory for <code>Long</code>s. */
	public static class LongIndexFactory extends NumericIndexFactory<Long> {

		public LongIndexFactory() {
			super("Long");
		}

		/** {@inheritDoc} */
		@Override
		public NumericIndex<Long> createIndex(Graph graph, Node graphName,
			String indexDir) {
			return new NumericIndex.LongIndex(predicate, indexDir);
		}
	}

	/** Index factory for <code>Double</code>s. */
	public static class DoubleIndexFactory extends NumericIndexFactory<Double> {

		public DoubleIndexFactory() {
			super("Double");
		}

		/** {@inheritDoc} */
		@Override
		public NumericIndex<Double> createIndex(Graph graph, Node graphName,
			String indexDir) {
			return new NumericIndex.DoubleIndex(predicate, indexDir);
		}
	}

	/** Index factory for <code>Float</code>s. */
	public static class FloatIndexFactory extends NumericIndexFactory<Float> {

		public FloatIndexFactory() {
			super("Float");
		}

		/** {@inheritDoc} */
		@Override
		public NumericIndex<Float> createIndex(Graph graph, Node graphName,
			String indexDir) {
			return new NumericIndex.FloatIndex(predicate, indexDir);
		}
	}
}
