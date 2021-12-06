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

public class IntervalAfterTest extends BaseOperandTestClass {
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
		return Operand.INTERVAL_AFTER;
	}

	@Test
	public void testTestExtents() {
		assertFalse(pf.testExtents(TestIndexFactory.ONE, TestIndexFactory.TWO),
			"This case should fail since it comes 'before' the other interval.");
		assertFalse(pf.testExtents(TestIndexFactory.THREE, TestIndexFactory.ONE),
			"These intervals share a common boundary therefore the 'after' relationship does not apply.");
		assertFalse(pf.testExtents(TestIndexFactory.FOUR, TestIndexFactory.THREE),
			"This test involves two overlapping intervals and therefore 'after' does not hold.");
		assertTrue(pf.testExtents(TestIndexFactory.TWO, TestIndexFactory.ONE));
		assertFalse(pf.testExtents(TestIndexFactory.FOUR, TestIndexFactory.JAN01),
			"This test involves incompatible datatypes and therefore must be false.");
	}


	/** Calls '?x after ONE' which should result in TWO, FOUR, and FIVE */
	@Test
	public void testBindFirstVar() {
		Set<String> answerKey = new TreeSet<>();
		answerKey.add("five");
		answerKey.add("four");
		answerKey.add("two");
		Iterator<Record<TemporalExtent>> it = pf.bindFirstVar(TestIndexFactory.ONE);
		compareExtentIteratorToExpected(it, answerKey);
	}

	/** Calls (FIVE after ?x) which should result in ONE, SIX */
	@Test
	public void testBindSecondVar() {
		Set<String> answerKey = new TreeSet<>();
		answerKey.add("one");
		answerKey.add("six");
		Iterator<Record<TemporalExtent>> it = pf.bindSecondVar(TestIndexFactory.FIVE);
		compareExtentIteratorToExpected(it, answerKey);
	}
}
