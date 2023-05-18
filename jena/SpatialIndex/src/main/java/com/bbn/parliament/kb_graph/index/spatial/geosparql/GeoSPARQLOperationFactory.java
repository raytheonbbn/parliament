package com.bbn.parliament.kb_graph.index.spatial.geosparql;

import java.util.HashMap;
import java.util.Map;

import com.bbn.parliament.kb_graph.index.spatial.Operation;
import com.bbn.parliament.kb_graph.index.spatial.OperationFactory;
import com.bbn.parliament.kb_graph.index.spatial.geosparql.vocabulary.Geo;

/** @author rbattle */
public class GeoSPARQLOperationFactory implements OperationFactory {
	private Map<String, Operation> ops;

	public GeoSPARQLOperationFactory() {
		this.ops = createOperationMap();
	}

	private static Map<String, Operation> createOperationMap() {
		Map<String, Operation> ops = new HashMap<>();

		// OGC
		ops.put(Geo.sf_contains.getURI(), Operation.SimpleFeatures.CONTAINS);
		ops.put(Geo.sf_crosses.getURI(), Operation.SimpleFeatures.CROSSES);
		ops.put(Geo.sf_disjoint.getURI(), Operation.SimpleFeatures.DISJOINT);
		ops.put(Geo.sf_equals.getURI(), Operation.SimpleFeatures.EQUALS);
		ops.put(Geo.sf_intersects.getURI(), Operation.SimpleFeatures.INTERSECTS);
		ops.put(Geo.sf_overlaps.getURI(), Operation.SimpleFeatures.OVERLAPS);
		ops.put(Geo.sf_touches.getURI(), Operation.SimpleFeatures.TOUCHES);
		ops.put(Geo.sf_within.getURI(), Operation.SimpleFeatures.WITHIN);

		// EH
		ops.put(Geo.eh_equals.getURI(), Operation.Egenhofer.EQUALS);
		ops.put(Geo.eh_disjoint.getURI(), Operation.Egenhofer.DISJOINT);
		ops.put(Geo.eh_meet.getURI(), Operation.Egenhofer.MEET);
		ops.put(Geo.eh_overlap.getURI(), Operation.Egenhofer.OVERLAP);
		ops.put(Geo.eh_covers.getURI(), Operation.Egenhofer.COVERS);
		ops.put(Geo.eh_coveredBy.getURI(), Operation.Egenhofer.COVEREDBY);
		ops.put(Geo.eh_inside.getURI(), Operation.Egenhofer.INSIDE);
		ops.put(Geo.eh_contains.getURI(), Operation.Egenhofer.CONTAINS);

		// RCC8
		ops.put(Geo.rcc8_dc.getURI(), Operation.RCC8.DC);
		ops.put(Geo.rcc8_eq.getURI(), Operation.RCC8.EQ);
		ops.put(Geo.rcc8_po.getURI(), Operation.RCC8.PO);
		ops.put(Geo.rcc8_ec.getURI(), Operation.RCC8.EC);
		ops.put(Geo.rcc8_tpp.getURI(), Operation.RCC8.TPP);
		ops.put(Geo.rcc8_tppi.getURI(), Operation.RCC8.TPPI);
		ops.put(Geo.rcc8_ntpp.getURI(), Operation.RCC8.NTPP);
		ops.put(Geo.rcc8_ntppi.getURI(), Operation.RCC8.NTPPI);

		return ops;
	}

	/** {@inheritDoc} */
	@Override
	public Operation createOperation(String uri) {
		return ops.get(uri);
	}

	/** {@inheritDoc} */
	@Override
	public String[] getURIs() {
		return ops.keySet().toArray(new String[] {});
	}
}
