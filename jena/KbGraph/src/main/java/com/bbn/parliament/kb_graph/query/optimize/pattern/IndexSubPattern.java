package com.bbn.parliament.kb_graph.query.optimize.pattern;

import java.util.Set;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.engine.ExecutionContext;
import org.apache.jena.sparql.engine.QueryIterator;

public abstract class IndexSubPattern extends BasicPattern {
	public abstract QueryIterator evaluate(QueryIterator input, ExecutionContext context);
	public abstract Set<Node> getVariables();
}
