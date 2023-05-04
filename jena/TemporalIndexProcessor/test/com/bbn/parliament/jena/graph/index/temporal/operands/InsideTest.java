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

public class InsideTest extends BaseOperandTestClass {
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

	@Override
	public Operand getOperand() {
		return Operand.INSIDE;
	}

	@Test
	public void testTestExtents() {
		assertFalse(getOperator().testExtents(TestIndexFactory.FIVE, TestIndexFactory.FOUR),
			"These extents are not compatible.");
		assertFalse(getOperator().testExtents(TestIndexFactory.JAN05, TestIndexFactory.FOUR),
			"These extents are not compatible.");
		assertTrue(getOperator().testExtents(TestIndexFactory.FOUR, TestIndexFactory.JAN06));
		assertFalse(pf.testExtents(TestIndexFactory.FOUR, TestIndexFactory.JAN02),
			"There is no 'inside' relation here.");
		assertFalse(pf.testExtents(TestIndexFactory.THREE, TestIndexFactory.JAN02),
			"This relation is 'hasBeginning', not 'inside'.");
	}

	/** Calls '?x inside JAN03' which should result in THREE. */
	@Test
	public void testBindFirstVar() {
		Set<String> answerKey = new TreeSet<>();
		answerKey.add("three");
		Iterator<Record<TemporalExtent>> it = getOperator().bindFirstVar(TestIndexFactory.JAN03);
		compareExtentIteratorToExpected(it, answerKey);
	}

	/** Calls 'FOUR inside ?x' which should result in January 4, 5, 6, and also January 6. */
	@Test
	public void testBindSecondVar() {
		Set<String> answerKey = new TreeSet<>();
		answerKey.add("January 4");
		answerKey.add("January 5");
		answerKey.add("January 6");
		answerKey.add("Also January 6");
		Iterator<Record<TemporalExtent>> it = getOperator().bindSecondVar(TestIndexFactory.FOUR);
		compareExtentIteratorToExpected(it, answerKey);
	}
}
