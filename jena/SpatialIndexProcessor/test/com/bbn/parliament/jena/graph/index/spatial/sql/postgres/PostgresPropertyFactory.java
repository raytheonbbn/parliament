package com.bbn.parliament.jena.graph.index.spatial.sql.postgres;

import java.util.Properties;

import com.bbn.parliament.jena.graph.index.spatial.AbstractSpatialTest;
import com.bbn.parliament.jena.graph.index.spatial.Constants;

public class PostgresPropertyFactory {
   public static Properties create() {
      Properties properties = new Properties();
      properties.put(Constants.GEOMETRY_INDEX_TYPE, Constants.GEOMETRY_INDEX_POSTGRESQL);
      properties.put(Constants.USERNAME, AbstractSpatialTest.USERNAME);
      properties.put(Constants.PASSWORD, AbstractSpatialTest.PASSWORD);
      properties.put(Constants.JDBC_URL, AbstractSpatialTest.JDBC_URL);

      return properties;
   }
}
