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

public class IntervalOverlappedByTest extends BaseOperandTestClass {
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
		return Operand.INTERVAL_OVERLAPPED_BY;
	}

	@Test
	public void testTestExtents() {
		assertFalse(getOperator().testExtents(TestIndexFactory.THREE, TestIndexFactory.FOUR),
			"These intervals overlap.");
		assertFalse(getOperator().testExtents(TestIndexFactory.FOUR, TestIndexFactory.TWO),
			"One interval starts the other, so 'overlappedBy' does not hold.");
		assertFalse(getOperator().testExtents(TestIndexFactory.ONE, TestIndexFactory.SIX),
			"These intervals are equal.");
		assertFalse(getOperator().testExtents(TestIndexFactory.FIVE, TestIndexFactory.FOUR),
			"One interval contains the other.");
		assertFalse(getOperator().testExtents(TestIndexFactory.THREE, TestIndexFactory.ONE),
			"These intervals share a boundary.");
		assertTrue(getOperator().testExtents(TestIndexFactory.FOUR, TestIndexFactory.THREE));
		assertFalse(pf.testExtents(TestIndexFactory.FOUR, TestIndexFactory.JAN05),
			"This test involves incompatible datatypes and therefore must be false.");
	}

	/** Calls '?x overlappedBy THREE' which should result in FOUR. */
	@Test
	public void testBindFirstVar() {
		Set<String> answerKey = new TreeSet<>();
		answerKey.add("four");
		Iterator<Record<TemporalExtent>> it = getOperator().bindFirstVar(
			TestIndexFactory.THREE);
		compareExtentIteratorToExpected(it, answerKey);
	}

	/** Calls 'FOUR overlappedBy ?x' which should result in THREE. */
	@Test
	public void testBindSecondVar() {
		Set<String> answerKey = new TreeSet<>();
		answerKey.add("three");
		Iterator<Record<TemporalExtent>> it = getOperator().bindSecondVar(
			TestIndexFactory.FOUR);
		compareExtentIteratorToExpected(it, answerKey);
	}
}
