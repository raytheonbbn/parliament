/**
 *
 */
package com.bbn.parliament.jena.graph.index.spatial.jts;

import java.util.Properties;

import com.bbn.parliament.jena.graph.index.spatial.AbstractThreadTest;

/**
 * @author rbattle
 *
 */
public class JTSThreadTest extends AbstractThreadTest {

   /**
    * {@inheritDoc}
    */
   @Override
   protected Properties getProperties() {
      return JTSPropertyFactory.create();
   }

}
