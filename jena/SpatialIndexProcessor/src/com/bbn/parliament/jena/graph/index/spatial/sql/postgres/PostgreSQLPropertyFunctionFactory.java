package com.bbn.parliament.jena.graph.index.spatial.sql.postgres;

import org.locationtech.jts.geom.Geometry;

import com.bbn.parliament.jena.graph.index.spatial.Operation;
import com.bbn.parliament.jena.graph.index.spatial.OperationFactory;
import com.bbn.parliament.jena.graph.index.spatial.SpatialPropertyFunctionFactory;
import com.bbn.parliament.kb_graph.query.index.operand.OperandFactory;
import com.bbn.parliament.kb_graph.query.index.pfunction.IndexPropertyFunction;

public class PostgreSQLPropertyFunctionFactory extends SpatialPropertyFunctionFactory {
	public PostgreSQLPropertyFunctionFactory(OperationFactory operationFactory,
		Class<? extends OperandFactory<Geometry>> operandFactoryClass) {
		super(operationFactory, operandFactoryClass);
	}

	@Override
	protected IndexPropertyFunction<Geometry> create(String uri, Operation operation) {
		return new PostgresPropertyFunction(uri, operation, operandFactoryClass);
	}
}
