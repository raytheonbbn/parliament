package com.bbn.parliament.kb_graph.index.numeric.composite;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.bbn.parliament.kb_graph.query.index.IndexTestMethods;

public class CompositeNumericIndexTest {
	@SuppressWarnings("static-method")
	@ParameterizedTest
	@EnumSource(IndexTestMethods.IndexUnderTest.class)
	public void testAddAndRemove(IndexTestMethods.IndexUnderTest iut) {
		try (CompositeNumericIndexTestMethods testMethods = new CompositeNumericIndexTestMethods()) {
			testMethods.testAddAndRemove(testMethods.getIndex(iut));
		}
	}

	@SuppressWarnings("static-method")
	@ParameterizedTest
	@EnumSource(IndexTestMethods.IndexUnderTest.class)
	public void testOpenClose(IndexTestMethods.IndexUnderTest iut) {
		try (CompositeNumericIndexTestMethods testMethods = new CompositeNumericIndexTestMethods()) {
			testMethods.testOpenClose(testMethods.getIndex(iut));
		}
	}

	@SuppressWarnings("static-method")
	@ParameterizedTest
	@EnumSource(IndexTestMethods.IndexUnderTest.class)
	public void testIterator(IndexTestMethods.IndexUnderTest iut) {
		try (CompositeNumericIndexTestMethods testMethods = new CompositeNumericIndexTestMethods()) {
			testMethods.testIterator(testMethods.getIndex(iut));
		}
	}

	@SuppressWarnings("static-method")
	@ParameterizedTest
	@EnumSource(IndexTestMethods.IndexUnderTest.class)
	public void testAddClosed(IndexTestMethods.IndexUnderTest iut) {
		try (CompositeNumericIndexTestMethods testMethods = new CompositeNumericIndexTestMethods()) {
			testMethods.testAddClosed(testMethods.getIndex(iut));
		}
	}

	@SuppressWarnings("static-method")
	@ParameterizedTest
	@EnumSource(IndexTestMethods.IndexUnderTest.class)
	public void testRemoveClosed(IndexTestMethods.IndexUnderTest iut) {
		try (CompositeNumericIndexTestMethods testMethods = new CompositeNumericIndexTestMethods()) {
			testMethods.testRemoveClosed(testMethods.getIndex(iut));
		}
	}

	@SuppressWarnings("static-method")
	@ParameterizedTest
	@EnumSource(IndexTestMethods.IndexUnderTest.class)
	public void testIteratorClosed(IndexTestMethods.IndexUnderTest iut) {
		try (CompositeNumericIndexTestMethods testMethods = new CompositeNumericIndexTestMethods()) {
			testMethods.testIteratorClosed(testMethods.getIndex(iut));
		}
	}

	@SuppressWarnings("static-method")
	@ParameterizedTest
	@EnumSource(IndexTestMethods.IndexUnderTest.class)
	public void testDelete(IndexTestMethods.IndexUnderTest iut) {
		try (CompositeNumericIndexTestMethods testMethods = new CompositeNumericIndexTestMethods()) {
			testMethods.testDelete(testMethods.getIndex(iut), testMethods.getGraph(iut),
				testMethods.getGraphName(iut));
		}
	}

	@SuppressWarnings("static-method")
	@ParameterizedTest
	@EnumSource(IndexTestMethods.IndexUnderTest.class)
	public void testDeleteOpen(IndexTestMethods.IndexUnderTest iut) {
		try (CompositeNumericIndexTestMethods testMethods = new CompositeNumericIndexTestMethods()) {
			testMethods.testDeleteOpen(testMethods.getIndex(iut));
		}
	}

	@SuppressWarnings("static-method")
	@ParameterizedTest
	@EnumSource(IndexTestMethods.IndexUnderTest.class)
	public void testClear(IndexTestMethods.IndexUnderTest iut) {
		try (CompositeNumericIndexTestMethods testMethods = new CompositeNumericIndexTestMethods()) {
			testMethods.testClear(testMethods.getIndex(iut));
		}
	}

	@SuppressWarnings("static-method")
	@ParameterizedTest
	@EnumSource(IndexTestMethods.IndexUnderTest.class)
	public void testSubIndexes(IndexTestMethods.IndexUnderTest iut) {
		try (CompositeNumericIndexTestMethods testMethods = new CompositeNumericIndexTestMethods()) {
			testMethods.testSubIndexes(testMethods.getIndex(iut));
		}
	}
}
