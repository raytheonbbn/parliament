package com.bbn.parliament.jena.query.optimize;

import com.bbn.parliament.jena.graph.index.IndexManager;
import com.bbn.parliament.jena.query.index.pfunction.algebra.IndexPropertyFunctionGenerator;
import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.algebra.TransformCopy;
import com.hp.hpl.jena.sparql.algebra.op.OpBGP;
import com.hp.hpl.jena.sparql.algebra.op.OpTriple;
import com.hp.hpl.jena.sparql.pfunction.PropertyFunctionRegistry;
import com.hp.hpl.jena.sparql.util.Context;

public class TransformIndexPropertyFunction extends TransformCopy {

   private Context context;

   public TransformIndexPropertyFunction(Context context) {
      this.context = context;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Op transform(OpTriple opTriple) {
      return transform(opTriple.asBGP());
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Op transform(OpBGP opBGP) {
      // no indexes
      if (IndexManager.getInstance().size() == 0) {
         return opBGP;
      }

      // no property functions registered
      PropertyFunctionRegistry registry = PropertyFunctionRegistry.get(context);
      if (null == registry) {
         return opBGP;
      }

      return IndexPropertyFunctionGenerator.buildIndexPropertyFunctions(opBGP, context);
   }
}
