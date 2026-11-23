package com.bbn.parliament.kb_graph.index.spatial.standard;

import java.util.HashMap;
import java.util.Map;

import com.bbn.parliament.kb_graph.index.spatial.Operation;
import com.bbn.parliament.kb_graph.index.spatial.OperationFactory;

/** @author rbattle */
public class StandardOperationFactory implements OperationFactory {
	private Map<String, Operation> ops;

	public StandardOperationFactory() {
		this.ops = createOperationMap();
	}

	private static Map<String, Operation> createOperationMap() {
		Map<String, Operation> ops = new HashMap<>();

		// OGC
		ops.put(StdConstants.OGC_EXT_NS + "contains", Operation.SimpleFeatures.CONTAINS);
		ops.put(StdConstants.OGC_EXT_NS + "crosses", Operation.SimpleFeatures.CROSSES);
		ops.put(StdConstants.OGC_EXT_NS + "disjoint", Operation.SimpleFeatures.DISJOINT);
		ops.put(StdConstants.OGC_EXT_NS + "equals", Operation.SimpleFeatures.EQUALS);
		ops.put(StdConstants.OGC_EXT_NS + "intersects", Operation.SimpleFeatures.INTERSECTS);
		ops.put(StdConstants.OGC_EXT_NS + "overlaps", Operation.SimpleFeatures.OVERLAPS);
		ops.put(StdConstants.OGC_EXT_NS + "touches", Operation.SimpleFeatures.TOUCHES);
		ops.put(StdConstants.OGC_EXT_NS + "within", Operation.SimpleFeatures.WITHIN);

		// OGC Extentions
		ops.put(StdConstants.OGC_EXT_NS + "covers", Operation.Egenhofer.COVERS);
		ops.put(StdConstants.OGC_EXT_NS + "coveredBy", Operation.Egenhofer.COVEREDBY);

		// RCC8
		ops.put(StdConstants.RCC_EXT_NS + "disconnected", Operation.RCC8.DC);
		ops.put(StdConstants.RCC_EXT_NS + "identical", Operation.RCC8.EQ);
		ops.put(StdConstants.RCC_EXT_NS + "partiallyOverlaps", Operation.RCC8.PO);
		ops.put(StdConstants.RCC_EXT_NS + "externallyConnected", Operation.RCC8.EC);
		ops.put(StdConstants.RCC_EXT_NS + "tangentialProperPart", Operation.RCC8.TPP);
		ops.put(StdConstants.RCC_EXT_NS + "invTangentialProperPart", Operation.RCC8.TPPI);
		ops.put(StdConstants.RCC_EXT_NS + "nonTangentialProperPart", Operation.RCC8.NTPP);
		ops.put(StdConstants.RCC_EXT_NS + "invNonTangentialProperPart", Operation.RCC8.NTPPI);

		// RCC Extentions
		ops.put(StdConstants.RCC_EXT_NS + "connected", Operation.RCC_EXT.CONNECTED);
		ops.put(StdConstants.RCC_EXT_NS + "part", Operation.RCC_EXT.PART);
		ops.put(StdConstants.RCC_EXT_NS + "invPart", Operation.RCC_EXT.INV_PART);
		ops.put(StdConstants.RCC_EXT_NS + "properPart", Operation.RCC_EXT.PROPER_PART);
		ops.put(StdConstants.RCC_EXT_NS + "invProperPart", Operation.RCC_EXT.INV_PROPER_PART);

		// Spatial Relations
		ops.put(StdConstants.SPATIAL_RELATIONS_NS + "contains", Operation.SimpleFeatures.CONTAINS);
		ops.put(StdConstants.SPATIAL_RELATIONS_NS + "crosses", Operation.SimpleFeatures.CROSSES);
		ops.put(StdConstants.SPATIAL_RELATIONS_NS + "disjoint", Operation.SimpleFeatures.DISJOINT);
		ops.put(StdConstants.SPATIAL_RELATIONS_NS + "equals", Operation.SimpleFeatures.EQUALS);
		ops.put(StdConstants.SPATIAL_RELATIONS_NS + "intersects", Operation.SimpleFeatures.INTERSECTS);
		ops.put(StdConstants.SPATIAL_RELATIONS_NS + "overlaps", Operation.SimpleFeatures.OVERLAPS);
		ops.put(StdConstants.SPATIAL_RELATIONS_NS + "touches", Operation.SimpleFeatures.TOUCHES);
		ops.put(StdConstants.SPATIAL_RELATIONS_NS + "within", Operation.SimpleFeatures.WITHIN);

		// OGC Extentions
		ops.put(StdConstants.SPATIAL_RELATIONS_NS + "covers", Operation.Egenhofer.COVERS);
		ops.put(StdConstants.SPATIAL_RELATIONS_NS + "coveredBy", Operation.Egenhofer.COVEREDBY);

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
