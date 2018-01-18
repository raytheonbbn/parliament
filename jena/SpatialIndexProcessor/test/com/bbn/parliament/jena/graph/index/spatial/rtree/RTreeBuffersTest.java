/**
 *
 */
package com.bbn.parliament.jena.graph.index.spatial.rtree;

import java.util.Properties;

import com.bbn.parliament.jena.graph.index.spatial.AbstractBuffersTest;

/**
 * @author rbattle
 *
 */
public class RTreeBuffersTest extends AbstractBuffersTest {

   /**
    * {@inheritDoc}
    */
   @Override
   protected Properties getProperties() {
      return RTreePropertyFactory.create();
   }

}
