package com.bbn.parliament.kb_graph.index.spatial.jts;

import java.util.Properties;

import com.bbn.parliament.kb_graph.index.spatial.Constants;

public class JTSPropertyFactory {
	public static Properties create() {
		Properties properties = new Properties();
		properties.put(Constants.GEOMETRY_INDEX_TYPE, Constants.GEOMETRY_INDEX_JTS);
		return properties;
	}
}
