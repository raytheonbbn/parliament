// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.kb_graph.index.spatial.standard.data;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateFilter;
import org.locationtech.jts.geom.CoordinateSequenceComparator;
import org.locationtech.jts.geom.CoordinateSequenceFilter;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryComponentFilter;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.GeometryFilter;

/** @author Robert Battle */
public abstract class EphemeralGeometry extends Geometry {
	private static final long serialVersionUID = 1L;

	private Node _blankNode;

	public EphemeralGeometry(GeometryFactory factory) {
		super(factory);
		_blankNode = NodeFactory.createBlankNode();
	}

	public Node getExtentBlankNode() {
		return _blankNode;
	}

	/** {@inheritDoc} */
	@Override
	public void apply(CoordinateFilter filter) {
	}

	/** {@inheritDoc} */
	@Override
	public void apply(CoordinateSequenceFilter filter) {
	}

	/** {@inheritDoc} */
	@Override
	public void apply(GeometryFilter filter) {
	}

	/** {@inheritDoc} */
	@Override
	public void apply(GeometryComponentFilter filter) {
	}

	/** {@inheritDoc} */
	@Override
	protected int compareToSameClass(Object o) {
		return 0;
	}

	/** {@inheritDoc} */
	@Override
	protected int compareToSameClass(Object o, CoordinateSequenceComparator comp) {
		return 0;
	}

	/** {@inheritDoc} */
	@Override
	protected Envelope computeEnvelopeInternal() {
		return null;
	}

	/** {@inheritDoc} */
	@Override
	public boolean equalsExact(Geometry other, double tolerance) {
		return false;
	}

	/** {@inheritDoc} */
	@Override
	public Geometry getBoundary() {
		return null;
	}

	/** {@inheritDoc} */
	@Override
	public int getBoundaryDimension() {
		return 0;
	}

	/** {@inheritDoc} */
	@Override
	public Coordinate getCoordinate() {
		return null;
	}

	/** {@inheritDoc} */
	@Override
	public Coordinate[] getCoordinates() {
		return null;
	}

	/** {@inheritDoc} */
	@Override
	public int getDimension() {
		return 2;
	}

	/** {@inheritDoc} */
	@Override
	public boolean isEmpty() {
		return false;
	}

	/** {@inheritDoc} */
	@Override
	public void normalize() {
	}

	/** {@inheritDoc} */
	@Override
	public String getGeometryType() {
		return null;
	}

	/** {@inheritDoc} */
	@Override
	public int getNumPoints() {
		return 0;
	}

	/** {@inheritDoc} */
	@Override
	public Geometry reverse() {
		return null;
	}
}
