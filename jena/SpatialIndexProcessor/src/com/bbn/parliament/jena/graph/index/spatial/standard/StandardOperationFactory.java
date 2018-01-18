package com.bbn.parliament.jena.graph.index.spatial.standard;

import java.util.HashMap;
import java.util.Map;

import com.bbn.parliament.jena.graph.index.spatial.Operation;
import com.bbn.parliament.jena.graph.index.spatial.OperationFactory;

/** @author rbattle */
public class StandardOperationFactory implements OperationFactory {
	private Map<String, Operation> ops;

	public StandardOperationFactory() {
		this.ops = createOperationMap();
	}

	private static Map<String, Operation> createOperationMap() {
		Map<String, Operation> ops = new HashMap<>();

		// OGC
		ops.put(Constants.OGC_NS + "contains", Operation.SimpleFeatures.CONTAINS);
		ops.put(Constants.OGC_NS + "crosses", Operation.SimpleFeatures.CROSSES);
		ops.put(Constants.OGC_NS + "disjoint", Operation.SimpleFeatures.DISJOINT);
		ops.put(Constants.OGC_NS + "equals", Operation.SimpleFeatures.EQUALS);
		ops.put(Constants.OGC_NS + "intersects", Operation.SimpleFeatures.INTERSECTS);
		ops.put(Constants.OGC_NS + "overlaps", Operation.SimpleFeatures.OVERLAPS);
		ops.put(Constants.OGC_NS + "touches", Operation.SimpleFeatures.TOUCHES);
		ops.put(Constants.OGC_NS + "within", Operation.SimpleFeatures.WITHIN);

		// OGC Extentions
		ops.put(Constants.OGC_NS + "covers", Operation.Egenhofer.COVERS);
		ops.put(Constants.OGC_NS + "coveredBy", Operation.Egenhofer.COVEREDBY);

		// RCC8
		ops.put(Constants.RCC_NS + "disconnected", Operation.RCC8.DC);
		ops.put(Constants.RCC_NS + "identical", Operation.RCC8.EQ);
		ops.put(Constants.RCC_NS + "partiallyOverlaps", Operation.RCC8.PO);
		ops.put(Constants.RCC_NS + "externallyConnected", Operation.RCC8.EC);
		ops.put(Constants.RCC_NS + "tangentialProperPart", Operation.RCC8.TPP);
		ops.put(Constants.RCC_NS + "invTangentialProperPart", Operation.RCC8.TPPI);
		ops.put(Constants.RCC_NS + "nonTangentialProperPart", Operation.RCC8.NTPP);
		ops.put(Constants.RCC_NS + "invNonTangentialProperPart", Operation.RCC8.NTPPI);

		// RCC Extentions
		ops.put(Constants.RCC_NS + "connected", Operation.RCC_EXT.CONNECTED);
		ops.put(Constants.RCC_NS + "part", Operation.RCC_EXT.PART);
		ops.put(Constants.RCC_NS + "invPart", Operation.RCC_EXT.INV_PART);
		ops.put(Constants.RCC_NS + "properPart", Operation.RCC_EXT.PROPER_PART);
		ops.put(Constants.RCC_NS + "invProperPart", Operation.RCC_EXT.INV_PROPER_PART);

		// Spatial Relations
		ops.put(Constants.SPATIAL_RELATIONS_NS + "contains", Operation.SimpleFeatures.CONTAINS);
		ops.put(Constants.SPATIAL_RELATIONS_NS + "crosses", Operation.SimpleFeatures.CROSSES);
		ops.put(Constants.SPATIAL_RELATIONS_NS + "disjoint", Operation.SimpleFeatures.DISJOINT);
		ops.put(Constants.SPATIAL_RELATIONS_NS + "equals", Operation.SimpleFeatures.EQUALS);
		ops.put(Constants.SPATIAL_RELATIONS_NS + "intersects", Operation.SimpleFeatures.INTERSECTS);
		ops.put(Constants.SPATIAL_RELATIONS_NS + "overlaps", Operation.SimpleFeatures.OVERLAPS);
		ops.put(Constants.SPATIAL_RELATIONS_NS + "touches", Operation.SimpleFeatures.TOUCHES);
		ops.put(Constants.SPATIAL_RELATIONS_NS + "within", Operation.SimpleFeatures.WITHIN);

		// OGC Extentions
		ops.put(Constants.SPATIAL_RELATIONS_NS + "covers", Operation.Egenhofer.COVERS);
		ops.put(Constants.SPATIAL_RELATIONS_NS + "coveredBy", Operation.Egenhofer.COVEREDBY);

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
