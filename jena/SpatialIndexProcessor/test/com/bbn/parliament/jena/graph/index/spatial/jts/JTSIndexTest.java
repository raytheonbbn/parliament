// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.graph.index.spatial.jts;

import java.util.Properties;

import com.bbn.parliament.jena.graph.index.IndexFactory;
import com.bbn.parliament.jena.graph.index.spatial.AbstractIndexTest;
import com.bbn.parliament.jena.graph.index.spatial.SpatialIndex;
import com.bbn.parliament.jena.graph.index.spatial.SpatialIndexFactory;
import com.vividsolutions.jts.geom.Geometry;

/** @author Robert Battle */
public class JTSIndexTest extends AbstractIndexTest {
	@SuppressWarnings("static-method")
	protected Properties getProperties() {
		return JTSPropertyFactory.create();
	}

	@Override
	protected IndexFactory<SpatialIndex, Geometry> getIndexFactory() {
		SpatialIndexFactory f = new SpatialIndexFactory();
		f.configure(getProperties());
		return f;
	}
}
