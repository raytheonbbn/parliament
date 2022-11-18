
package com.bbn.parliament.jena.graph.index.numeric;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.bbn.parliament.jena.query.index.IndexTestMethods;

/** @author rbattle */
public class NumericIndexTest {
	@ParameterizedTest
	@EnumSource(IndexTestMethods.IndexUnderTest.class)
	public void testAddAndRemove(IndexTestMethods.IndexUnderTest iut) {
		try (NumericIndexTestMethods testMethods = new NumericIndexTestMethods()) {
			testMethods.testAddAndRemove(testMethods.getIndex(iut));
		}
	}

	@ParameterizedTest
	@EnumSource(IndexTestMethods.IndexUnderTest.class)
	public void testOpenClose(IndexTestMethods.IndexUnderTest iut) {
		try (NumericIndexTestMethods testMethods = new NumericIndexTestMethods()) {
			testMethods.testOpenClose(testMethods.getIndex(iut));
		}
	}

	@ParameterizedTest
	@EnumSource(IndexTestMethods.IndexUnderTest.class)
	public void testIterator(IndexTestMethods.IndexUnderTest iut) {
		try (NumericIndexTestMethods testMethods = new NumericIndexTestMethods()) {
			testMethods.testIterator(testMethods.getIndex(iut));
		}
	}

	@ParameterizedTest
	@EnumSource(IndexTestMethods.IndexUnderTest.class)
	public void testAddClosed(IndexTestMethods.IndexUnderTest iut) {
		try (NumericIndexTestMethods testMethods = new NumericIndexTestMethods()) {
			testMethods.testAddClosed(testMethods.getIndex(iut));
		}
	}

	@ParameterizedTest
	@EnumSource(IndexTestMethods.IndexUnderTest.class)
	public void testRemoveClosed(IndexTestMethods.IndexUnderTest iut) {
		try (NumericIndexTestMethods testMethods = new NumericIndexTestMethods()) {
			testMethods.testRemoveClosed(testMethods.getIndex(iut));
		}
	}

	@ParameterizedTest
	@EnumSource(IndexTestMethods.IndexUnderTest.class)
	public void testIteratorClosed(IndexTestMethods.IndexUnderTest iut) {
		try (NumericIndexTestMethods testMethods = new NumericIndexTestMethods()) {
			testMethods.testIteratorClosed(testMethods.getIndex(iut));
		}
	}

	@ParameterizedTest
	@EnumSource(IndexTestMethods.IndexUnderTest.class)
	public void testDelete(IndexTestMethods.IndexUnderTest iut) {
		try (NumericIndexTestMethods testMethods = new NumericIndexTestMethods()) {
			testMethods.testDelete(testMethods.getIndex(iut), testMethods.getGraph(iut),
				testMethods.getGraphName(iut));
		}
	}

	@ParameterizedTest
	@EnumSource(IndexTestMethods.IndexUnderTest.class)
	public void testDeleteOpen(IndexTestMethods.IndexUnderTest iut) {
		try (NumericIndexTestMethods testMethods = new NumericIndexTestMethods()) {
			testMethods.testDeleteOpen(testMethods.getIndex(iut));
		}
	}

	@ParameterizedTest
	@EnumSource(IndexTestMethods.IndexUnderTest.class)
	public void testClear(IndexTestMethods.IndexUnderTest iut) {
		try (NumericIndexTestMethods testMethods = new NumericIndexTestMethods()) {
			testMethods.testClear(testMethods.getIndex(iut));
		}
	}

	@ParameterizedTest
	@EnumSource(IndexTestMethods.IndexUnderTest.class)
	public void testLookup(IndexTestMethods.IndexUnderTest iut) {
		try (NumericIndexTestMethods testMethods = new NumericIndexTestMethods()) {
			testMethods.testLookup(testMethods.getIndex(iut));
		}
	}

	@ParameterizedTest
	@EnumSource(IndexTestMethods.IndexUnderTest.class)
	public void testFilter(IndexTestMethods.IndexUnderTest iut) {
		try (NumericIndexTestMethods testMethods = new NumericIndexTestMethods()) {
			testMethods.testFilter(testMethods.getIndex(iut), testMethods.getModel(iut));
		}
	}

	@ParameterizedTest
	@EnumSource(IndexTestMethods.IndexUnderTest.class)
	public void testComplexQuery(IndexTestMethods.IndexUnderTest iut) {
		try (NumericIndexTestMethods testMethods = new NumericIndexTestMethods()) {
			testMethods.testComplexQuery(testMethods.getIndex(iut), testMethods.getModel(iut));
		}
	}

	@ParameterizedTest
	@EnumSource(IndexTestMethods.IndexUnderTest.class)
	public void testRangeIterator(IndexTestMethods.IndexUnderTest iut) {
		try (NumericIndexTestMethods testMethods = new NumericIndexTestMethods()) {
			testMethods.testRangeIterator(testMethods.getIndex(iut));
		}
	}
}
