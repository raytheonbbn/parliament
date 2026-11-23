package com.bbn.parliament.kb_graph.index.spatial.sql.postgres;

import java.util.Properties;

import com.bbn.parliament.kb_graph.index.spatial.Constants;
import com.bbn.parliament.kb_graph.index.spatial.SpatialTestDataset;

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
