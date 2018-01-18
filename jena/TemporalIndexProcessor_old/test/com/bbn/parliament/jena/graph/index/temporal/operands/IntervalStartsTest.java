// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
package com.bbn.parliament.jena.graph.index.temporal.operands;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import com.bbn.parliament.jena.graph.index.Record;
import com.bbn.parliament.jena.graph.index.temporal.Operand;
import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalExtent;
import com.bbn.parliament.jena.graph.index.temporal.index.TestIndexFactory;

public class IntervalStartsTest extends BaseOperandTestClass {

	/**{@inheritDoc} */
	@Override
	public Operand getOperand() {
		return Operand.INTERVAL_STARTS;
	}

	public void testTestExtents() {
		assertFalse("These intervals match 'overlappedBy'.",
			getOperator().testExtents(TestIndexFactory.FOUR,
				TestIndexFactory.THREE));
		assertFalse(
			"These are equal.",
			getOperator().testExtents(TestIndexFactory.ONE, TestIndexFactory.SIX));
		assertFalse("These overlap.", getOperator().testExtents(
			TestIndexFactory.THREE, TestIndexFactory.FOUR));
		assertFalse("These should only match 'startedBy'.", getOperator().testExtents(
			TestIndexFactory.FOUR, TestIndexFactory.TWO));
		assertTrue(getOperator().testExtents(TestIndexFactory.TWO,
			TestIndexFactory.FOUR));
	}

	/** Calls '?x starts FOUR' which should result in TWO. */
	public void testBindFirstVar() {
		Set<String> answerKey = new TreeSet<>();
		answerKey.add("two");
		Iterator<Record<TemporalExtent>> it = getOperator().bindFirstVar(
			TestIndexFactory.FOUR);
		compareExtentIteratorToExpected(it, answerKey);
	}

	/** Calls 'TWO starts ?x' which should result in FOUR. */
	public void testBindSecondVar() {
		Set<String> answerKey = new TreeSet<>();
		answerKey.add("four");
		Iterator<Record<TemporalExtent>> it = getOperator().bindSecondVar(
			TestIndexFactory.TWO);
		compareExtentIteratorToExpected(it, answerKey);
	}
}
