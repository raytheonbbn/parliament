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
		// TODO Auto-generated method stub
	}

	/** {@inheritDoc} */
	@Override
	public void apply(CoordinateSequenceFilter filter) {
		// TODO Auto-generated method stub
	}

	/** {@inheritDoc} */
	@Override
	public void apply(GeometryFilter filter) {
		// TODO Auto-generated method stub
	}

	/** {@inheritDoc} */
	@Override
	public void apply(GeometryComponentFilter filter) {
		// TODO Auto-generated method stub
	}

	/** {@inheritDoc} */
	@Override
	protected int compareToSameClass(Object o) {
		// TODO Auto-generated method stub
		return 0;
	}

	/** {@inheritDoc} */
	@Override
	protected int compareToSameClass(Object o, CoordinateSequenceComparator comp) {
		// TODO Auto-generated method stub
		return 0;
	}

	/** {@inheritDoc} */
	@Override
	protected Envelope computeEnvelopeInternal() {
		// TODO Auto-generated method stub
		return null;
	}

	/** {@inheritDoc} */
	@Override
	public boolean equalsExact(Geometry other, double tolerance) {
		// TODO Auto-generated method stub
		return false;
	}

	/** {@inheritDoc} */
	@Override
	public Geometry getBoundary() {
		// TODO Auto-generated method stub
		return null;
	}

	/** {@inheritDoc} */
	@Override
	public int getBoundaryDimension() {
		// TODO Auto-generated method stub
		return 0;
	}

	/** {@inheritDoc} */
	@Override
	public Coordinate getCoordinate() {
		// TODO Auto-generated method stub
		return null;
	}

	/** {@inheritDoc} */
	@Override
	public Coordinate[] getCoordinates() {
		// TODO Auto-generated method stub
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
		// TODO Auto-generated method stub
		return false;
	}

	/** {@inheritDoc} */
	@Override
	public void normalize() {
		// TODO Auto-generated method stub
	}

	/** {@inheritDoc} */
	@Override
	public String getGeometryType() {
		// TODO Auto-generated method stub
		return null;
	}

	/** {@inheritDoc} */
	@Override
	public int getNumPoints() {
		// TODO Auto-generated method stub
		return 0;
	}

	/** {@inheritDoc} */
	@Override
	public Geometry reverse() {
		// TODO Auto-generated method stub
		return null;
	}
}
