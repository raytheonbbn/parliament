// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.graph.index.temporal.operands;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bbn.parliament.jena.graph.index.Record;
import com.bbn.parliament.jena.graph.index.temporal.Operand;
import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalExtent;
import com.bbn.parliament.jena.graph.index.temporal.query.TestIndexFactory;

public class IntervalDuringTest extends BaseOperandTestClass {
	@Override
	@BeforeEach
	public void beforeEach() {
		super.beforeEach();
	}

	@Override
	@AfterEach
	public void afterEach() {
		super.afterEach();
	}

	/** {@inheritDoc} */
	@Override
	public Operand getOperand() {
		return Operand.INTERVAL_DURING;
	}

	@Test
	public void testTestExtents() {
		assertFalse(getOperator().testExtents(TestIndexFactory.THREE, TestIndexFactory.FOUR),
			"These intervals overlap but neither contains the other.");
		assertFalse(getOperator().testExtents(TestIndexFactory.TWO, TestIndexFactory.FOUR),
			"One interval starts the other, so 'contains' does not hold.");
		assertFalse(getOperator().testExtents(TestIndexFactory.TWO, TestIndexFactory.THREE),
			"One interval starts the other, so 'contains' does not hold.");
		assertFalse(getOperator().testExtents(TestIndexFactory.ONE, TestIndexFactory.SIX),
			"These intervals are equal.");
		assertFalse(getOperator().testExtents(TestIndexFactory.FOUR, TestIndexFactory.FIVE),
			"This is a 'contains' relationship rather than 'during'.");
		assertTrue(getOperator().testExtents(TestIndexFactory.FIVE, TestIndexFactory.FOUR));
		assertFalse(pf.testExtents(TestIndexFactory.JAN05, TestIndexFactory.FOUR),
			"This test involves incompatible datatypes and therefore must be false.");
	}

	/** Calls '?x during FOUR' which should result in FIVE. */
	@Test
	public void testBindFirstVar() {
		Set<String> answerKey = new TreeSet<>();
		answerKey.add("five");
		Iterator<Record<TemporalExtent>> it = getOperator().bindFirstVar(TestIndexFactory.FOUR);
		compareExtentIteratorToExpected(it, answerKey);
	}

	/** Calls 'FIVE during ?x' which should result in FOUR. */
	@Test
	public void testBindSecondVar() {
		Set<String> answerKey = new TreeSet<>();
		answerKey.add("four");
		Iterator<Record<TemporalExtent>> it = getOperator().bindSecondVar(TestIndexFactory.FIVE);
		compareExtentIteratorToExpected(it, answerKey);
	}
}
