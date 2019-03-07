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
public class IntervalEqualsTest extends BaseOperandTestClass {
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
		return Operand.INTERVAL_EQUALS;
	}

	@Test
	public void testTestExtents() {
		assertFalse(getOperator().testExtents(TestIndexFactory.THREE, TestIndexFactory.FOUR),
			"These intervals overlap but are not equal.");
		assertFalse(getOperator().testExtents(TestIndexFactory.TWO, TestIndexFactory.FOUR),
			"One interval starts the other but are not equal.");
		assertFalse(getOperator().testExtents(TestIndexFactory.FIVE, TestIndexFactory.FOUR),
			"This is a 'during' relationship rather than 'equals'.");
		assertTrue(getOperator().testExtents(TestIndexFactory.ONE, TestIndexFactory.SIX),
			"These intervals are equal.");
		assertTrue(getOperator().testExtents(TestIndexFactory.ONE, TestIndexFactory.ONE),
			"These are the same two intervals.");
		assertFalse(pf.testExtents(TestIndexFactory.JAN01, TestIndexFactory.JAN01),
			"This test involves incompatible datatypes and therefore must be false.");
	}

	/** Calls '?x equals SIX' which should result in ONE and SIX. */
	@Test
	public void testBindFirstVar() {
		Set<String> answerKey = new TreeSet<>();
		answerKey.add("one");
		answerKey.add("six");
		Iterator<Record<TemporalExtent>> it = getOperator().bindFirstVar(TestIndexFactory.SIX);
		compareExtentIteratorToExpected(it, answerKey);
	}

	/** Calls 'ONE equals ?x' which should result in ONE and SIX. */
	@Test
	public void testBindSecondVar() {
		Set<String> answerKey = new TreeSet<>();
		answerKey.add("six");
		answerKey.add("one");
		Iterator<Record<TemporalExtent>> it = getOperator().bindSecondVar(TestIndexFactory.ONE);
		compareExtentIteratorToExpected(it, answerKey);
	}
}
