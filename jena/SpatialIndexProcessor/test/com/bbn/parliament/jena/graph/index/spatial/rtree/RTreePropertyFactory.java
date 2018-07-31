package com.bbn.parliament.jena.graph.index.spatial.rtree;

import java.util.Properties;

import com.bbn.parliament.jena.graph.index.spatial.Constants;

public class RTreePropertyFactory {
	public static Properties create() {
		Properties properties = new Properties();
		properties.put(Constants.GEOMETRY_INDEX_TYPE, Constants.GEOMETRY_INDEX_RTREE);
		return properties;
	}
}
