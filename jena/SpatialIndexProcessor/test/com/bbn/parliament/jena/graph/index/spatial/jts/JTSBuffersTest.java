/**
 *
 */
package com.bbn.parliament.jena.graph.index.spatial.jts;

import java.util.Properties;

import com.bbn.parliament.jena.graph.index.spatial.AbstractBuffersTest;

/**
 * @author rbattle
 *
 */
public class JTSBuffersTest extends AbstractBuffersTest {

   /**
    * {@inheritDoc}
    */
   @Override
   protected Properties getProperties() {
      return JTSPropertyFactory.create();
   }

}
