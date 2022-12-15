package com.bbn.parliament.spring_boot;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.engine.ExecutionContext;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.pfunction.PropFuncArg;
import org.apache.jena.sparql.pfunction.PropertyFunctionBase;
import org.apache.jena.sparql.util.IterLib;

public class Suspend extends PropertyFunctionBase {
	@Override
	public QueryIterator exec(Binding binding, PropFuncArg argSubject,
		Node predicate, PropFuncArg argObject, ExecutionContext execCxt) {
		System.out.println("sleeping");
		try {
			Thread.sleep(100000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("awake");
		return IterLib.noResults(execCxt);
	}
}
