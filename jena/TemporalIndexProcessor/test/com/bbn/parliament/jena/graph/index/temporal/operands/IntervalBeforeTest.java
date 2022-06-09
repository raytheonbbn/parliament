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

public class IntervalBeforeTest extends BaseOperandTestClass {
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
		return Operand.INTERVAL_BEFORE;
	}

	@Test
	public void testTestExtents() {
		assertFalse(getOperator().testExtents(TestIndexFactory.TWO, TestIndexFactory.ONE),
			"This case should fail since it comes 'after' the other interval.");
		assertFalse(getOperator().testExtents(TestIndexFactory.ONE, TestIndexFactory.THREE),
			"These intervals share a common boundary therefore the 'before' relationship does not apply.");
		assertFalse(getOperator().testExtents(TestIndexFactory.THREE, TestIndexFactory.FOUR),
			"This test involves two overlapping intervals and therefore 'before' does not hold.");
		assertTrue(getOperator().testExtents(TestIndexFactory.ONE, TestIndexFactory.TWO));
		assertFalse(pf.testExtents(TestIndexFactory.JAN01, TestIndexFactory.FOUR),
			"This test involves incompatible datatypes and therefore must be false.");
	}

	/** Calls '?x before FIVE' which should result in ONE and SIX */
	@Test
	public void testBindFirstVar() {
		Set<String> answerKey = new TreeSet<>();
		answerKey.add("one");
		answerKey.add("six");
		Iterator<Record<TemporalExtent>> it = getOperator().bindFirstVar(TestIndexFactory.FIVE);
		compareExtentIteratorToExpected(it, answerKey);
	}

	/** Calls 'ONE before ?x' which should result in TWO, FOUR, and FIVE */
	@Test
	public void testBindSecondVar() {
		Set<String> answerKey = new TreeSet<>();
		answerKey.add("two");
		answerKey.add("four");
		answerKey.add("five");
		Iterator<Record<TemporalExtent>> it = getOperator().bindSecondVar(TestIndexFactory.ONE);
		compareExtentIteratorToExpected(it, answerKey);
	}
}
