package com.bbn.parliament.jena.graph.index.spatial.jts;

import com.bbn.parliament.jena.graph.index.IndexFactory;
import com.bbn.parliament.jena.graph.index.spatial.SpatialIndex;
import com.bbn.parliament.jena.graph.index.spatial.SpatialIndexFactory;
import com.bbn.parliament.jena.graph.index.spatial.SpatialIndexTestMethods;
import org.locationtech.jts.geom.Geometry;

public class JTSIndexTestMethods extends SpatialIndexTestMethods {
	@Override
	protected IndexFactory<SpatialIndex, Geometry> getIndexFactory() {
		SpatialIndexFactory f = new SpatialIndexFactory();
		f.configure(JTSPropertyFactory.create());
		return f;
	}
}
