// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
package com.bbn.parliament.jena.graph.index.temporal.operands;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import com.bbn.parliament.jena.graph.index.temporal.Operand;
import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalExtent;
import com.bbn.parliament.jena.graph.index.temporal.index.TestIndexFactory;
import com.bbn.parliament.kb_graph.index.Record;

public class IntervalOverlapsTest extends BaseOperandTestClass {

	/** {@inheritDoc} */
	@Override
	public Operand getOperand() {
		return Operand.INTERVAL_OVERLAPS;
	}

	public void testTestExtents() {
		assertFalse("These intervals match 'overlappedBy'.",
			getOperator().testExtents(TestIndexFactory.FOUR, TestIndexFactory.THREE));
		assertFalse("One interval starts the other, so 'overlaps' does not hold.",
			getOperator().testExtents(TestIndexFactory.TWO, TestIndexFactory.FOUR));
		assertFalse("These intervals are equal.", getOperator().testExtents(
			TestIndexFactory.ONE, TestIndexFactory.SIX));
		assertFalse("One interval contains the other.", getOperator().testExtents(
			TestIndexFactory.FOUR, TestIndexFactory.FIVE));
		assertFalse("These intervals share a boundary.",
			getOperator().testExtents(TestIndexFactory.ONE, TestIndexFactory.THREE));
		assertTrue(getOperator().testExtents(TestIndexFactory.THREE, TestIndexFactory.FOUR));
	}

	/** Calls '?x overlaps FOUR' which should result in THREE. */
	public void testBindFirstVar() {
		Set<String> answerKey = new TreeSet<>();
		answerKey.add("three");
		Iterator<Record<TemporalExtent>> it = getOperator().bindFirstVar(
			TestIndexFactory.FOUR);
		compareExtentIteratorToExpected(it, answerKey);
	}

	/** Calls 'THREE overlaps ?x' which should result in FOUR. */
	public void testBindSecondVar() {
		Set<String> answerKey = new TreeSet<>();
		answerKey.add("four");
		Iterator<Record<TemporalExtent>> it = getOperator().bindSecondVar(
			TestIndexFactory.THREE);
		compareExtentIteratorToExpected(it, answerKey);
	}
}
