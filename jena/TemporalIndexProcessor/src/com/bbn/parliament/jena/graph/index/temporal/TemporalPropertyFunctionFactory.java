package com.bbn.parliament.jena.graph.index.temporal;

import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalExtent;
import com.bbn.parliament.jena.query.index.pfunction.IndexPropertyFunctionFactory;

public abstract class TemporalPropertyFunctionFactory<I extends TemporalIndex> implements IndexPropertyFunctionFactory<TemporalExtent> {

   /**
    * {@inheritDoc}
    */
   @Override
   public abstract TemporalPropertyFunction<I> create(String uri);

   public TemporalPropertyFunction<I> create(Operand op) {
      return create(op.getUri());
   }

}
