// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.graph.index.spatial.sql.postgres;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.bbn.parliament.jena.query.index.IndexTestMethods;

/** @author Robert Battle */
@Disabled
public class PostgresIndexTest {
	@SuppressWarnings("static-method")
	@ParameterizedTest
	@EnumSource(IndexTestMethods.IndexUnderTest.class)
	public void testAddAndRemove(IndexTestMethods.IndexUnderTest iut) {
		try (PostgresIndexTestMethods testMethods = new PostgresIndexTestMethods()) {
			testMethods.testAddAndRemove(testMethods.getIndex(iut));
		}
	}

	@SuppressWarnings("static-method")
	@ParameterizedTest
	@EnumSource(IndexTestMethods.IndexUnderTest.class)
	public void testOpenClose(IndexTestMethods.IndexUnderTest iut) {
		try (PostgresIndexTestMethods testMethods = new PostgresIndexTestMethods()) {
			testMethods.testOpenClose(testMethods.getIndex(iut));
		}
	}

	@SuppressWarnings("static-method")
	@ParameterizedTest
	@EnumSource(IndexTestMethods.IndexUnderTest.class)
	public void testIterator(IndexTestMethods.IndexUnderTest iut) {
		try (PostgresIndexTestMethods testMethods = new PostgresIndexTestMethods()) {
			testMethods.testIterator(testMethods.getIndex(iut));
		}
	}

	@SuppressWarnings("static-method")
	@ParameterizedTest
	@EnumSource(IndexTestMethods.IndexUnderTest.class)
	public void testAddClosed(IndexTestMethods.IndexUnderTest iut) {
		try (PostgresIndexTestMethods testMethods = new PostgresIndexTestMethods()) {
			testMethods.testAddClosed(testMethods.getIndex(iut));
		}
	}

	@SuppressWarnings("static-method")
	@ParameterizedTest
	@EnumSource(IndexTestMethods.IndexUnderTest.class)
	public void testRemoveClosed(IndexTestMethods.IndexUnderTest iut) {
		try (PostgresIndexTestMethods testMethods = new PostgresIndexTestMethods()) {
			testMethods.testRemoveClosed(testMethods.getIndex(iut));
		}
	}

	@SuppressWarnings("static-method")
	@ParameterizedTest
	@EnumSource(IndexTestMethods.IndexUnderTest.class)
	public void testIteratorClosed(IndexTestMethods.IndexUnderTest iut) {
		try (PostgresIndexTestMethods testMethods = new PostgresIndexTestMethods()) {
			testMethods.testIteratorClosed(testMethods.getIndex(iut));
		}
	}

	@SuppressWarnings("static-method")
	@ParameterizedTest
	@EnumSource(IndexTestMethods.IndexUnderTest.class)
	public void testDelete(IndexTestMethods.IndexUnderTest iut) {
		try (PostgresIndexTestMethods testMethods = new PostgresIndexTestMethods()) {
			testMethods.testDelete(testMethods.getIndex(iut), testMethods.getGraph(iut),
				testMethods.getGraphName(iut));
		}
	}

	@SuppressWarnings("static-method")
	@ParameterizedTest
	@EnumSource(IndexTestMethods.IndexUnderTest.class)
	public void testDeleteOpen(IndexTestMethods.IndexUnderTest iut) {
		try (PostgresIndexTestMethods testMethods = new PostgresIndexTestMethods()) {
			testMethods.testDeleteOpen(testMethods.getIndex(iut));
		}
	}

	@SuppressWarnings("static-method")
	@ParameterizedTest
	@EnumSource(IndexTestMethods.IndexUnderTest.class)
	public void testClear(IndexTestMethods.IndexUnderTest iut) {
		try (PostgresIndexTestMethods testMethods = new PostgresIndexTestMethods()) {
			testMethods.testClear(testMethods.getIndex(iut));
		}
	}

	@SuppressWarnings("static-method")
	@ParameterizedTest
	@EnumSource(IndexTestMethods.IndexUnderTest.class)
	public void testLookup(IndexTestMethods.IndexUnderTest iut) {
		try (PostgresIndexTestMethods testMethods = new PostgresIndexTestMethods()) {
			testMethods.testLookup(testMethods.getIndex(iut));
		}
	}

	@SuppressWarnings("static-method")
	@ParameterizedTest
	@EnumSource(IndexTestMethods.IndexUnderTest.class)
	public void testAddGeometry(IndexTestMethods.IndexUnderTest iut) {
		try (PostgresIndexTestMethods testMethods = new PostgresIndexTestMethods()) {
			testMethods.testAddGeometry(testMethods.getIndex(iut));
		}
	}

	@SuppressWarnings("static-method")
	@ParameterizedTest
	@EnumSource(IndexTestMethods.IndexUnderTest.class)
	public void testAddSameNode(IndexTestMethods.IndexUnderTest iut) {
		try (PostgresIndexTestMethods testMethods = new PostgresIndexTestMethods()) {
			testMethods.testAddSameNode(testMethods.getIndex(iut));
		}
	}
}
