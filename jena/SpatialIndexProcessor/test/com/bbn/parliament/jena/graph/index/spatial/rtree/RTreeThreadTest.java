/**
 *
 */
package com.bbn.parliament.jena.graph.index.spatial.rtree;

import java.util.Properties;

import com.bbn.parliament.jena.graph.index.spatial.AbstractThreadTest;

/**
 * @author rbattle
 *
 */
public class RTreeThreadTest extends AbstractThreadTest {

   /**
    * {@inheritDoc}
    */
   @Override
   protected Properties getProperties() {
      return RTreePropertyFactory.create();
   }

}
