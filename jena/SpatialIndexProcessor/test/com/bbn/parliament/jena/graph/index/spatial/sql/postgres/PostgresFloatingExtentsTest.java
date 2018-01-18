package com.bbn.parliament.jena.graph.index.spatial.sql.postgres;

import java.util.Properties;

import org.junit.Ignore;

import com.bbn.parliament.jena.graph.index.spatial.AbstractFloatingExtentsTest;

@Ignore
public class PostgresFloatingExtentsTest extends AbstractFloatingExtentsTest {

   @Override
   protected Properties getProperties() {
      return PostgresPropertyFactory.create();
   }

}
