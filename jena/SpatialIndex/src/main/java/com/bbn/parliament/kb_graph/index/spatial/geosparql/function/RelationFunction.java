package com.bbn.parliament.kb_graph.index.spatial.geosparql.function;

import java.util.List;

import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionEnv;
import org.locationtech.jts.geom.Geometry;

import com.bbn.parliament.kb_graph.index.spatial.Operation;
import com.bbn.parliament.kb_graph.index.spatial.geosparql.datatypes.GeoSPARQLLiteral;

public abstract class RelationFunction extends DoubleGeometrySpatialFunction {
	private Operation operation;

	public RelationFunction(Operation operation) {
		this.operation = operation;
	}

	@Override
	protected String[] getRestOfArgumentTypes() {
		return new String[] { };
	}

	@Override
	protected NodeValue exec(Geometry g1, Geometry g2,
		GeoSPARQLLiteral datatype, Binding binding, List<NodeValue> evalArgs,
		String uri, FunctionEnv env) {
		// TODO: Add support for optimizing functions
		boolean valid = operation.relate(g1, g2);
		if (valid) {
			return NodeValue.TRUE;
		}
		return NodeValue.FALSE;
	}

	public static class RCC8_EQ extends RelationFunction {
		public RCC8_EQ() {
			super(Operation.RCC8.EQ);
		}
	}

	public static class RCC8_DC extends RelationFunction {
		public RCC8_DC() {
			super(Operation.RCC8.DC);
		}
	}

	public static class RCC8_EC extends RelationFunction {
		public RCC8_EC() {
			super(Operation.RCC8.EC);
		}
	}

	public static class RCC8_PO extends RelationFunction {
		public RCC8_PO() {
			super(Operation.RCC8.PO);
		}
	}

	public static class RCC8_TPPI extends RelationFunction {
		public RCC8_TPPI() {
			super(Operation.RCC8.TPPI);
		}
	}

	public static class RCC8_TPP extends RelationFunction {
		public RCC8_TPP() {
			super(Operation.RCC8.TPP);
		}
	}

	public static class RCC8_NTTP extends RelationFunction {
		public RCC8_NTTP() {
			super(Operation.RCC8.NTPP);
		}
	}

	public static class RCC8_NTTPI extends RelationFunction {
		public RCC8_NTTPI() {
			super(Operation.RCC8.NTPPI);
		}
	}

	public static class EH_Equals extends RelationFunction {
		public EH_Equals() {
			super(Operation.Egenhofer.EQUALS);
		}
	}

	public static class EH_Disjoint extends RelationFunction {
		public EH_Disjoint() {
			super(Operation.Egenhofer.DISJOINT);
		}
	}

	public static class EH_Meet extends RelationFunction {
		public EH_Meet() {
			super(Operation.Egenhofer.MEET);
		}
	}

	public static class EH_Overlap extends RelationFunction {
		public EH_Overlap() {
			super(Operation.Egenhofer.OVERLAP);
		}
	}

	public static class EH_Covers extends RelationFunction {
		public EH_Covers() {
			super(Operation.Egenhofer.COVERS);
		}
	}

	public static class EH_CoveredBy extends RelationFunction {
		public EH_CoveredBy() {
			super(Operation.Egenhofer.COVEREDBY);
		}
	}

	public static class EH_Inside extends RelationFunction {
		public EH_Inside() {
			super(Operation.Egenhofer.INSIDE);
		}
	}

	public static class EH_Contains extends RelationFunction {
		public EH_Contains() {
			super(Operation.Egenhofer.CONTAINS);
		}
	}

	public static class SF_Equals extends RelationFunction {
		public SF_Equals() {
			super(Operation.SimpleFeatures.EQUALS);
		}
	}

	public static class SF_Disjoint extends RelationFunction {
		public SF_Disjoint() {
			super(Operation.SimpleFeatures.DISJOINT);
		}
	}

	public static class SF_Intersects extends RelationFunction {
		public SF_Intersects() {
			super(Operation.SimpleFeatures.INTERSECTS);
		}
	}

	public static class SF_Touches extends RelationFunction {
		public SF_Touches() {
			super(Operation.SimpleFeatures.TOUCHES);
		}
	}

	public static class SF_Within extends RelationFunction {
		public SF_Within() {
			super(Operation.SimpleFeatures.WITHIN);
		}
	}

	public static class SF_Contains extends RelationFunction {
		public SF_Contains() {
			super(Operation.SimpleFeatures.CONTAINS);
		}
	}

	public static class SF_Overlaps extends RelationFunction {
		public SF_Overlaps() {
			super(Operation.SimpleFeatures.OVERLAPS);
		}
	}

	public static class SF_Crosses extends RelationFunction {
		public SF_Crosses() {
			super(Operation.SimpleFeatures.CROSSES);
		}
	}
}
