// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2018, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.graph.index.spatial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;

import com.bbn.parliament.jena.graph.index.IndexFactory.IndexFactoryHelper;
import com.bbn.parliament.jena.graph.index.Record;
import com.bbn.parliament.jena.query.index.QueryableIndexTestMethods;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

public abstract class SpatialIndexTestMethods extends QueryableIndexTestMethods<SpatialIndex, Geometry> {
	@Override
	protected Record<Geometry> createRecord(int seed) {
		Geometry extent = GeometryConverter.createPoint(new double[] { 53.2 + seed, 23.1 });
		Node n = Node.createURI("http://example.org/" + seed);
		return GeometryRecord.create(n, extent);
	}

	@Override
	protected void doSetup() {
	}

	/** {@inheritDoc} */
	@Override
	protected boolean checkDeleted(SpatialIndex index, Graph graph, Node graphName) {
		File indexDir = new File(IndexFactoryHelper.getIndexDirectory(graph, graphName), "spatial");
		return !indexDir.exists();
	}

	// Test method
	@SuppressWarnings("static-method")
	public void testAddGeometry(SpatialIndex index) {
		Node n = Node.createURI("http://test.org#test");
		Geometry extent = GeometryConverter
			.createPoint(new double[] { 1.0, 1.0 });
		// test add node
		try {
			index.add(Record.create(n, extent));
		} catch (SpatialIndexException ex) {
			fail(ex.getMessage());
		}
		assertEquals(1, index.size());

		// test add another node
		n = Node.createURI("http://test.org#test2");
		extent = GeometryConverter.createPoint(new double[] { 1.0, 2.0 });
		try {
			index.add(Record.create(n, extent));
		} catch (SpatialIndexException ex) {
			fail(ex.getMessage());
		}
		assertEquals(2, index.size());
	}

	// Test method
	@SuppressWarnings("static-method")
	public void testAddSameNode(SpatialIndex index) {
		// test add another node
		Node n = Node.createURI("http://test.org#test2");
		Geometry extent = GeometryConverter
			.createPoint(new double[] { 1.0, 2.0 });

		// test add same node twice
		try {
			index.add(Record.create(n, extent));
		} catch (SpatialIndexException ex) {
			fail(ex.getMessage());
		}
		assertEquals(1, index.size());

		// test add same node twice
		try {
			index.add(Record.create(n, extent));
		} catch (SpatialIndexException ex) {
			fail(ex.getMessage());
		}
		assertEquals(1, index.size());

		// test add same node, different extent
		Geometry extent1 = GeometryConverter
			.createPoint(new double[] { 1.0, 3.0 });
		try {
			index.add(Record.create(n, extent1));
		} catch (SpatialIndexException ex) {
			fail(ex.getMessage());
		}
		assertEquals(1, index.size());

		Record<Geometry> result = index.find(n);
		assertNotNull(result);
		Point test = (Point) result.getValue();
		assertTrue(extent1.equals(test));
	}
}
