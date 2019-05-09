package com.bbn.parliament.jena.query.index.operand;

import com.bbn.parliament.jena.graph.index.QueryableIndex;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.sparql.core.BasicPattern;
import com.hp.hpl.jena.sparql.engine.binding.Binding;

public interface OperandFactory<T> {
	public void setIndex(QueryableIndex<T> index);
	public Operand<T> createOperand(Node rootNode, BasicPattern pattern, Binding binding);
	public Operand<T> createOperand(Node rootNode, Binding binding);
}
