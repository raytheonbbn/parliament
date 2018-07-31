package com.bbn.parliament.jena.graph.index.spatial.sql.postgres;

import java.util.Properties;

import com.bbn.parliament.jena.graph.index.spatial.SpatialTestDataset;
import com.bbn.parliament.jena.graph.index.spatial.Constants;

public class PostgresPropertyFactory {
	public static Properties create() {
		Properties properties = new Properties();
		properties.put(Constants.GEOMETRY_INDEX_TYPE, Constants.GEOMETRY_INDEX_POSTGRESQL);
		properties.put(Constants.USERNAME, SpatialTestDataset.USERNAME);
		properties.put(Constants.PASSWORD, SpatialTestDataset.PASSWORD);
		properties.put(Constants.JDBC_URL, SpatialTestDataset.JDBC_URL);
		return properties;
	}
}
