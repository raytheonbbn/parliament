// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.graph.index.spatial.jts;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.bbn.parliament.jena.graph.index.Record;
import com.bbn.parliament.jena.graph.index.spatial.GeometryRecord;
import com.bbn.parliament.jena.graph.index.spatial.Operation;
import com.bbn.parliament.jena.graph.index.spatial.Profile;
import com.bbn.parliament.jena.graph.index.spatial.SpatialIndexException;
import com.hp.hpl.jena.graph.Node;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.index.SpatialIndex;
import org.locationtech.jts.index.quadtree.Quadtree;

/** @author Robert Battle */
public class JTSIndex extends com.bbn.parliament.jena.graph.index.spatial.SpatialIndex {
	private SpatialIndex index;
	private Map<String, List<Node>> extentsToNodes;

	private Object indexLock = new Object();

	public JTSIndex(Profile profile,
		Properties configuration, String indexDir) {
		super(profile, configuration, indexDir);
		index = new Quadtree();
		extentsToNodes = new HashMap<>();
		initialize(configuration, indexDir);
	}

	private static void initialize(Properties properties, String indexDir) {
		File indexFile = new File(indexDir);
		if (!indexFile.exists()) {
			indexFile.mkdirs();
		}
	}

	public SpatialIndex getIndex() {
		return index;
	}

	@Override
	protected void indexOpen() {
		synchronized (indexLock) {
			Iterator<Record<Geometry>> it = doIterator();
			while (it.hasNext()) {
				Record<Geometry> r = it.next();
				indexAdd(r);
			}
		}
	}

	@Override
	protected void indexClose() throws SpatialIndexException {
	}

	@Override
	protected void indexDelete() {
	}

	/** {@inheritDoc} */
	@Override
	protected void indexClear() {
		synchronized (indexLock) {
			extentsToNodes.clear();
			index = null;
			index = new Quadtree();
		}
	}

	/** {@inheritDoc} */
	@Override
	protected boolean indexAdd(Record<Geometry> r) {
		Node node = r.getKey();
		Geometry extent = r.getValue();
		synchronized (indexLock) {
			index.insert(extent.getEnvelopeInternal(), extent);
			extentsToNodes.computeIfAbsent(extent.toText(), k -> new ArrayList<>()).add(node);
			return true;
		}
	}

	/** {@inheritDoc} */
	@Override
	protected boolean indexRemove(Record<Geometry> r) {
		boolean result = false;
		synchronized (indexLock) {
			Record<Geometry> record = find(r.getKey());
			if (null != record) {
				Geometry geom = record.getValue();
				extentsToNodes.remove(geom.toText());
				index.remove(geom.getEnvelopeInternal(), geom);
				result = true;
			}
		}
		return result;
	}

	/** {@inheritDoc} */
	@Override
	public Iterator<Record<Geometry>> iterator(final Geometry geometry, final Operation operation) {
		List<Geometry> i = index.query(geometry.getEnvelopeInternal());
		final Iterator<Geometry> items = i.iterator();

		Iterator<Record<Geometry>> it = new Iterator<Record<Geometry>>() {
			private Iterator<Node> nit = null;
			private Geometry current = null;
			private boolean hasBeenNexted = true;
			private boolean hasNextValue = false;

			@Override
			public boolean hasNext() {
				if (!hasBeenNexted) {
					return hasNextValue;
				}

				if (null == nit || !nit.hasNext()) {
					while (items.hasNext()) {
						Geometry t = items.next();
						if (operation.relate(t, geometry)) {
							current = t;
							List<Node> ns = extentsToNodes.get(current.toText());
							nit = ns.iterator();
							break;
						}
					}
				}
				if (null == nit) {
					hasNextValue = false;
					current = null;
				} else {
					hasNextValue = nit.hasNext();
				}
				hasBeenNexted = false;
				return hasNextValue;
			}

			@Override
			public Record<Geometry> next() {
				if (hasBeenNexted) {
					if (!hasNext()) {
						throw new RuntimeException("No more items");
					}
				}
				Node n = nit.next();
				hasBeenNexted = true;
				return GeometryRecord.create(n, current);
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
		return it;
	}

	/** {@inheritDoc} */
	@Override
	public Iterator<Record<Geometry>> query(final Geometry value) {
		List<Node> nodesForExtent = extentsToNodes.get(value.toText());
		if (null == nodesForExtent) {
			nodesForExtent = Collections.emptyList();
		}

		final Iterator<Node> nit = nodesForExtent.iterator();

		Iterator<Record<Geometry>> it = new Iterator<Record<Geometry>>() {
			@Override
			public boolean hasNext() {
				return nit.hasNext();
			}

			@Override
			public Record<Geometry> next() {
				Node n = nit.next();
				return GeometryRecord.create(n, value);
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
		return it;
	}

	@Override
	protected long estimate(Geometry geometry, Operation operation)
		throws SpatialIndexException {
		return getIndex().query(geometry.getEnvelopeInternal()).size();
	}
}
