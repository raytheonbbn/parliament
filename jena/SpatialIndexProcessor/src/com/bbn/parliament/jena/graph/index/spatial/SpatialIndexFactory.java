package com.bbn.parliament.jena.graph.index.spatial;

import java.util.Properties;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.locationtech.jts.geom.Geometry;

import com.bbn.parliament.jena.graph.index.spatial.jts.JTSIndex;
import com.bbn.parliament.jena.graph.index.spatial.rtree.RTreeIndex;
import com.bbn.parliament.jena.graph.index.spatial.sql.postgres.PostgresIndex;
import com.bbn.parliament.kb_graph.KbGraph;
import com.bbn.parliament.kb_graph.index.IndexFactory;
import com.bbn.parliament.kb_graph.util.FileUtil;

public class SpatialIndexFactory extends IndexFactory<SpatialIndex, Geometry> {
	private static final String LABEL = "Spatial Index";

	private static String cleanGraphName(Node graphName) {
		String uri = null;
		if (null == graphName) {
			uri = "default";
		} else {
			uri = graphName.getURI();
		}
		String s = "s" + uri.hashCode();
		return s;
	}

	private static String cleanDirName(String directory) {
		String clean = FileUtil.encodeStringForFilename(directory);
		StringBuilder b = new StringBuilder();

		boolean first = true;
		for (int i = 0; i < clean.length(); i++) {
			char c = clean.charAt(i);
			if (first && (!((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')))) {
				continue;
			} else if (first) {
				first = false;
				b.append(c);
				continue;
			}
			if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
				|| (c >= '0' && c <= '9') || (c == '_')) {
				b.append(c);
			} else {
				b.append('_');
			}
		}
		return b.toString();
	}

	private Properties config;

	public SpatialIndexFactory() {
		super(LABEL);
		config = new Properties();
		config.put(Constants.GEOMETRY_INDEX_TYPE, Constants.GEOMETRY_INDEX_RTREE);
	}

	/** {@inheritDoc} */
	@Override
	public SpatialIndex createIndex(Graph graph, Node graphName, String indexDir) {
		Properties props = new Properties();
		props.putAll(config);

		Profile profile = ProfileFactory.createProfile(props, graph);

		String indexType = props.getProperty(Constants.GEOMETRY_INDEX_TYPE);
		//boolean geoSPARQL = Boolean.parseBoolean(props.getProperty(Constants.GEOSPARQL_ENABLED, Boolean.FALSE.toString()));
		SpatialIndex index = null;

		if (Constants.GEOMETRY_INDEX_POSTGRESQL.equals(indexType)) {
			String id = null;
			if (graph instanceof KbGraph kbg) {
				String dir = kbg.getRelativeDirectory();
				id = cleanDirName((null == dir || dir.isEmpty())
					? "s_default"
					: "s" + dir);
			} else {
				id = cleanGraphName(graphName);
			}
			index = new PostgresIndex(profile, props, id, indexDir);
		} else if (Constants.GEOMETRY_INDEX_JTS.equals(indexType)) {
			index = new JTSIndex(profile, props, indexDir);
		} else {
			index = new RTreeIndex(profile, props, indexDir);
		}

		return index;
	}

	/** {@inheritDoc} */
	@Override
	public void configure(Properties configuration) {
		config.putAll(configuration);
	}

	/** {@inheritDoc} */
	@Override
	public String getLabel() {
		return LABEL;
	}
}
