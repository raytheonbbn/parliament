/**
 *
 */
package com.bbn.parliament.jena.graph.index.spatial.sql.postgres;

import java.util.Properties;

import org.junit.Ignore;

import com.bbn.parliament.jena.graph.index.spatial.AbstractQueryTest;

/**
 * @author rbattle
 *
 */
@Ignore
public class PostgresQueryTest extends AbstractQueryTest {

   /**
    * {@inheritDoc}
    */
   @Override
   protected Properties getProperties() {
      return PostgresPropertyFactory.create();
   }

}
