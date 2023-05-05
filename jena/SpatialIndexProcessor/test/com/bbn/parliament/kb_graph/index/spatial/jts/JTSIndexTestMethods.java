package com.bbn.parliament.kb_graph.index.spatial.jts;

import org.locationtech.jts.geom.Geometry;

import com.bbn.parliament.kb_graph.index.spatial.SpatialIndex;
import com.bbn.parliament.kb_graph.index.spatial.SpatialIndexFactory;
import com.bbn.parliament.kb_graph.index.spatial.SpatialIndexTestMethods;
import com.bbn.parliament.kb_graph.index.IndexFactory;

public class JTSIndexTestMethods extends SpatialIndexTestMethods {
	@Override
	protected IndexFactory<SpatialIndex, Geometry> getIndexFactory() {
		SpatialIndexFactory f = new SpatialIndexFactory();
		f.configure(JTSPropertyFactory.create());
		return f;
	}
}
