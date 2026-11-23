// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
package com.bbn.parliament.kb_graph.index.temporal.query;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.graph.GraphFactory;

import com.bbn.parliament.kb_graph.index.IndexException;
import com.bbn.parliament.kb_graph.index.IndexManager;
import com.bbn.parliament.kb_graph.index.Record;
import com.bbn.parliament.kb_graph.index.temporal.TemporalIndex;
import com.bbn.parliament.kb_graph.index.temporal.TemporalIndexFactory;
import com.bbn.parliament.kb_graph.index.temporal.TemporalPropertyFunctionFactory;
import com.bbn.parliament.kb_graph.index.temporal.bdb.PersistentPropertyFunctionFactory;
import com.bbn.parliament.kb_graph.index.temporal.bdb.PersistentTemporalIndex;
import com.bbn.parliament.kb_graph.index.temporal.extent.TemporalExtent;
import com.bbn.parliament.kb_graph.index.temporal.extent.TemporalInstant;
import com.bbn.parliament.kb_graph.index.temporal.extent.TemporalInterval;

public class TestIndexFactory {
	private static TemporalIndex populatedIndex;
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

	public static TemporalInstant JAN01;
	public static TemporalInstant JAN02;
	public static TemporalInstant JAN03;
	public static TemporalInstant JAN04;
	public static TemporalInstant JAN05;
	public static TemporalInstant JAN06;
	public static TemporalInstant ALSO_JAN06;

	/* The default test intervals:
	 * January
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

		JAN01 = createTestInstant(Jan01);
		JAN02 = createTestInstant(Jan02);
		JAN03 = createTestInstant(Jan03);
		JAN04 = createTestInstant(Jan04);
		JAN05 = createTestInstant(Jan05);
		JAN06 = createTestInstant(Jan06);
		ALSO_JAN06 = createTestInstant(Jan06);


	}

	public static TemporalIndex createPopulatedTestIndex() throws IndexException {
		if(populatedIndex == null) {
			TemporalIndexFactory tif = new TemporalIndexFactory();
			TemporalIndex index = tif.createIndex(GraphFactory.createPlainGraph(), null);
			index.open();
			IndexManager.getInstance().register(index.getGraph(), null, tif, index);
			populatedIndex = addDefaultTestExtents(index);
		}
		if (populatedIndex.isClosed()) {
			populatedIndex.open();
		}

		return populatedIndex;
	}

	private static long getTime(Node n) {
		return ((XSDDateTime)n.getLiteralValue()).asCalendar().getTimeInMillis();
	}

	private static TemporalInterval createTestInterval(Node start, Node end) {
		long s = getTime(start);
		long e = getTime(end);
		return new TemporalInterval(new TemporalInstant(s, null, true),
				new TemporalInstant(e, null, false));
	}

	private static TemporalIndex addTestExtent(TemporalIndex index, TemporalExtent ti, String label) throws IndexException {
		Record<TemporalExtent> record = Record.create(NodeFactory.createURI(label), ti);
		index.add(record);

		return index;
	}

	private static TemporalInstant createTestInstant(Node instant) {
		long t = getTime(instant);
		return new TemporalInstant(t);
	}

	private static TemporalIndex addDefaultTestExtents(TemporalIndex index) throws IndexException {
		index.clear();
		addTestExtent(index, ONE, "one");
		addTestExtent(index, TWO, "two");
		addTestExtent(index, THREE, "three");
		addTestExtent(index, FOUR, "four");
		addTestExtent(index, FIVE, "five");
		addTestExtent(index, SIX, "six");

		addTestExtent(index, JAN01, "January 1");
		addTestExtent(index, JAN02, "January 2");
		addTestExtent(index, JAN03, "January 3");
		addTestExtent(index, JAN04, "January 4");
		addTestExtent(index, JAN05, "January 5");
		addTestExtent(index, JAN06, "January 6");
		addTestExtent(index, ALSO_JAN06, "Also January 6");

		return index;
	}

	public static TemporalPropertyFunctionFactory<PersistentTemporalIndex> createPropertyFunctionFactory() {
		if (null == factory) {
			factory = new PersistentPropertyFunctionFactory();
		}
		return factory;
	}
}
