/**
 *
 */
package com.bbn.parliament.jena.graph.index.spatial.rtree;

import java.util.Properties;

import com.bbn.parliament.jena.graph.index.spatial.AbstractQueryTest;

/**
 * @author rbattle
 *
 */
public class RTreeQueryTest extends AbstractQueryTest {

   /**
    * {@inheritDoc}
    */
   @Override
   protected Properties getProperties() {
      return RTreePropertyFactory.create();
   }

}
