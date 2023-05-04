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

public class IntervalStartedByTest extends BaseOperandTestClass {
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
		return Operand.INTERVAL_STARTED_BY;
	}

	@Test
	public void testTestExtents() {
		assertFalse(getOperator().testExtents(TestIndexFactory.FOUR, TestIndexFactory.THREE),
			"These intervals match 'overlappedBy'.");
		assertFalse(getOperator().testExtents(TestIndexFactory.ONE, TestIndexFactory.SIX),
			"These are equal.");
		assertFalse(getOperator().testExtents(TestIndexFactory.THREE, TestIndexFactory.FOUR),
			"These overlap.");
		assertFalse(getOperator().testExtents(TestIndexFactory.TWO, TestIndexFactory.FOUR),
			"These should only match 'starts'.");
		assertTrue(getOperator().testExtents(TestIndexFactory.FOUR, TestIndexFactory.TWO));
		assertFalse(pf.testExtents(TestIndexFactory.FOUR, TestIndexFactory.JAN03),
			"This test involves incompatible datatypes and therefore must be false.");
	}

	/** Calls '?x startedBy TWO' which should result in FOUR. */
	@Test
	public void testBindFirstVar() {
		Set<String> answerKey = new TreeSet<>();
		answerKey.add("four");
		Iterator<Record<TemporalExtent>> it = getOperator().bindFirstVar(
			TestIndexFactory.TWO);
		compareExtentIteratorToExpected(it, answerKey);
	}

	/** Calls 'FOUR startedBy ?x' which should result in TWO. */
	@Test
	public void testBindSecondVar() {
		Set<String> answerKey = new TreeSet<>();
		answerKey.add("two");
		Iterator<Record<TemporalExtent>> it = getOperator().bindSecondVar(
			TestIndexFactory.FOUR);
		compareExtentIteratorToExpected(it, answerKey);
	}
}
