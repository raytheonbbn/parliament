package com.bbn.parliament.jena.graph.index.temporal;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.bbn.parliament.kb_graph.query.index.IndexTestMethods;

public class TemporalIndexTest {
	@SuppressWarnings("static-method")
	@ParameterizedTest
	@EnumSource(IndexTestMethods.IndexUnderTest.class)
	public void testAddAndRemove(IndexTestMethods.IndexUnderTest iut) {
		try (TemporalIndexTestMethods testMethods = new TemporalIndexTestMethods()) {
			testMethods.testAddAndRemove(testMethods.getIndex(iut));
		}
	}

	@SuppressWarnings("static-method")
	@ParameterizedTest
	@EnumSource(IndexTestMethods.IndexUnderTest.class)
	public void testOpenClose(IndexTestMethods.IndexUnderTest iut) {
		try (TemporalIndexTestMethods testMethods = new TemporalIndexTestMethods()) {
			testMethods.testOpenClose(testMethods.getIndex(iut));
		}
	}

	@SuppressWarnings("static-method")
	@ParameterizedTest
	@EnumSource(IndexTestMethods.IndexUnderTest.class)
	public void testIterator(IndexTestMethods.IndexUnderTest iut) {
		try (TemporalIndexTestMethods testMethods = new TemporalIndexTestMethods()) {
			testMethods.testIterator(testMethods.getIndex(iut));
		}
	}

	@SuppressWarnings("static-method")
	@ParameterizedTest
	@EnumSource(IndexTestMethods.IndexUnderTest.class)
	public void testAddClosed(IndexTestMethods.IndexUnderTest iut) {
		try (TemporalIndexTestMethods testMethods = new TemporalIndexTestMethods()) {
			testMethods.testAddClosed(testMethods.getIndex(iut));
		}
	}

	@SuppressWarnings("static-method")
	@ParameterizedTest
	@EnumSource(IndexTestMethods.IndexUnderTest.class)
	public void testRemoveClosed(IndexTestMethods.IndexUnderTest iut) {
		try (TemporalIndexTestMethods testMethods = new TemporalIndexTestMethods()) {
			testMethods.testRemoveClosed(testMethods.getIndex(iut));
		}
	}

	@SuppressWarnings("static-method")
	@ParameterizedTest
	@EnumSource(IndexTestMethods.IndexUnderTest.class)
	public void testIteratorClosed(IndexTestMethods.IndexUnderTest iut) {
		try (TemporalIndexTestMethods testMethods = new TemporalIndexTestMethods()) {
			testMethods.testIteratorClosed(testMethods.getIndex(iut));
		}
	}

	@SuppressWarnings("static-method")
	@ParameterizedTest
	@EnumSource(IndexTestMethods.IndexUnderTest.class)
	public void testDelete(IndexTestMethods.IndexUnderTest iut) {
		try (TemporalIndexTestMethods testMethods = new TemporalIndexTestMethods()) {
			testMethods.testDelete(testMethods.getIndex(iut), testMethods.getGraph(iut),
				testMethods.getGraphName(iut));
		}
	}

	@SuppressWarnings("static-method")
	@ParameterizedTest
	@EnumSource(IndexTestMethods.IndexUnderTest.class)
	public void testDeleteOpen(IndexTestMethods.IndexUnderTest iut) {
		try (TemporalIndexTestMethods testMethods = new TemporalIndexTestMethods()) {
			testMethods.testDeleteOpen(testMethods.getIndex(iut));
		}
	}

	@SuppressWarnings("static-method")
	@ParameterizedTest
	@EnumSource(IndexTestMethods.IndexUnderTest.class)
	public void testClear(IndexTestMethods.IndexUnderTest iut) {
		try (TemporalIndexTestMethods testMethods = new TemporalIndexTestMethods()) {
			testMethods.testClear(testMethods.getIndex(iut));
		}
	}

	@SuppressWarnings("static-method")
	@ParameterizedTest
	@EnumSource(IndexTestMethods.IndexUnderTest.class)
	public void testLookup(IndexTestMethods.IndexUnderTest iut) {
		try (TemporalIndexTestMethods testMethods = new TemporalIndexTestMethods()) {
			testMethods.testLookup(testMethods.getIndex(iut));
		}
	}
}
