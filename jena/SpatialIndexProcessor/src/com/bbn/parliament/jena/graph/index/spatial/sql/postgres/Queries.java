// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.graph.index.spatial.sql.postgres;

/** @author Robert Battle */
public class Queries {
	public static final String SELECT_ALL_QUERY = "SELECT " + PostgresIndex.NODE_COLUMN + ", AsBinary(" + PostgresIndex.GEOMETRY_COLUMN + ") AS " + PostgresIndex.GEOMETRY_COLUMN + " FROM %1$s ";
	private static final String PREFIX = SELECT_ALL_QUERY + " WHERE ";

	// operations
	@SuppressWarnings("hiding")
	public static class SimpleFeatures {
		public static final String EQUALS = PREFIX + " ST_Equals(%2$s, %3$s)";
		public static final String DISJOINT = PREFIX + "NOT ST_Intersects(%2$s, %3$s)";
		public static final String INTERSECTS = PREFIX + "ST_Intersects(%2$s, %3$s)";
		public static final String TOUCHES = PREFIX + "ST_Touches(%2$s, %3$s)";
		public static final String WITHIN = PREFIX + "ST_Within(%2$s, %3$s)";
		public static final String CONTAINS = PREFIX + "ST_Contains(%2$s, %3$s)";
		public static final String OVERLAPS = PREFIX + "ST_Overlaps(%2$s, %3$s)";
		public static final String CROSSES = PREFIX + "ST_CrosseS(%2$s, %3$s)";

		public static class Estimate {
			public static final String EQUALS = EST_PREFIX + PostgresIndex.GEOMETRY_COLUMN + " = ST_SetSRID(?::geometry,?)";
		}
	}

	@SuppressWarnings("hiding")
	public static class Egenhofer {
		public static final String EQUALS = SimpleFeatures.EQUALS;
		public static final String DISJOINT = SimpleFeatures.DISJOINT;
		public static final String MEET = SimpleFeatures.TOUCHES;
		public static final String OVERLAP = SimpleFeatures.OVERLAPS;
		public static final String COVERS = PREFIX + "ST_Covers(%2$s, %3$s)";
		public static final String COVEREDBY = PREFIX + "ST_CoveredBy(%2$s, %3$s)";
		public static final String INSIDE = PREFIX + "ST_Relate(%2$s, %3$s, 'TFF*FFT**') && ST_SetSRID(?::box2d,?)";
		public static final String CONTAINS = PREFIX + "ST_Relate(%2$s, %3$s, 'T*TFF*FF*') && ST_SetSRID(?::box2d,?)";
	}

	public static class RCC8 {
		public static final String EQ = SimpleFeatures.EQUALS;
		public static final String DC = SimpleFeatures.DISJOINT;
	}

	public static final String RELATE = PREFIX + "ST_Relate(%2$s, %3$s, %4$s)";
	public static final String RELATE_INTERSECTION = PREFIX + PostgresIndex.GEOMETRY_COLUMN + " && ST_SetSRID(?::box2d, ?) AND ST_Relate(%2$s, %3$s, %4$s)";
	public static final String CONNECTED = PREFIX + "ST_Intersects(%2$s, %3$s)";
	public static final String DISCONNECTED = PREFIX + "NOT ST_Intersects(%2$s, %3$s)";
	public static final String EXTERNALLY_CONNECTED = PREFIX + "ST_Touches(%2$s, %3$s)";
	public static final String IDENTICAL = PREFIX + "ST_Equals(%2$s, %3$s)";
	public static final String PART = PREFIX + PostgresIndex.GEOMETRY_COLUMN + " && ST_SetSRID(?::box2d,?) AND (ST_Overlaps(%2$s, %3$s) OR ProperPart(%2$s, %3$s))";
	public static final String PROPER_PART = PREFIX + "ProperPart(%2$s, %3$s)";
	public static final String TANGENTIAL_PROPER_PART = PREFIX + "TangentialProperPart(%2$s, %3$s)";
	public static final String NON_TANGENTIAL_PROPER_PART = PREFIX + "NonTangentialProperPart(%2$s, %3$s)";
	public static final String PARTIALLY_OVERLAPS = PREFIX + "ST_Overlaps(%2$s, %3$s)";
	public static final String INTERSECTS = PREFIX + "ST_Intersects(%2$s, %3$s)";
	public static final String EQUALS = PREFIX + "ST_Equals(%2$s, %3$s)";
	public static final String TOUCHES = PREFIX + "ST_Touches(%2$s, %3$s)";
	public static final String CROSSES = PREFIX + "ST_Crosses(%2$s, %3$s)";
	public static final String WITHIN = PREFIX + "ST_Within(%2$s, %3$s)";
	public static final String OVERLAPS = PREFIX + "ST_Overlaps(%2$s, %3$s)";
	public static final String COVERS = PREFIX + "ST_Covers(%2$s, %3$s)";
	public static final String COVERED_BY = PREFIX + "ST_CoveredBy(%2$s, %3$s)";

	// estimate selectivity
	public static final String EST_PREFIX = "SELECT COUNT(" + PostgresIndex.GEOMETRY_COLUMN + ") FROM %1$s WHERE ";
	private static final String OVERLAPS_QUERY = EST_PREFIX + PostgresIndex.GEOMETRY_COLUMN + " && ST_SetSRID(?::geometry,?)";
	private static final String CONTAINS_QUERY = EST_PREFIX + PostgresIndex.GEOMETRY_COLUMN + " @ ST_SetSRID(?::geometry,?)";
	private static final String INV_CONTAINS_QUERY = EST_PREFIX + "ST_SetSRID(?::geometry,?) @ " + PostgresIndex.GEOMETRY_COLUMN;
	public static final String PART_EST = OVERLAPS_QUERY;
	public static final String INV_PART_EST = OVERLAPS_QUERY;
	public static final String DISCONNECTED_EST = EST_PREFIX + "NOT (" + PostgresIndex.GEOMETRY_COLUMN + " && ST_SetSRID(?::geometry,?))";
	public static final String CONNECTED_EST = OVERLAPS_QUERY;
	public static final String EXTERNALLY_CONNECTED_EST = OVERLAPS_QUERY;//EST_PREFIX + "ST_Touches(" + PostgresIndex.GEOMETRY_COLUMN + ", ?)";
	public static final String PARTIALLY_OVERLAPS_EST = OVERLAPS_QUERY;
	public static final String PROPER_PART_EST = CONTAINS_QUERY;
	public static final String INV_PROPER_PART_EST = INV_CONTAINS_QUERY;
	public static final String TANGENTIAL_PROPER_PART_EST = CONTAINS_QUERY;
	public static final String INV_TANGENTIAL_PROPER_PART_EST = INV_CONTAINS_QUERY;
	public static final String NON_TANGENTIAL_PROPER_PART_EST = CONTAINS_QUERY;
	public static final String INV_NON_TANGENTIAL_PROPER_PART_EST = INV_CONTAINS_QUERY;
	public static final String IDENTICAL_EST = EST_PREFIX + PostgresIndex.GEOMETRY_COLUMN + " = ST_SetSRID(?::geometry,?)";
	public static final String EQUALS_EST = IDENTICAL_EST;
	public static final String INTERSECTS_EST = OVERLAPS_QUERY;
	public static final String TOUCHES_EST = OVERLAPS_QUERY;
	public static final String CROSSES_EST = OVERLAPS_QUERY;
	public static final String WITHIN_EST = CONTAINS_QUERY;
	public static final String OVERLAPS_EST = OVERLAPS_QUERY;
	public static final String COVERS_EST = INV_CONTAINS_QUERY;
	public static final String COVERED_BY_EST = CONTAINS_QUERY;

	// miscellaneous queries
	public static final String SIZE_QUERY = "SELECT COUNT(" + PostgresIndex.NODE_COLUMN + ") AS nodeCount FROM %1$s";
	public static final String DELETE = "DELETE FROM %1$s WHERE " + PostgresIndex.NODE_COLUMN + " = ?";
	public static final String INSERT = "INSERT INTO %1$s (" + PostgresIndex.NODE_COLUMN + ", " + PostgresIndex.GEOMETRY_COLUMN + ") VALUES (?, ?)";

	// lookup queries
	public static final String INTERSECTS_GEOMETRY_QUERY = SELECT_ALL_QUERY + "WHERE ST_Intersects(ST_Envelope(?), " + PostgresIndex.GEOMETRY_COLUMN + ")";
	public static final String NODE_TO_GEOMETRY_QUERY = SELECT_ALL_QUERY + "WHERE node = ? LIMIT 1";
	public static final String GEOMETRY_TO_NODE_QUERY = SELECT_ALL_QUERY + "WHERE " + PostgresIndex.GEOMETRY_COLUMN + " = ST_SetSRID(?, ?) LIMIT 1";
	public static final String INTERSECTS_DISTANCE_QUERY = SELECT_ALL_QUERY + "WHERE ST_Intersects(" + PostgresIndex.GEOMETRY_COLUMN + ", Transform(ST_Buffer(Transform(ST_SetSRID(?, ?),?), ?), ?))";
}
