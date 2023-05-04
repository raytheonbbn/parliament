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

import com.bbn.parliament.jena.graph.index.temporal.Operand;
import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalExtent;
import com.bbn.parliament.jena.graph.index.temporal.query.TestIndexFactory;
import com.bbn.parliament.kb_graph.index.Record;

public class IntervalOverlapsTest extends BaseOperandTestClass {
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
		return Operand.INTERVAL_OVERLAPS;
	}

	@Test
	public void testTestExtents() {
		assertFalse(getOperator().testExtents(TestIndexFactory.FOUR, TestIndexFactory.THREE),
			"These intervals match 'overlappedBy'.");
		assertFalse(getOperator().testExtents(TestIndexFactory.TWO, TestIndexFactory.FOUR),
			"One interval starts the other, so 'overlaps' does not hold.");
		assertFalse(getOperator().testExtents(TestIndexFactory.ONE, TestIndexFactory.SIX),
			"These intervals are equal.");
		assertFalse(getOperator().testExtents(TestIndexFactory.FOUR, TestIndexFactory.FIVE),
			"One interval contains the other.");
		assertFalse(getOperator().testExtents(TestIndexFactory.ONE, TestIndexFactory.THREE),
			"These intervals share a boundary.");
		assertTrue(getOperator().testExtents(TestIndexFactory.THREE, TestIndexFactory.FOUR));
		assertFalse(pf.testExtents(TestIndexFactory.FOUR, TestIndexFactory.JAN05),
			"This test involves incompatible datatypes and therefore must be false.");
	}

	/** Calls '?x overlaps FOUR' which should result in THREE. */
	@Test
	public void testBindFirstVar() {
		Set<String> answerKey = new TreeSet<>();
		answerKey.add("three");
		Iterator<Record<TemporalExtent>> it = getOperator().bindFirstVar(
			TestIndexFactory.FOUR);
		compareExtentIteratorToExpected(it, answerKey);
	}

	/** Calls 'THREE overlaps ?x' which should result in FOUR. */
	@Test
	public void testBindSecondVar() {
		Set<String> answerKey = new TreeSet<>();
		answerKey.add("four");
		Iterator<Record<TemporalExtent>> it = getOperator().bindSecondVar(
			TestIndexFactory.THREE);
		compareExtentIteratorToExpected(it, answerKey);
	}
}
