package com.bbn.parliament.jena.graph.index.spatial.rtree;

import com.bbn.parliament.jena.graph.index.IndexFactory;
import com.bbn.parliament.jena.graph.index.spatial.SpatialIndex;
import com.bbn.parliament.jena.graph.index.spatial.SpatialIndexFactory;
import com.bbn.parliament.jena.graph.index.spatial.SpatialIndexTestMethods;
import com.vividsolutions.jts.geom.Geometry;

public class RTreeIndexTestMethods extends SpatialIndexTestMethods {
	@Override
	protected IndexFactory<SpatialIndex, Geometry> getIndexFactory() {
		SpatialIndexFactory f = new SpatialIndexFactory();
		f.configure(RTreePropertyFactory.create());
		return f;
	}
}
