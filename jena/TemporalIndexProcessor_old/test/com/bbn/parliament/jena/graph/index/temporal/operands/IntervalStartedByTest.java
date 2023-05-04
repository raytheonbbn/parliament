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

public class IntervalStartedByTest extends BaseOperandTestClass {

	/** {@inheritDoc} */
	@Override
	public Operand getOperand() {
		return Operand.INTERVAL_STARTED_BY;
	}

	public void testTestExtents() {
		assertFalse("These intervals match 'overlappedBy'.",
			getOperator().testExtents(TestIndexFactory.FOUR,
				TestIndexFactory.THREE));
		assertFalse( "These are equal.",
			getOperator().testExtents(TestIndexFactory.ONE, TestIndexFactory.SIX));
		assertFalse("These overlap.", getOperator().testExtents(
			TestIndexFactory.THREE, TestIndexFactory.FOUR));
		assertFalse("These should only match 'starts'.", getOperator().testExtents(
			TestIndexFactory.TWO, TestIndexFactory.FOUR));
		assertTrue(getOperator().testExtents(TestIndexFactory.FOUR,
			TestIndexFactory.TWO));
	}

	/** Calls '?x startedBy TWO' which should result in FOUR. */
	public void testBindFirstVar() {
		Set<String> answerKey = new TreeSet<>();
		answerKey.add("four");
		Iterator<Record<TemporalExtent>> it = getOperator().bindFirstVar(
			TestIndexFactory.TWO);
		compareExtentIteratorToExpected(it, answerKey);
	}

	/** Calls 'FOUR startedBy ?x' which should result in TWO. */
	public void testBindSecondVar() {
		Set<String> answerKey = new TreeSet<>();
		answerKey.add("two");
		Iterator<Record<TemporalExtent>> it = getOperator().bindSecondVar(
			TestIndexFactory.FOUR);
		compareExtentIteratorToExpected(it, answerKey);
	}
}
