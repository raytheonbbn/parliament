package com.bbn.parliament.jena.graph.index.spatial;

public class Constants {
	public static final int QUERY_CACHE_SIZE = 100;

	public static final String INDEX_PREFIX = "spatial";

	// configuration properties
	public static final String USERNAME = "username";
	public static final String PASSWORD = "password";
	public static final String JDBC_URL = "jdbcUrl";

	public static final String INDEX_DIRECTORY = "indexDirectory";
	public static final String GEOSPARQL_ENABLED = "geoSPARQL";
	public static final String GEOMETRY_INDEX_TYPE = "indexType";
	public static final String GEOMETRY_INDEX_JTS = "JTS";
	public static final String GEOMETRY_INDEX_POSTGRESQL = "PostgreSQL";
	public static final String GEOMETRY_INDEX_RTREE = "RTree";

	/** Spatial Reference ID for WGS84 */
	public static final int WGS84_SRID = 4326;
	public static final int DEFAULT_SRID = 0;
	public static final String DEFAULT_CRS = "CRS:84";

	/** Internal coordinate reference system code. This CRS represents all geometries. */
	//public static final String INTERNAL_CRS = "EPSG:4326";
	public static final String INTERNAL_CRS = "CRS:84";

	public static final String SPATIAL_FUNCTION_NS = "http://parliament.semwebcentral.org/spatial/pfunction#";
}
