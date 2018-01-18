package com.bbn.parliament.jena.graph.index.temporal.operands;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import com.bbn.parliament.jena.graph.index.Record;
import com.bbn.parliament.jena.graph.index.temporal.Operand;
import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalExtent;
import com.bbn.parliament.jena.graph.index.temporal.query.TestIndexFactory;

public class BeforeTest extends BaseOperandTestClass {

	/** {@inheritDoc}} */
	@Override
	public Operand getOperand() {
		return Operand.BEFORE;
	}

	public void testTestExtents() {
		assertFalse("This case should fail since it comes 'after' the other instant.",
				getOperator().testExtents(TestIndexFactory.JAN02, TestIndexFactory.JAN01));
		assertFalse("This case should fail since it comes 'after' the other interval.",
				getOperator().testExtents(TestIndexFactory.JAN04, TestIndexFactory.ONE));
		assertFalse("This case should fail since it comes 'after' the other instant.",
				getOperator().testExtents(TestIndexFactory.FOUR, TestIndexFactory.JAN01));
		assertFalse("This test involves two equal instants and therefore 'before' does not hold.",
				getOperator().testExtents(TestIndexFactory.JAN03, TestIndexFactory.JAN03));
		assertTrue("These datatypes ought to be compatible with this operand.",
				pf.testExtents(TestIndexFactory.ONE, TestIndexFactory.FOUR));
		assertTrue(getOperator().testExtents(TestIndexFactory.JAN01, TestIndexFactory.JAN02));
		assertTrue("These datatypes ought to be compatible with this operand.",
				getOperator().testExtents(TestIndexFactory.ONE, TestIndexFactory.JAN03));
		assertTrue("These datatypes ought to be compatible with this operand.",
				getOperator().testExtents(TestIndexFactory.JAN02, TestIndexFactory.TWO));
	}

	/** Calls '?x before JAN05' which should result in all instants and intervals before JAN05. */
	public void testBindFirstInstantVar() {
		Set<String> answerKey = new TreeSet<>();
		answerKey.add("January 1");
		answerKey.add("January 2");
		answerKey.add("January 3");
		answerKey.add("January 4");
		answerKey.add("one");
		answerKey.add("six");
		answerKey.add("two");
		answerKey.add("three");
		Iterator<Record<TemporalExtent>> it = getOperator().bindFirstVar(TestIndexFactory.JAN05);
		compareExtentIteratorToExpected(it, answerKey);
	}

	/** Calls 'JAN01 before ?x' which should result in all instants and intervals after JAN01. */
	public void testBindSecondInstantVar() {
		Set<String> answerKey = new TreeSet<>();
		answerKey.add("January 2");
		answerKey.add("January 3");
		answerKey.add("January 4");
		answerKey.add("January 5");
		answerKey.add("January 6");
		answerKey.add("Also January 6");
		answerKey.add("two");
		answerKey.add("three");
		answerKey.add("four");
		answerKey.add("five");
		Iterator<Record<TemporalExtent>> it = getOperator().bindSecondVar(TestIndexFactory.JAN01);
		compareExtentIteratorToExpected(it, answerKey);
	}

	/** Calls '?x before FIVE' which should result in all entities before FIVE. */
	public void testBindFirstIntervalVar() {
		Set<String> answerKey = new TreeSet<>();
		answerKey.add("January 1");
		answerKey.add("January 2");
		answerKey.add("January 3");
		answerKey.add("one");
		answerKey.add("six");
		Iterator<Record<TemporalExtent>> it = getOperator().bindFirstVar(TestIndexFactory.FIVE);
		compareExtentIteratorToExpected(it, answerKey);
	}

	/** Calls 'ONE before ?x' which should result in all entities after ONE. */
	public void testBindSecondIntervalVar() {
		Set<String> answerKey = new TreeSet<>();
		answerKey.add("January 3");
		answerKey.add("January 4");
		answerKey.add("January 5");
		answerKey.add("January 6");
		answerKey.add("Also January 6");
		answerKey.add("two");
		answerKey.add("four");
		answerKey.add("five");
		Iterator<Record<TemporalExtent>> it = getOperator().bindSecondVar(TestIndexFactory.ONE);
		compareExtentIteratorToExpected(it, answerKey);
	}
}
