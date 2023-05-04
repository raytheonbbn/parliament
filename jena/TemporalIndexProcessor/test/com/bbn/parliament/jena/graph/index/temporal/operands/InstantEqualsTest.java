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

public class InstantEqualsTest extends BaseOperandTestClass {
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

	/** {@inheritDoc}} */
	@Override
	public Operand getOperand() {
		return Operand.INSTANT_EQUALS;
	}

	@Test
	public void testTestExtents() {
		assertFalse(getOperator().testExtents(TestIndexFactory.JAN03, TestIndexFactory.JAN04),
			"These instants are not equal.");
		assertTrue(getOperator().testExtents(TestIndexFactory.JAN06, TestIndexFactory.ALSO_JAN06),
			"These instants are equal.");
		assertTrue(getOperator().testExtents(TestIndexFactory.JAN06, TestIndexFactory.JAN06),
			"These are the same two instants.");
		assertFalse(pf.testExtents(TestIndexFactory.ONE, TestIndexFactory.ONE),
			"This test involves incompatible datatypes and therefore must be false.");
	}

	/** Calls '?x equals JAN06' which should result in JAN06 and Also JAN06. */
	@Test
	public void testBindFirstVar() {
		Set<String> answerKey = new TreeSet<>();
		answerKey.add("January 6");
		answerKey.add("Also January 6");
		Iterator<Record<TemporalExtent>> it = getOperator().bindFirstVar(TestIndexFactory.JAN06);
		compareExtentIteratorToExpected(it, answerKey);
	}

	/** Calls 'Also JAN06 equals ?x' which should result in Also JAN06 and JAN06. */
	@Test
	public void testBindSecondVar() {
		Set<String> answerKey = new TreeSet<>();
		answerKey.add("Also January 6");
		answerKey.add("January 6");
		Iterator<Record<TemporalExtent>> it = getOperator().bindSecondVar(TestIndexFactory.ALSO_JAN06);
		compareExtentIteratorToExpected(it, answerKey);
	}
}
