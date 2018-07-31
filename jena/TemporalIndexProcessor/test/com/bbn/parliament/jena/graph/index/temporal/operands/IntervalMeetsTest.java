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
public class IntervalMeetsTest extends BaseOperandTestClass {
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
		return Operand.INTERVAL_MEETS;
	}

	@Test
	public void testTestExtents() {
		assertFalse(getOperator().testExtents(TestIndexFactory.THREE, TestIndexFactory.FOUR),
			"These intervals overlap.");
		assertFalse(getOperator().testExtents(TestIndexFactory.FOUR, TestIndexFactory.TWO),
			"One interval starts the other, so 'meets' does not hold.");
		assertFalse(getOperator().testExtents(TestIndexFactory.ONE, TestIndexFactory.SIX),
			"These intervals are equal.");
		assertFalse(getOperator().testExtents(TestIndexFactory.ONE, TestIndexFactory.TWO),
			"These intervals do not share a boundary.");
		assertTrue(getOperator().testExtents(TestIndexFactory.ONE, TestIndexFactory.THREE));
		assertFalse(pf.testExtents(TestIndexFactory.ONE, TestIndexFactory.JAN02),
			"This test involves incompatible datatypes and therefore must be false.");
	}

	/** Calls '?x meets THREE' which should result in ONE and SIX. */
	@Test
	public void testBindFirstVar() {
		Set<String> answerKey = new TreeSet<>();
		answerKey.add("one");
		answerKey.add("six");
		Iterator<Record<TemporalExtent>> it = getOperator().bindFirstVar(
			TestIndexFactory.THREE);
		compareExtentIteratorToExpected(it, answerKey);
	}

	/** Calls 'THREE meets ?x' which should result in FIVE. */
	@Test
	public void testBindSecondVar() {
		Set<String> answerKey = new TreeSet<>();
		answerKey.add("five");
		Iterator<Record<TemporalExtent>> it = getOperator().bindSecondVar(
			TestIndexFactory.THREE);
		compareExtentIteratorToExpected(it, answerKey);
	}
}
