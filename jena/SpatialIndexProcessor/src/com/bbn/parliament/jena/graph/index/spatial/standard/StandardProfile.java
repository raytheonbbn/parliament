package com.bbn.parliament.jena.graph.index.spatial.standard;

import java.util.Properties;

import org.locationtech.jts.geom.Geometry;
import org.openjena.riot.system.PrefixMap;

import com.bbn.parliament.jena.graph.index.spatial.GeometryRecordFactory;
import com.bbn.parliament.jena.graph.index.spatial.IterableFunctionFactory;
import com.bbn.parliament.jena.graph.index.spatial.IterablePropertyFunctionFactory;
import com.bbn.parliament.jena.graph.index.spatial.OperationFactory;
import com.bbn.parliament.jena.graph.index.spatial.Profile;
import com.bbn.parliament.jena.graph.index.spatial.SpatialPropertyFunctionFactory;
import com.bbn.parliament.jena.query.index.operand.OperandFactory;
import com.hp.hpl.jena.graph.Graph;

public class StandardProfile implements Profile {
	private GeometryRecordFactory recordFactory;
	private OperationFactory operationFactory;
	private SpatialPropertyFunctionFactory pfuncFactory;

	public StandardProfile(Properties props, Graph graph) {
		this.recordFactory = new StandardRecordFactory(graph);
		this.operationFactory = new StandardOperationFactory();
		this.pfuncFactory = new SpatialPropertyFunctionFactory(operationFactory, SpatialOperandFactory.class);
	}

	/** {@inheritDoc} */
	@Override
	public GeometryRecordFactory getRecordFactory() {
		return recordFactory;
	}

	/** {@inheritDoc} */
	@Override
	public IterableFunctionFactory getFunctionFactory() {
		return null;
	}

	/** {@inheritDoc} */
	@Override
	public PrefixMap getPrefixes() {
		PrefixMap prefixes = new PrefixMap();

		prefixes.add("gml", StdConstants.GML_NS);
		prefixes.add("gmlh", StdConstants.GML_NS_H);
		prefixes.add("buffer", StdConstants.BUFFER_NS);
		prefixes.add("spatial", StdConstants.SPATIAL_RELATIONS_NS);
		prefixes.add("rcc", StdConstants.RCC_EXT_NS);
		prefixes.add("ogc", StdConstants.OGC_EXT_NS);
		prefixes.add("georss", StdConstants.GEORSS_NS);

		return prefixes;
	}

	/** {@inheritDoc} */
	@Override
	public OperationFactory getOperationFactory() {
		return operationFactory;
	}

	/** {@inheritDoc} */
	@Override
	public IterablePropertyFunctionFactory getPropertyFunctionFactory() {
		return pfuncFactory;
	}

	/** {@inheritDoc} */
	@Override
	public Class<? extends OperandFactory<Geometry>> getOperandFactoryClass() {
		return SpatialOperandFactory.class;
	}
}
