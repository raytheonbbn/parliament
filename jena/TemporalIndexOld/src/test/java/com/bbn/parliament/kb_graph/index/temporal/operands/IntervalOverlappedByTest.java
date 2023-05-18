// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
package com.bbn.parliament.kb_graph.index.temporal.operands;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import com.bbn.parliament.kb_graph.index.Record;
import com.bbn.parliament.kb_graph.index.temporal.Operand;
import com.bbn.parliament.kb_graph.index.temporal.extent.TemporalExtent;
import com.bbn.parliament.kb_graph.index.temporal.index.TestIndexFactory;

public class IntervalOverlappedByTest extends BaseOperandTestClass {

	/** {@inheritDoc} */
	@Override
	public Operand getOperand() {
		return Operand.INTERVAL_OVERLAPPED_BY;
	}

	public void testTestExtents() {
		assertFalse("These intervals overlap.",
			getOperator().testExtents(TestIndexFactory.THREE, TestIndexFactory.FOUR));
		assertFalse("One interval starts the other, so 'overlappedBy' does not hold.",
			getOperator().testExtents(TestIndexFactory.FOUR, TestIndexFactory.TWO));
		assertFalse("These intervals are equal.", getOperator().testExtents(
			TestIndexFactory.ONE, TestIndexFactory.SIX));
		assertFalse("One interval contains the other.", getOperator().testExtents(
			TestIndexFactory.FIVE, TestIndexFactory.FOUR));
		assertFalse("These intervals share a boundary.",
			getOperator().testExtents(TestIndexFactory.THREE, TestIndexFactory.ONE));
		assertTrue(getOperator().testExtents(TestIndexFactory.FOUR,
			TestIndexFactory.THREE));
	}

	/** Calls '?x overlappedBy THREE' which should result in FOUR. */
	public void testBindFirstVar() {
		Set<String> answerKey = new TreeSet<>();
		answerKey.add("four");
		Iterator<Record<TemporalExtent>> it = getOperator().bindFirstVar(
			TestIndexFactory.THREE);
		compareExtentIteratorToExpected(it, answerKey);
	}

	/** Calls 'FOUR overlappedBy ?x' which should result in THREE. */
	public void testBindSecondVar() {
		Set<String> answerKey = new TreeSet<>();
		answerKey.add("three");
		Iterator<Record<TemporalExtent>> it = getOperator().bindSecondVar(
			TestIndexFactory.FOUR);
		compareExtentIteratorToExpected(it, answerKey);
	}
}
