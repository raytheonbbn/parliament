/**
 *
 */
package com.bbn.parliament.jena.graph.index.spatial.sql.postgres;

import java.util.Properties;

import org.junit.Ignore;

import com.bbn.parliament.jena.graph.index.spatial.AbstractBuffersTest;

/**
 * @author rbattle
 *
 */
@Ignore
public class PostgresBuffersTest extends AbstractBuffersTest {

   /**
    * {@inheritDoc}
    */
   @Override
   protected Properties getProperties() {
      return PostgresPropertyFactory.create();
   }

}
