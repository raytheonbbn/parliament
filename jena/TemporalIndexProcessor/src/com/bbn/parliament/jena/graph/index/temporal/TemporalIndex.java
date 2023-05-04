// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
package com.bbn.parliament.jena.graph.index.temporal;

import java.util.Properties;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.pfunction.PropertyFunctionRegistry;

import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalExtent;
import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalRecordFactory;
import com.bbn.parliament.kb_graph.index.IndexException;
import com.bbn.parliament.kb_graph.index.QueryableIndex;
import com.bbn.parliament.kb_graph.query.index.QueryCache;
import com.bbn.parliament.kb_graph.query.index.pfunction.IndexPropertyFunctionFactory;

public abstract class TemporalIndex implements QueryableIndex<TemporalExtent> {
	protected TemporalRecordFactory recordFactory;
	protected Graph graph;
	protected Properties configuration;
	protected QueryCache<TemporalExtent> cache;
	protected boolean alwaysUseFirst = false;

	public TemporalIndex(Graph graph, Properties configuration) {
		this.graph = graph;
		this.configuration = configuration;
		recordFactory = new TemporalRecordFactory(graph, this);
		cache = new QueryCache<>(Constants.QUERY_CACHE_SIZE);
		alwaysUseFirst = Boolean.parseBoolean(configuration.getProperty(Constants.ALWAYS_USE_FIRST, "false"));
	}

	/** {@inheritDoc} */
	@Override
	public TemporalRecordFactory getRecordFactory() {
		return recordFactory;
	}

	public Graph getGraph() {
		return graph;
	}

	/** {@inheritDoc} */
	@Override
	public QueryCache<TemporalExtent> getQueryCache() {
		return cache;
	}

	protected abstract IndexPropertyFunctionFactory<TemporalExtent> getPropertyFunctionFactory();

	/** {@inheritDoc} */
	@Override
	public void register(@SuppressWarnings("hiding") Graph graph, Node graphName) {
		IndexPropertyFunctionFactory<TemporalExtent> factory = getPropertyFunctionFactory();
		PropertyFunctionRegistry registry = PropertyFunctionRegistry.get();
		for (Operand op : Operand.values()) {
			registry.put(op.getUri(), factory);
		}
	}

	/** {@inheritDoc} */
	@Override
	public void unregister(@SuppressWarnings("hiding") Graph graph, Node graphName) {
		PropertyFunctionRegistry registry = PropertyFunctionRegistry.get();
		for (Operand op : Operand.values()) {
			registry.remove(op.getUri());
		}
	}

	@Override
	public void flush() throws IndexException {
		close();
		open();
	}
}
