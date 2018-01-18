package com.bbn.parliament.jena.joseki.bridge.tracker;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.pfunction.PropFuncArg;
import com.hp.hpl.jena.sparql.pfunction.PropertyFunctionBase;
import com.hp.hpl.jena.sparql.util.IterLib;

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
