/**
 *
 */
package com.bbn.parliament.jena.graph.index.spatial.jts;

import java.util.Properties;

import com.bbn.parliament.jena.graph.index.spatial.AbstractQueryTest;

/**
 * @author rbattle
 *
 */
public class JTSQueryTest extends AbstractQueryTest {

   /**
    * {@inheritDoc}
    */
   @Override
   protected Properties getProperties() {
      return JTSPropertyFactory.create();
   }

}
