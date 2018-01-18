package com.bbn.parliament.jena.graph.index.temporal.operands;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import com.bbn.parliament.jena.graph.index.Record;
import com.bbn.parliament.jena.graph.index.temporal.Operand;
import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalExtent;
import com.bbn.parliament.jena.graph.index.temporal.query.TestIndexFactory;

public class InsideTest extends BaseOperandTestClass {

	@Override
	public Operand getOperand() {
		return Operand.INSIDE;
	}

	public void testTestExtents() {
		assertFalse("These extents are not compatible.",
			getOperator().testExtents(TestIndexFactory.FIVE, TestIndexFactory.FOUR));
		assertFalse("These extents are not compatible.",
				getOperator().testExtents(TestIndexFactory.JAN05, TestIndexFactory.FOUR));
		assertTrue(getOperator().testExtents(TestIndexFactory.FOUR, TestIndexFactory.JAN06));
		assertFalse("There is no 'inside' relation here.",
				pf.testExtents(TestIndexFactory.FOUR, TestIndexFactory.JAN02));
		assertFalse("This relation is 'hasBeginning', not 'inside'.",
				pf.testExtents(TestIndexFactory.THREE, TestIndexFactory.JAN02));
	}

	/** Calls '?x inside JAN03' which should result in THREE. */
	public void testBindFirstVar() {
		Set<String> answerKey = new TreeSet<>();
		answerKey.add("three");
		Iterator<Record<TemporalExtent>> it = getOperator().bindFirstVar(TestIndexFactory.JAN03);
		compareExtentIteratorToExpected(it, answerKey);
	}

	/** Calls 'FOUR inside ?x' which should result in January 4, 5, 6, and also January 6. */
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
