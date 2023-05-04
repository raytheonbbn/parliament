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

public class IntervalBeforeTest extends BaseOperandTestClass {

	/** {@inheritDoc} */
	@Override
	public Operand getOperand() {
		return Operand.INTERVAL_BEFORE;
	}

	public void testTestExtents() {
		assertFalse("This case should fail since it comes 'after' the other interval.",
			getOperator().testExtents(TestIndexFactory.TWO, TestIndexFactory.ONE));
		assertFalse("These intervals share a common boundary therefore the 'before' relationship does not apply.",
			getOperator().testExtents(TestIndexFactory.ONE, TestIndexFactory.THREE));
		assertFalse("This test involves two overlapping intervals and therefore 'before' does not hold.",
			getOperator().testExtents(TestIndexFactory.THREE, TestIndexFactory.FOUR));
		assertTrue(getOperator().testExtents(TestIndexFactory.ONE, TestIndexFactory.TWO));
	}

	/** Calls '?x before FIVE' which should result in ONE and SIX */
	public void testBindFirstVar() {
		Set<String> answerKey = new TreeSet<>();
		answerKey.add("one");
		answerKey.add("six");
		Iterator<Record<TemporalExtent>> it = getOperator().bindFirstVar(TestIndexFactory.FIVE);
		compareExtentIteratorToExpected(it, answerKey);
	}

	/** Calls 'ONE before ?x' which should result in TWO, FOUR, and FIVE */
	public void testBindSecondVar() {
		Set<String> answerKey = new TreeSet<>();
		answerKey.add("two");
		answerKey.add("four");
		answerKey.add("five");
		Iterator<Record<TemporalExtent>> it = getOperator().bindSecondVar(TestIndexFactory.ONE);
		compareExtentIteratorToExpected(it, answerKey);
	}
}
