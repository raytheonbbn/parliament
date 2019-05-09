package com.bbn.parliament.jena.graph.index.spatial;

import com.hp.hpl.jena.sparql.pfunction.PropertyFunctionFactory;

public interface IterablePropertyFunctionFactory extends PropertyFunctionFactory, Iterable<String> {
}
