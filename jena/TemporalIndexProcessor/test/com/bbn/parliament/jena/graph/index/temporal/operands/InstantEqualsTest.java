package com.bbn.parliament.jena.graph.index.temporal.operands;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import com.bbn.parliament.jena.graph.index.Record;
import com.bbn.parliament.jena.graph.index.temporal.Operand;
import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalExtent;
import com.bbn.parliament.jena.graph.index.temporal.query.TestIndexFactory;

public class InstantEqualsTest extends BaseOperandTestClass {

	/** {@inheritDoc}} */
	@Override
	public Operand getOperand() {
		return Operand.INSTANT_EQUALS;
	}

	public void testTestExtents() {
		assertFalse("These instants are not equal.",
			getOperator().testExtents(TestIndexFactory.JAN03, TestIndexFactory.JAN04));
		assertTrue("These instants are equal.",
			getOperator().testExtents(TestIndexFactory.JAN06, TestIndexFactory.ALSO_JAN06));
		assertTrue("These are the same two instants.",
			getOperator().testExtents(TestIndexFactory.JAN06, TestIndexFactory.JAN06));
		assertFalse("This test involves incompatible datatypes and therefore must be false.",
				pf.testExtents(TestIndexFactory.ONE, TestIndexFactory.ONE));
	}

	/** Calls '?x equals JAN06' which should result in JAN06 and Also JAN06. */
	public void testBindFirstVar() {
		Set<String> answerKey = new TreeSet<>();
		answerKey.add("January 6");
		answerKey.add("Also January 6");
		Iterator<Record<TemporalExtent>> it = getOperator().bindFirstVar(TestIndexFactory.JAN06);
		compareExtentIteratorToExpected(it, answerKey);
	}

	/** Calls 'Also JAN06 equals ?x' which should result in Also JAN06 and JAN06. */
	public void testBindSecondVar() {
		Set<String> answerKey = new TreeSet<>();
		answerKey.add("Also January 6");
		answerKey.add("January 6");
		Iterator<Record<TemporalExtent>> it = getOperator().bindSecondVar(TestIndexFactory.ALSO_JAN06);
		compareExtentIteratorToExpected(it, answerKey);
	}
}
