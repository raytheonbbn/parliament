package com.bbn.parliament.jena.query.optimize.pattern;

import java.util.Set;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.sparql.core.BasicPattern;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.engine.QueryIterator;

public abstract class IndexSubPattern extends BasicPattern {
	public abstract QueryIterator evaluate(QueryIterator input, ExecutionContext context);
	public abstract Set<Node> getVariables();
}
