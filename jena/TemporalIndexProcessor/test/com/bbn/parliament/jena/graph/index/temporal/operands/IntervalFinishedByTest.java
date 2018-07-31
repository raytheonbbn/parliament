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
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.bbn.parliament.jena.graph.index.Record;
import com.bbn.parliament.jena.graph.index.temporal.Operand;
import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalExtent;
import com.bbn.parliament.jena.graph.index.temporal.query.TestIndexFactory;

@RunWith(JUnitPlatform.class)
public class IntervalFinishedByTest extends BaseOperandTestClass {
	@BeforeEach
	public void beforeEach() {
		super.beforeEach();
	}

	@AfterEach
	public void afterEach() {
		super.afterEach();
	}

	/** {@inheritDoc} */
	@Override
	public Operand getOperand() {
		return Operand.INTERVAL_FINISHED_BY;
	}

	@Test
	public void testTestExtents() {
		assertFalse(getOperator().testExtents(TestIndexFactory.FOUR, TestIndexFactory.THREE),
			"These intervals match 'overlappedBy'.");
		assertFalse(getOperator().testExtents(TestIndexFactory.ONE, TestIndexFactory.SIX),
			"These are equal.");
		assertFalse(getOperator().testExtents(TestIndexFactory.THREE, TestIndexFactory.FOUR),
			"These overlap.");
		assertFalse(getOperator().testExtents(TestIndexFactory.FOUR, TestIndexFactory.TWO),
			"These should only match 'startedBy'.");
		assertTrue(getOperator().testExtents(TestIndexFactory.THREE, TestIndexFactory.TWO));
		assertFalse(pf.testExtents(TestIndexFactory.THREE, TestIndexFactory.JAN04),
			"This test involves incompatible datatypes and therefore must be false.");
	}

	/** Calls '?x finishedBy TWO' which should result in THREE. */
	@Test
	public void testBindFirstVar() {
		Set<String> answerKey = new TreeSet<>();
		answerKey.add("three");
		Iterator<Record<TemporalExtent>> it = getOperator().bindFirstVar(
			TestIndexFactory.TWO);
		compareExtentIteratorToExpected(it, answerKey);
	}

	/** Calls 'THREE finishedBy ?x' which should result in TWO. */
	@Test
	public void testBindSecondVar() {
		Set<String> answerKey = new TreeSet<>();
		answerKey.add("two");
		Iterator<Record<TemporalExtent>> it = getOperator().bindSecondVar(
			TestIndexFactory.THREE);
		compareExtentIteratorToExpected(it, answerKey);
	}
}
