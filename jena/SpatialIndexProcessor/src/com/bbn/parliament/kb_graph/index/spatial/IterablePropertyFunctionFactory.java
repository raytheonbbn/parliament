package com.bbn.parliament.kb_graph.index.spatial;

import org.apache.jena.sparql.pfunction.PropertyFunctionFactory;

public interface IterablePropertyFunctionFactory extends PropertyFunctionFactory, Iterable<String> {
}
