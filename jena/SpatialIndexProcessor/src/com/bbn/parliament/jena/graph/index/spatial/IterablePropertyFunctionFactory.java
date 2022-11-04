package com.bbn.parliament.jena.graph.index.spatial;

import org.apache.jena.sparql.pfunction.PropertyFunctionFactory;

public interface IterablePropertyFunctionFactory extends PropertyFunctionFactory, Iterable<String> {
}
