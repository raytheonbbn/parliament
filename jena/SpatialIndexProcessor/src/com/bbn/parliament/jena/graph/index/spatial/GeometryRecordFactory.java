package com.bbn.parliament.jena.graph.index.spatial;

import com.bbn.parliament.jena.graph.index.RecordFactory;
import com.hp.hpl.jena.graph.Triple;
import org.locationtech.jts.geom.Geometry;

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
