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

public class IntervalEqualsTest extends BaseOperandTestClass {
	/** {@inheritDoc} */
	@Override
	public Operand getOperand() {
		return Operand.INTERVAL_EQUALS;
	}

	public void testTestExtents() {
		assertFalse("These intervals overlap but are not equal.",
			getOperator().testExtents(TestIndexFactory.THREE, TestIndexFactory.FOUR));
		assertFalse("One interval starts the other but are not equal.",
			getOperator().testExtents(TestIndexFactory.TWO, TestIndexFactory.FOUR));
		assertFalse("This is a 'during' relationship rather than 'equals'.",
			getOperator().testExtents(TestIndexFactory.FIVE, TestIndexFactory.FOUR));
		assertTrue("These intervals are equal.",
			getOperator().testExtents(TestIndexFactory.ONE, TestIndexFactory.SIX));
		assertTrue("These are the same two intervals.",
			getOperator().testExtents(TestIndexFactory.ONE, TestIndexFactory.ONE));
	}

	/** Calls '?x equals SIX' which should result in ONE and SIX. */
	public void testBindFirstVar() {
		Set<String> answerKey = new TreeSet<>();
		answerKey.add("one");
		answerKey.add("six");
		Iterator<Record<TemporalExtent>> it = getOperator().bindFirstVar(TestIndexFactory.SIX);
		compareExtentIteratorToExpected(it, answerKey);
	}

	/** Calls 'ONE equals ?x' which should result in ONE and SIX. */
	public void testBindSecondVar() {
		Set<String> answerKey = new TreeSet<>();
		answerKey.add("six");
		answerKey.add("one");
		Iterator<Record<TemporalExtent>> it = getOperator().bindSecondVar(TestIndexFactory.ONE);
		compareExtentIteratorToExpected(it, answerKey);
	}
}
