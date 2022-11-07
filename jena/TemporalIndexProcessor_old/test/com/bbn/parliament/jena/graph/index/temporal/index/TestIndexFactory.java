// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.graph.index.temporal.index;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.graph.GraphFactory;

import com.bbn.parliament.jena.graph.index.IndexException;
import com.bbn.parliament.jena.graph.index.IndexManager;
import com.bbn.parliament.jena.graph.index.Record;
import com.bbn.parliament.jena.graph.index.temporal.TemporalIndex;
import com.bbn.parliament.jena.graph.index.temporal.TemporalIndexFactory;
import com.bbn.parliament.jena.graph.index.temporal.TemporalPropertyFunctionFactory;
import com.bbn.parliament.jena.graph.index.temporal.bdb.PersistentPropertyFunctionFactory;
import com.bbn.parliament.jena.graph.index.temporal.bdb.PersistentTemporalIndex;
import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalExtent;
import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalInstant;
import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalInterval;

public class TestIndexFactory {
	private static TemporalPropertyFunctionFactory<PersistentTemporalIndex> factory;

	public static Node Jan01;
	public static Node Jan02;
	public static Node Jan03;
	public static Node Jan04;
	public static Node Jan05;
	public static Node Jan06;
	public static Node Jan07;

	public static TemporalInterval ONE;
	public static TemporalInterval TWO;
	public static TemporalInterval THREE;
	public static TemporalInterval FOUR;
	public static TemporalInterval FIVE;
	public static TemporalInterval SIX;

	/* The default test intervals:
	 * 1     2     3     4     5     6     7     8     9     10
	 * [-One-]     [-Two-]
	 *       [--Three----]
	 *             [----------Four---------]
	 *                   [Five-]
	 * [-Six-]
	 */

	static {
		Jan01 = NodeFactory.createLiteral("2005-01-01T05:00:00", null, XSDDatatype.XSDdateTime);
		Jan02 = NodeFactory.createLiteral("2005-01-02T05:00:00", null, XSDDatatype.XSDdateTime);
		Jan03 = NodeFactory.createLiteral("2005-01-03T05:00:00", null, XSDDatatype.XSDdateTime);
		Jan04 = NodeFactory.createLiteral("2005-01-04T05:00:00", null, XSDDatatype.XSDdateTime);
		Jan05 = NodeFactory.createLiteral("2005-01-05T05:00:00", null, XSDDatatype.XSDdateTime);
		Jan06 = NodeFactory.createLiteral("2005-01-06T05:00:00", null, XSDDatatype.XSDdateTime);
		Jan07 = NodeFactory.createLiteral("2005-01-07T05:00:00", null, XSDDatatype.XSDdateTime);

		ONE = createTestInterval(Jan01, Jan02);
		TWO = createTestInterval(Jan03, Jan04);
		THREE = createTestInterval(Jan02, Jan04);
		FOUR = createTestInterval(Jan03, Jan07);
		FIVE = createTestInterval(Jan04, Jan05);
		SIX = createTestInterval(Jan01, Jan02);
	}

	public static TemporalIndex createPopulatedTestIndex() throws IndexException {
		TemporalIndexFactory tif = new TemporalIndexFactory();
		TemporalIndex index = tif.createIndex(GraphFactory.createPlainGraph(), null);
		index.open();
		IndexManager.getInstance().register(index.getGraph(), null, tif, index);
		addDefaultTestIntervals(index);
		return index;
	}

	private static long getTime(Node n) {
		return ((XSDDateTime)n.getLiteralValue()).asCalendar().getTimeInMillis();
	}

	private static TemporalInterval createTestInterval(Node start, Node end) {
		long s = getTime(start);
		long e = getTime(end);
		return new TemporalInterval(
			new TemporalInstant(s, null, true),
			new TemporalInstant(e, null, false));
	}

	private static TemporalIndex addTestInterval(TemporalIndex index, TemporalInterval ti, String label) throws IndexException {
		Record<TemporalExtent> record = Record.create(NodeFactory.createURI(label), (TemporalExtent)ti);
		index.add(record);
		return index;
	}

	private static void addDefaultTestIntervals(TemporalIndex index) throws IndexException {
		index.clear();
		addTestInterval(index, ONE, "one");
		addTestInterval(index, TWO, "two");
		addTestInterval(index, THREE, "three");
		addTestInterval(index, FOUR, "four");
		addTestInterval(index, FIVE, "five");
		addTestInterval(index, SIX, "six");
	}

	public static TemporalPropertyFunctionFactory<PersistentTemporalIndex> createPropertyFunctionFactory() {
		if (null == factory) {
			factory = new PersistentPropertyFunctionFactory();
		}
		return factory;
	}
}
