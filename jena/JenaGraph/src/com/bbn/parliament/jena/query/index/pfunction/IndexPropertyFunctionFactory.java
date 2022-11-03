package com.bbn.parliament.jena.query.index.pfunction;

import org.apache.jena.sparql.pfunction.PropertyFunctionFactory;

/** @author rbattle */
public interface IndexPropertyFunctionFactory<T> extends PropertyFunctionFactory {
	/** Create a property function. */
	@Override
	public IndexPropertyFunction<T> create(String uri);
}
