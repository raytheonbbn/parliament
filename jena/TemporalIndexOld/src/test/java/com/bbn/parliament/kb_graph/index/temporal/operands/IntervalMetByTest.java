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

public class IntervalMetByTest extends BaseOperandTestClass {

	/** {@inheritDoc} */
	@Override
	public Operand getOperand() {
		return Operand.INTERVAL_MET_BY;
	}

	public void testTestExtents() {
		assertFalse("These intervals overlap.",
			getOperator().testExtents(TestIndexFactory.THREE, TestIndexFactory.FOUR));
		assertFalse("One interval starts the other, so 'metBy' does not hold.",
			getOperator().testExtents(TestIndexFactory.FOUR, TestIndexFactory.TWO));
		assertFalse("These intervals are equal.", getOperator().testExtents(
			TestIndexFactory.ONE, TestIndexFactory.SIX));
		assertFalse("These intervals do not share a boundary.",
			getOperator().testExtents(TestIndexFactory.ONE, TestIndexFactory.TWO));
		assertTrue(getOperator().testExtents(TestIndexFactory.FIVE,
			TestIndexFactory.THREE));
	}

	/** Calls '?x metBy THREE' which should result in FIVE. */
	public void testBindFirstVar() {
		Set<String> answerKey = new TreeSet<>();
		answerKey.add("five");
		Iterator<Record<TemporalExtent>> it = getOperator().bindFirstVar(
			TestIndexFactory.THREE);
		compareExtentIteratorToExpected(it, answerKey);
	}

	/** Calls 'THREE metBy ?x' which should result in ONE and SIX. */
	public void testBindSecondVar() {
		Set<String> answerKey = new TreeSet<>();
		answerKey.add("one");
		answerKey.add("six");
		Iterator<Record<TemporalExtent>> it = getOperator().bindSecondVar(
			TestIndexFactory.THREE);
		compareExtentIteratorToExpected(it, answerKey);
	}
}
