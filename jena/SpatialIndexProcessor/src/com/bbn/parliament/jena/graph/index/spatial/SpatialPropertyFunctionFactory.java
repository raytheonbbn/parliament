package com.bbn.parliament.jena.graph.index.spatial;

import java.util.Arrays;
import java.util.Iterator;

import org.locationtech.jts.geom.Geometry;

import com.bbn.parliament.jena.query.index.operand.OperandFactory;
import com.bbn.parliament.jena.query.index.pfunction.IndexPropertyFunction;
import com.bbn.parliament.jena.query.index.pfunction.IndexPropertyFunctionFactory;

/** @author rbattle */
public class SpatialPropertyFunctionFactory implements IndexPropertyFunctionFactory<Geometry>, IterablePropertyFunctionFactory {
	protected Class<? extends OperandFactory<Geometry>> operandFactoryClass;
	private OperationFactory operationFactory;

	public SpatialPropertyFunctionFactory(OperationFactory operationFactory,
		Class<? extends OperandFactory<Geometry>> operandFactoryClass) {
		this.operationFactory = operationFactory;
		this.operandFactoryClass = operandFactoryClass;
	}

	/** {@inheritDoc} */
	@Override
	public Iterator<String> iterator() {
		return Arrays.asList(operationFactory.getURIs()).iterator();
	}

	/** {@inheritDoc} */
	@Override
	public final IndexPropertyFunction<Geometry> create(String uri) {
		Operation operation = operationFactory.createOperation(uri);
		return create(uri, operation);
	}

	protected IndexPropertyFunction<Geometry> create(String uri, Operation operation) {
		return new SpatialPropertyFunction(uri, operation, operandFactoryClass);
	}
}
