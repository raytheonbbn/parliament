package com.bbn.parliament.jena.query.optimize;

import com.hp.hpl.jena.sparql.engine.optimizer.reorder.ReorderTransformation;

public class DefaultTransformationTestCase extends AbstractTransformTestCase {

   @Override
   protected ReorderTransformation setupTransformation() {
      return new DefaultCountTransformation(getGraph());
   }

}