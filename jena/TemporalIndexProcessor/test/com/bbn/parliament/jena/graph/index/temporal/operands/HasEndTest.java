package com.bbn.parliament.jena.graph.index.temporal.operands;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import com.bbn.parliament.jena.graph.index.Record;
import com.bbn.parliament.jena.graph.index.temporal.Operand;
import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalExtent;
import com.bbn.parliament.jena.graph.index.temporal.query.TestIndexFactory;

public class HasEndTest extends BaseOperandTestClass {

	/** {@inheritDoc}} */
	@Override
	public Operand getOperand() {
		return Operand.HAS_END;
	}

	public void testTestExtents() {
		assertFalse("Two intervals are not valid extents for this operand.",
			getOperator().testExtents(TestIndexFactory.THREE,
				TestIndexFactory.TWO));
		assertTrue("Equal instants must have the same end.",
			getOperator().testExtents(TestIndexFactory.JAN06, TestIndexFactory.ALSO_JAN06));
		assertFalse("These have the same beginning, not end.", getOperator().testExtents(
			TestIndexFactory.FOUR, TestIndexFactory.JAN03));
		assertFalse("The range of this operand must be an instant.", getOperator().testExtents(
			TestIndexFactory.JAN04, TestIndexFactory.TWO));
		assertTrue(getOperator().testExtents(TestIndexFactory.TWO,
			TestIndexFactory.JAN04));
	}

	/** Calls '?x hasEnd JAN04' which should result in TWO, THREE, and NOT JAN04. */
	public void testBindFirstVar() {
		Set<String> answerKey = new TreeSet<>();
		answerKey.add("two");
		answerKey.add("three");
		Iterator<Record<TemporalExtent>> it = getOperator().bindFirstVar(
			TestIndexFactory.JAN04);
		compareExtentIteratorToExpected(it, answerKey);
	}

	/** Calls 'FIVE hasEnd ?x' which should result in JAN05. */
	public void testBindSecondVar() {
		Set<String> answerKey = new TreeSet<>();
		answerKey.add("January 5");
		Iterator<Record<TemporalExtent>> it = getOperator().bindSecondVar(
			TestIndexFactory.FIVE);
		compareExtentIteratorToExpected(it, answerKey);
	}

}
