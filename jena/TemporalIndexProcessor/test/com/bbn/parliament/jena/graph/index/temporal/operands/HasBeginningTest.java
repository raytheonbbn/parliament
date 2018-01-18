package com.bbn.parliament.jena.graph.index.temporal.operands;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;

import com.bbn.parliament.jena.graph.index.Record;
import com.bbn.parliament.jena.graph.index.temporal.Operand;
import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalExtent;
import com.bbn.parliament.jena.graph.index.temporal.query.TestIndexFactory;

public class HasBeginningTest extends BaseOperandTestClass {

	/** {@inheritDoc}} */
	@Override
	public Operand getOperand() {
		return Operand.HAS_BEGINNING;
	}

	@Test
	public void testTestExtents() {
		assertFalse("Two intervals are not valid extents for this operand.",
			getOperator().testExtents(TestIndexFactory.FOUR,
				TestIndexFactory.TWO));
		assertTrue("Equal instants must have the same beginning.",
			getOperator().testExtents(TestIndexFactory.JAN06, TestIndexFactory.ALSO_JAN06));
		assertFalse("These have the same end, not beginning.", getOperator().testExtents(
			TestIndexFactory.THREE, TestIndexFactory.JAN04));
		assertFalse("The range of this operand must be an instant.", getOperator().testExtents(
			TestIndexFactory.JAN03, TestIndexFactory.TWO));
		assertTrue(getOperator().testExtents(TestIndexFactory.TWO,
			TestIndexFactory.JAN03));
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
