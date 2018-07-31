package com.bbn.parliament.jena.graph.index.temporal.operands;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.bbn.parliament.jena.graph.index.Record;
import com.bbn.parliament.jena.graph.index.temporal.Operand;
import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalExtent;
import com.bbn.parliament.jena.graph.index.temporal.query.TestIndexFactory;

@RunWith(JUnitPlatform.class)
public class HasEndTest extends BaseOperandTestClass {
	@BeforeEach
	public void beforeEach() {
		super.beforeEach();
	}

	@AfterEach
	public void afterEach() {
		super.afterEach();
	}

	/** {@inheritDoc}} */
	@Override
	public Operand getOperand() {
		return Operand.HAS_END;
	}

	@Test
	public void testTestExtents() {
		assertFalse(getOperator().testExtents(TestIndexFactory.THREE, TestIndexFactory.TWO),
			"Two intervals are not valid extents for this operand.");
		assertTrue(getOperator().testExtents(TestIndexFactory.JAN06, TestIndexFactory.ALSO_JAN06),
			"Equal instants must have the same end.");
		assertFalse(getOperator().testExtents(TestIndexFactory.FOUR, TestIndexFactory.JAN03),
			"These have the same beginning, not end.");
		assertFalse(getOperator().testExtents(TestIndexFactory.JAN04, TestIndexFactory.TWO),
			"The range of this operand must be an instant.");
		assertTrue(getOperator().testExtents(TestIndexFactory.TWO, TestIndexFactory.JAN04));
	}

	/** Calls '?x hasEnd JAN04' which should result in TWO, THREE, and NOT JAN04. */
	@Test
	public void testBindFirstVar() {
		Set<String> answerKey = new TreeSet<>();
		answerKey.add("two");
		answerKey.add("three");
		Iterator<Record<TemporalExtent>> it = getOperator().bindFirstVar(
			TestIndexFactory.JAN04);
		compareExtentIteratorToExpected(it, answerKey);
	}

	/** Calls 'FIVE hasEnd ?x' which should result in JAN05. */
	@Test
	public void testBindSecondVar() {
		Set<String> answerKey = new TreeSet<>();
		answerKey.add("January 5");
		Iterator<Record<TemporalExtent>> it = getOperator().bindSecondVar(
			TestIndexFactory.FIVE);
		compareExtentIteratorToExpected(it, answerKey);
	}
}
