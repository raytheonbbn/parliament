package com.bbn.parliament.jena.graph.index.spatial.rtree;

import org.locationtech.jts.geom.Geometry;

import com.bbn.parliament.jena.graph.index.spatial.SpatialIndex;
import com.bbn.parliament.jena.graph.index.spatial.SpatialIndexFactory;
import com.bbn.parliament.jena.graph.index.spatial.SpatialIndexTestMethods;
import com.bbn.parliament.kb_graph.index.IndexFactory;

public class RTreeIndexTestMethods extends SpatialIndexTestMethods {
	@Override
	protected IndexFactory<SpatialIndex, Geometry> getIndexFactory() {
		SpatialIndexFactory f = new SpatialIndexFactory();
		f.configure(RTreePropertyFactory.create());
		return f;
	}
}
