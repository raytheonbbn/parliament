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

public class HasBeginningTest extends BaseOperandTestClass {
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
		return Operand.HAS_BEGINNING;
	}

	@Test
	public void testTestExtents() {
		assertFalse(getOperator().testExtents(TestIndexFactory.FOUR, TestIndexFactory.TWO),
			"Two intervals are not valid extents for this operand.");
		assertTrue(getOperator().testExtents(TestIndexFactory.JAN06, TestIndexFactory.ALSO_JAN06),
			"Equal instants must have the same beginning.");
		assertFalse(getOperator().testExtents(TestIndexFactory.THREE, TestIndexFactory.JAN04),
			"These have the same end, not beginning.");
		assertFalse(getOperator().testExtents(TestIndexFactory.JAN03, TestIndexFactory.TWO),
			"The range of this operand must be an instant.");
		assertTrue(getOperator().testExtents(TestIndexFactory.TWO, TestIndexFactory.JAN03));
	}

	/** Calls '?x hasBeginning JAN04' which should result in FIVE and NOT JAN04. */
	@Test
	public void testBindFirstVar() {
		Set<String> answerKey = new TreeSet<>();
		answerKey.add("five");
		Iterator<Record<TemporalExtent>> it = getOperator().bindFirstVar(
			TestIndexFactory.JAN04);
		compareExtentIteratorToExpected(it, answerKey);
	}

	/** Calls 'TWO hasBeginning ?x' which should result in JAN03. */
	@Test
	public void testBindSecondVar() {
		Set<String> answerKey = new TreeSet<>();
		answerKey.add("January 3");
		Iterator<Record<TemporalExtent>> it = getOperator().bindSecondVar(
			TestIndexFactory.TWO);
		compareExtentIteratorToExpected(it, answerKey);
	}
}
