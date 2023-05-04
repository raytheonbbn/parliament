package com.bbn.parliament.jena.graph.index.spatial.sql.postgres;

import org.apache.jena.riot.system.PrefixMap;
import org.locationtech.jts.geom.Geometry;

import com.bbn.parliament.jena.graph.index.spatial.GeometryRecordFactory;
import com.bbn.parliament.jena.graph.index.spatial.IterableFunctionFactory;
import com.bbn.parliament.jena.graph.index.spatial.IterablePropertyFunctionFactory;
import com.bbn.parliament.jena.graph.index.spatial.OperationFactory;
import com.bbn.parliament.jena.graph.index.spatial.Profile;
import com.bbn.parliament.kb_graph.query.index.operand.OperandFactory;

public class PostgreSQLProfileWrapper implements Profile {
	private Profile base;

	public PostgreSQLProfileWrapper(Profile profile) {
		this.base = profile;
	}

	@Override
	public GeometryRecordFactory getRecordFactory() {
		return base.getRecordFactory();
	}

	@Override
	public IterableFunctionFactory getFunctionFactory() {
		return base.getFunctionFactory();
	}

	@Override
	public PrefixMap getPrefixes() {
		return base.getPrefixes();
	}

	@Override
	public OperationFactory getOperationFactory() {
		return base.getOperationFactory();
	}

	@Override
	public IterablePropertyFunctionFactory getPropertyFunctionFactory() {
		return new PostgreSQLPropertyFunctionFactory(base.getOperationFactory(), base.getOperandFactoryClass());
	}

	@Override
	public Class<? extends OperandFactory<Geometry>> getOperandFactoryClass() {
		return base.getOperandFactoryClass();
	}
}
