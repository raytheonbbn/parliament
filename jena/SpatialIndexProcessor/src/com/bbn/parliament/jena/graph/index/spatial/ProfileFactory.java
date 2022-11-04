package com.bbn.parliament.jena.graph.index.spatial;

import java.util.Properties;

import org.apache.jena.graph.Graph;

import com.bbn.parliament.jena.graph.index.spatial.geosparql.GeoSPARQLProfile;
import com.bbn.parliament.jena.graph.index.spatial.standard.StandardProfile;

public class ProfileFactory {
	public static Profile createProfile(Properties props, Graph graph) {
		boolean geoSPARQL = Boolean.parseBoolean(props.getProperty(
			Constants.GEOSPARQL_ENABLED, Boolean.FALSE.toString()));
		return (geoSPARQL) ? new GeoSPARQLProfile(props) : new StandardProfile(props, graph);
	}
}
