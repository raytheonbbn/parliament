package com.bbn.parliament.kb_graph.index.spatial;

import org.apache.jena.sparql.function.FunctionFactory;

public interface IterableFunctionFactory extends FunctionFactory, Iterable<String> {
}
