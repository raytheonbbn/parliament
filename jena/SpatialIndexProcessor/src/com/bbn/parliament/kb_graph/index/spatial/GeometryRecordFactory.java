package com.bbn.parliament.kb_graph.index.spatial;

import org.apache.jena.graph.Triple;
import org.locationtech.jts.geom.Geometry;

import com.bbn.parliament.kb_graph.index.RecordFactory;

public interface GeometryRecordFactory extends RecordFactory<Geometry> {
	/**
	 * Create a record from a triple.
	 *
	 * @param triple a triple.
	 * @return a record based on information in the triple, or <code>null</code>
	 *         if no record could be made.
	 */
	@Override
	public GeometryRecord createRecord(Triple triple);
}
