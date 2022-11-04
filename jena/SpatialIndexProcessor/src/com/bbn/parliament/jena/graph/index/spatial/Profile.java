package com.bbn.parliament.jena.graph.index.spatial;

import org.apache.jena.riot.system.PrefixMap;
import org.locationtech.jts.geom.Geometry;

import com.bbn.parliament.jena.query.index.operand.OperandFactory;

public interface Profile {
	public GeometryRecordFactory getRecordFactory();
	public IterableFunctionFactory getFunctionFactory();
	public PrefixMap getPrefixes();
	public OperationFactory getOperationFactory();
	public IterablePropertyFunctionFactory getPropertyFunctionFactory();
	public Class<? extends OperandFactory<Geometry>> getOperandFactoryClass();
	//public Properties getProperties();
	//public String getIndexDir();
	//public Node getGraphName();
}
