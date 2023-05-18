// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.kb_graph.index.temporal.memory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;

import com.bbn.parliament.kb_graph.index.IndexException;
import com.bbn.parliament.kb_graph.index.Record;
import com.bbn.parliament.kb_graph.index.temporal.TemporalIndex;
import com.bbn.parliament.kb_graph.index.temporal.TemporalPropertyFunctionFactory;
import com.bbn.parliament.kb_graph.index.temporal.extent.TemporalExtent;
import com.bbn.parliament.kb_graph.index.temporal.extent.TemporalInstant;
import com.bbn.parliament.kb_graph.index.temporal.extent.TemporalInterval;

/** @author dkolas */
public class MemoryTemporalIndex extends TemporalIndex {
	private TreeSet<TemporalInstant> instants;
	private Map<Node, TemporalExtent> nodesToExtents;
	private Map<TemporalExtent, List<Node>> extentsToNodes;

	public MemoryTemporalIndex(Graph graph, Properties configuration) {
		super(graph, configuration);
		instants = new TreeSet<>();
		nodesToExtents = new HashMap<>();
		extentsToNodes = new HashMap<>();
	}

	public Iterator<TemporalInstant> beforeFinishInclusive(TemporalExtent extent) {
		if (extent instanceof TemporalInterval tempInt) {
			extent = tempInt.getEnd().createLargerInstant();
		}
		return instants.headSet((TemporalInstant) extent).iterator();
	}

	public Iterator<TemporalInstant> beforeFinish(TemporalExtent extent) {
		if (extent instanceof TemporalInterval tempInt) {
			extent = tempInt.getEnd().createSmallerInstant();
		}
		return instants.headSet((TemporalInstant) extent).iterator();
	}

	public Iterator<TemporalInstant> beforeStartInclusive(TemporalExtent extent) {
		if (extent instanceof TemporalInterval tempInt) {
			extent = tempInt.getStart().createLargerInstant();
		}
		return instants.headSet((TemporalInstant) extent).iterator();
	}

	public Iterator<TemporalInstant> beforeStart(TemporalExtent extent) {
		if (extent instanceof TemporalInterval tempInt) {
			extent = tempInt.getStart().createSmallerInstant();
		}
		return instants.headSet((TemporalInstant) extent).iterator();
	}

	public Iterator<TemporalInstant> afterFinishInclusive(TemporalExtent extent) {
		if (extent instanceof TemporalInterval tempInt) {
			extent = tempInt.getEnd().createSmallerInstant();
		}
		return instants.tailSet((TemporalInstant) extent).iterator();
	}

	public Iterator<TemporalInstant> afterFinish(TemporalExtent extent) {
		if (extent instanceof TemporalInterval tempInt) {
			extent = tempInt.getEnd().createLargerInstant();
		}
		return instants.tailSet((TemporalInstant) extent).iterator();
	}

	public Iterator<TemporalInstant> afterStart(TemporalExtent extent) {
		if (extent instanceof TemporalInterval tempInt) {
			extent = tempInt.getStart().createLargerInstant();
		}
		return instants.tailSet((TemporalInstant) extent).iterator();
	}

	public Iterator<TemporalInstant> afterStartInclusive(TemporalExtent extent) {
		if (extent instanceof TemporalInterval tempInt) {
			extent = tempInt.getStart().createSmallerInstant();
		}
		return instants.tailSet((TemporalInstant) extent).iterator();
	}

	public Map<Node, TemporalExtent> getNodesToExtents() {
		return nodesToExtents;
	}

	public Map<TemporalExtent, List<Node>> getExtentsToNodes() {
		return extentsToNodes;
	}

	@Override
	public long size() {
		return nodesToExtents.size();
	}

	/**
	 * @see com.bbn.parliament.kb_graph.index.temporal.TemporalIndex#clear()
	 */
	@Override
	public void clear() {
		instants = new TreeSet<>();
		nodesToExtents = new HashMap<>();
		extentsToNodes = new HashMap<>();
	}

	@Override
	public Iterator<Record<TemporalExtent>> iterator() {
		return new AllNodeExtentWrapper();
	}

	private class AllNodeExtentWrapper implements
	Iterator<Record<TemporalExtent>> {
		private Iterator<Node> internal;

		public AllNodeExtentWrapper() {
			internal = nodesToExtents.keySet().iterator();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean hasNext() {
			return internal.hasNext();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Record<TemporalExtent> next() {
			Node node = internal.next();
			return Record.create(node, nodesToExtents.get(node));
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void remove() {
			throw new UnsupportedOperationException("No remove on this iterator!");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close() {
		clear();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void delete() {

	}
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isClosed() {
		return false;
	}
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void open() {

	}

	@Override
	public boolean add(Record<TemporalExtent> r) {
		Node node = r.getKey();
		TemporalExtent extent = r.getValue();

		if (!nodesToExtents.containsKey(node)) {
			nodesToExtents.put(node, extent);
			List<Node> nodes = extentsToNodes.get(extent);
			if (null == nodes) {
				nodes = new ArrayList<>();
				extentsToNodes.put(extent, nodes);
			}
			nodes.add(node);
			if (extent instanceof TemporalInstant) {
				instants.add((TemporalInstant) extent);
			} else if (extent instanceof TemporalInterval tempInt) {
				instants.add(tempInt.getStart());
				instants.add(tempInt.getEnd());
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean remove(Record<TemporalExtent> r) {
		Node node = r.getKey();

		TemporalExtent extent = nodesToExtents.get(node);
		if (null == extent) {
			return false;
		}

		extentsToNodes.remove(extent);
		instants.remove(extent.getStart());
		instants.remove(extent.getEnd());
		nodesToExtents.remove(node);
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Record<TemporalExtent> find(Node node) {
		TemporalExtent extent = nodesToExtents.get(node);
		if (null == extent) {
			return null;
		}
		return Record.create(node, extent);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Iterator<Record<TemporalExtent>> query(final TemporalExtent value) {
		List<Node> nodes = extentsToNodes.get(value);
		if (null == nodes) {
			nodes = Collections.emptyList();
		}
		final Iterator<Node> nit = nodes.iterator();
		return new Iterator<>() {

			@Override
			public boolean hasNext() {
				return nit.hasNext();
			}

			@Override
			public Record<TemporalExtent> next() {
				Node n = nit.next();
				return Record.create(n, value);
			}

			@Override
			public void remove() {

			}

		};
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected TemporalPropertyFunctionFactory<MemoryTemporalIndex> getPropertyFunctionFactory() {
		return new MemoryPropertyFunctionFactory();
	}

	@Override
	public void add(Iterator<Record<TemporalExtent>> records)
		throws IndexException {
		while (records.hasNext()) {
			add(records.next());
		}
	}

	@Override
	public void remove(Iterator<Record<TemporalExtent>> records)
		throws IndexException {
		while (records.hasNext()) {
			remove(records.next());
		}
	}
}
