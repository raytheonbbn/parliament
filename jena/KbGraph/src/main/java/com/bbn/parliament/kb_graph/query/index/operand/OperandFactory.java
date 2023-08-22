package com.bbn.parliament.kb_graph.query.index.operand;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.engine.binding.Binding;

import com.bbn.parliament.kb_graph.index.QueryableIndex;

public interface OperandFactory<T> {
	public void setIndex(QueryableIndex<T> index);
	public Operand<T> createOperand(Node rootNode, BasicPattern pattern, Binding binding);
	public Operand<T> createOperand(Node rootNode, Binding binding);
}