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

public class AfterTest extends BaseOperandTestClass {
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
		return Operand.AFTER;
	}

	@Test
	public void testTestExtents() {
		assertFalse(pf.testExtents(TestIndexFactory.JAN01, TestIndexFactory.JAN02),
			"This case should fail since it comes 'before' the other instant.");
		assertFalse(pf.testExtents(TestIndexFactory.ONE, TestIndexFactory.JAN03),
			"This case should fail since it comes 'before' the other instant.");
		assertFalse(pf.testExtents(TestIndexFactory.JAN01, TestIndexFactory.THREE),
			"This case should fail since it comes 'before' the other interval.");
		assertFalse(pf.testExtents(TestIndexFactory.JAN06, TestIndexFactory.ALSO_JAN06),
			"This test involves two equal instants and therefore 'after' does not hold.");
		assertTrue(pf.testExtents(TestIndexFactory.FOUR, TestIndexFactory.JAN01),
			"These datatypes ought to be compatible with this operand.");
		assertTrue(pf.testExtents(TestIndexFactory.JAN06, TestIndexFactory.THREE),
			"These datatypes ought to be compatible with this operand.");
		assertTrue(pf.testExtents(TestIndexFactory.JAN02, TestIndexFactory.JAN01));
		assertTrue(pf.testExtents(TestIndexFactory.FOUR, TestIndexFactory.ONE),
			"These datatypes ought to be compatible with this operand.");
	}

	/** Calls '?x after JAN02' which should result in all instants and intervals after JAN02. */
	@Test
	public void testBindFirstInstantVar() {
		Set<String> answerKey = new TreeSet<>();
		answerKey.add("January 3");
		answerKey.add("January 4");
		answerKey.add("January 5");
		answerKey.add("January 6");
		answerKey.add("Also January 6");
		answerKey.add("two");
		answerKey.add("four");
		answerKey.add("five");
		Iterator<Record<TemporalExtent>> it = pf.bindFirstVar(TestIndexFactory.JAN02);
		compareExtentIteratorToExpected(it, answerKey);
	}

	/** Calls (JAN05 after ?x) which should result in all instants and intervals before JAN05. */
	@Test
	public void testBindSecondInstantVar() {
		Set<String> answerKey = new TreeSet<>();
		answerKey.add("January 1");
		answerKey.add("January 2");
		answerKey.add("January 3");
		answerKey.add("January 4");
		answerKey.add("one");
		answerKey.add("six");
		answerKey.add("two");
		answerKey.add("three");
		Iterator<Record<TemporalExtent>> it = pf.bindSecondVar(TestIndexFactory.JAN05);
		compareExtentIteratorToExpected(it, answerKey);
	}

	/** Calls '?x after ONE' which should result in all entities after ONE. */
	@Test
	public void testBindFirstIntervalVar() {
		Set<String> answerKey = new TreeSet<>();
		answerKey.add("January 3");
		answerKey.add("January 4");
		answerKey.add("January 5");
		answerKey.add("January 6");
		answerKey.add("Also January 6");
		answerKey.add("two");
		answerKey.add("four");
		answerKey.add("five");
		Iterator<Record<TemporalExtent>> it = pf.bindFirstVar(TestIndexFactory.ONE);
		compareExtentIteratorToExpected(it, answerKey);
	}

	/** Calls (FIVE after ?x) which should result in all entities before FIVE. */
	@Test
	public void testBindSecondIntervalVar() {
		Set<String> answerKey = new TreeSet<>();
		answerKey.add("January 1");
		answerKey.add("January 2");
		answerKey.add("January 3");
		answerKey.add("one");
		answerKey.add("six");
		Iterator<Record<TemporalExtent>> it = pf.bindSecondVar(TestIndexFactory.FIVE);
		compareExtentIteratorToExpected(it, answerKey);
	}
}
