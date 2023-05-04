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

public class IntervalContainsTest extends BaseOperandTestClass {
	/** {@inheritDoc} */
	@Override
	public Operand getOperand() {
		return Operand.INTERVAL_CONTAINS;
	}

	public void testTestExtents() {
		assertFalse("These intervals overlap but neither contains the other.",
			getOperator().testExtents(TestIndexFactory.THREE, TestIndexFactory.FOUR));
		assertFalse("One interval starts the other, so 'contains' does not hold.",
			getOperator().testExtents(TestIndexFactory.TWO, TestIndexFactory.FOUR));
		assertFalse("One interval starts the other, so 'contains' does not hold.",
			getOperator().testExtents(TestIndexFactory.TWO, TestIndexFactory.THREE));
		assertFalse("These intervals are equal.",
			getOperator().testExtents(TestIndexFactory.ONE, TestIndexFactory.SIX));
		assertFalse("This is a 'during' relationship rather than 'contains'.",
			getOperator().testExtents(TestIndexFactory.FIVE, TestIndexFactory.FOUR));
		assertTrue(getOperator().testExtents(TestIndexFactory.FOUR, TestIndexFactory.FIVE));
	}

	/** Calls '?x contains FIVE' which should result in FOUR. */
	public void testBindFirstVar() {
		Set<String> answerKey = new TreeSet<>();
		answerKey.add("four");
		Iterator<Record<TemporalExtent>> it = getOperator().bindFirstVar(TestIndexFactory.FIVE);
		compareExtentIteratorToExpected(it, answerKey);
	}

	/** Calls 'FOUR contains ?x' which should result in FIVE. */
	public void testBindSecondVar() {
		Set<String> answerKey = new TreeSet<>();
		answerKey.add("five");
		Iterator<Record<TemporalExtent>> it = getOperator().bindSecondVar(TestIndexFactory.FOUR);
		compareExtentIteratorToExpected(it, answerKey);
	}
}
