package com.bbn.parliament.jena.graph.index.spatial.sql.postgres;

import java.util.Properties;

import com.bbn.parliament.jena.graph.index.spatial.AbstractSpatialTest;

public abstract class AbstractPostgresTest extends AbstractSpatialTest {

   @Override
   protected Properties getProperties() {
      return PostgresPropertyFactory.create();
   }
}
