package com.bbn.parliament.jena.graph.index.spatial;

import java.util.Properties;
import com.bbn.parliament.jena.graph.index.spatial.geosparql.GeoSPARQLProfile;
import com.bbn.parliament.jena.graph.index.spatial.standard.StandardProfile;
import com.hp.hpl.jena.graph.Graph;

public class ProfileFactory {
	public static Profile createProfile(Properties props, Graph graph) {
		boolean geoSPARQL = Boolean.parseBoolean(props.getProperty(Constants.GEOSPARQL_ENABLED, Boolean.FALSE.toString()));
		Profile p = (geoSPARQL) ? new GeoSPARQLProfile(props) : new StandardProfile(props, graph);

		return p;
	}
}
