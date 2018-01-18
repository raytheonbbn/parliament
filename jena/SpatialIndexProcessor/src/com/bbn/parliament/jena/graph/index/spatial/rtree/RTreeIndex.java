package com.bbn.parliament.jena.graph.index.spatial.rtree;

import java.io.File;
import java.util.Iterator;
import java.util.Properties;

import org.deegree.io.rtree.HyperBoundingBox;
import org.deegree.io.rtree.HyperPoint;
import org.deegree.io.rtree.RTree;
import org.deegree.io.rtree.RTreeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jena.graph.index.Record;
import com.bbn.parliament.jena.graph.index.spatial.GeometryRecord;
import com.bbn.parliament.jena.graph.index.spatial.Operation;
import com.bbn.parliament.jena.graph.index.spatial.Profile;
import com.bbn.parliament.jena.graph.index.spatial.SpatialIndex;
import com.bbn.parliament.jena.graph.index.spatial.SpatialIndexException;
import com.bbn.parliament.jena.graph.index.spatial.persistence.NodeData;
import com.bbn.parliament.jena.graph.index.spatial.persistence.NodeKey;
import com.bbn.parliament.jena.util.FileUtil;
import com.bbn.parliament.jena.util.NodeUtil;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.util.iterator.ClosableIterator;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.IntersectionMatrix;
import com.vividsolutions.jts.io.ParseException;

public class RTreeIndex extends SpatialIndex {
	private static final Logger LOG = LoggerFactory.getLogger(RTreeIndex.class);
	private static final int MAX_NODE_LOAD = 50;
	private static final int DATA_DIMENSION = 2;

	private static final String INDEX_FILE_NAME = "spatial.idx";

	private RTree tree;
	private String indexPath;
	private Object lock = new Object();

	public RTreeIndex(Profile profile, Properties configuration, String indexDir) {
		super(profile, configuration, indexDir);
		this.indexPath = this.indexDir + INDEX_FILE_NAME;
	}

	@Override
	protected void indexClose() throws SpatialIndexException {
		try {
			synchronized (lock) {
				if (null != tree) {
					tree.close();
				}
			}
		} catch (RTreeException e) {
			throw new SpatialIndexException(this, e);
		}
		tree = null;
	}

	@Override
	protected void indexOpen() throws SpatialIndexException {
		createTree();
	}

	protected void createTree() throws SpatialIndexException {
		try {
			File f = new File(indexPath);
			if (f.exists()) {
				tree = new RTree(indexPath);
			} else {
				tree = new RTree(DATA_DIMENSION, MAX_NODE_LOAD, indexPath);
			}
		} catch (RTreeException e) {
			throw new SpatialIndexException(this, e);
		}
	}

	@Override
	protected void indexDelete() {
		deleteTree();
	}

	protected void deleteTree() {
		File f = new File(this.indexPath);
		FileUtil.delete(f);
	}

	/** {@inheritDoc} */
	@Override
	protected long estimate(Geometry g, Operation operation) {
		try {
			synchronized (lock) {
				return tree.intersects(createBoundingBox(this, g)).length;
			}
		} catch (RTreeException e) {
			return Long.MAX_VALUE;
		}
	}

	@Override
	protected void indexClear() throws SpatialIndexException {
		try {
			synchronized (lock) {
				tree.close();
			}
		} catch (RTreeException e) {
			throw new SpatialIndexException(this, e);
		}
		deleteTree();
		createTree();
	}

	static HyperBoundingBox createBoundingBox(SpatialIndex index, Geometry g)
		throws SpatialIndexException {
		Geometry targetGeometry = g;
		Envelope e = targetGeometry.getEnvelopeInternal();
		HyperPoint min = new HyperPoint(new double[] { e.getMinX(), e.getMinY() });
		HyperPoint max = new HyperPoint(new double[] { e.getMaxX(), e.getMaxY() });
		return new HyperBoundingBox(min, max);
	}

	/** {@inheritDoc} */
	@Override
	protected boolean indexAdd(Record<Geometry> r) throws SpatialIndexException {
		synchronized (lock) {
			boolean inserted = false;
			try {
				String n = NodeUtil.getStringRepresentation(r.getKey());
				NodeKey key = new NodeKey(n);
				NodeData data = nodes.get(key);
				int id = data.getId();
				inserted = tree.insert(id, createBoundingBox(this, r.getValue()));
			} catch (RTreeException e) {
				throw new SpatialIndexException(this, e);
			}
			return inserted;
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @throws SpatialIndexException
	 *            if an error occurs parsing the geometry from the index
	 */
	@Override
	protected boolean indexRemove(Record<Geometry> r) throws SpatialIndexException {
		synchronized (lock) {
			String n = NodeUtil.getStringRepresentation(r.getKey());
			NodeKey key = new NodeKey(n);
			NodeData data = nodes.get(key);
			if (null == data) {
				LOG.error("Could not find id for: " + r.getKey() + " (" + n + ")");
				return false;
			}

			int id = data.getId();
			Geometry g;
			try {
				g = getGeometryRepresentation(data);
			} catch (ParseException e) {
				throw new SpatialIndexException(this, e);
			}
			if (!g.equals(r.getValue())) {
				LOG.error("The value for {} is different from the value to be deleted",
					n);
				return false;
			}
			try {
				return tree.delete(createBoundingBox(this, g), id);
			} catch (RTreeException e) {
				throw new SpatialIndexException(this, e);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @throws SpatialIndexException
	 *            if an error occurs querying the index or parsing a geometry.
	 */
	@Override
	public Iterator<Record<Geometry>> query(final Geometry value)
		throws SpatialIndexException {
		return iterator(value, Operation.SimpleFeatures.EQUALS);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @throws SpatialIndexException
	 *            if an error occurs querying the index or parsing the geometry
	 */
	@Override
	public Iterator<Record<Geometry>> iterator(final Geometry value,
		final Operation operation) throws SpatialIndexException {

		final Iterator<Record<Geometry>> records;

		boolean isIntersection = false;

		for (IntersectionMatrix m : operation.getIntersectionMatrices()) {
			if (m.isIntersects()) {
				isIntersection = true;
				break;
			}
		}

		if (isIntersection) {
			final Object[] objs;
			try {
				synchronized (lock) {
					objs = tree.intersects(createBoundingBox(this, value));
				}
			} catch (RTreeException e) {
				throw new SpatialIndexException(this, e);
			}

			records = new RecordIterator(objs);
		} else {
			records = this.iterator();
			// TODO: Perform intersection and skip those ids
		}
		return new Operation.OperationIterator(records, value, operation);
	}

	private class RecordIterator implements ClosableIterator<Record<Geometry>> {
		private Object[] ids;
		private int pos;

		public RecordIterator(Object[] ids) {
			this.ids = ids;
			this.pos = 0;
		}

		@Override
		public boolean hasNext() {
			return pos < this.ids.length;
		}

		@Override
		public Record<Geometry> next() {
			int id = (Integer) ids[pos++];
			NodeData data = idNodes.get(id);
			Node n = NodeUtil.getNodeRepresentation(data.getNode());
			Geometry g;
			try {
				g = getGeometryRepresentation(data);
			} catch (ParseException e) {
				throw new RuntimeException(e);
			}
			return GeometryRecord.create(n, g);
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void close() {
			ids = null;
		}
	}
}
