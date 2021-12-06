// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
package com.bbn.parliament.jena.graph.index.temporal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.StringReader;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.TimeZone;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bbn.parliament.jena.graph.index.IndexException;
import com.bbn.parliament.jena.graph.index.Record;
import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalExtent;
import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalInstant;
import com.bbn.parliament.jena.graph.index.temporal.pt.TemporalIndexField;
import com.hp.hpl.jena.datatypes.DatatypeFormatException;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.util.iterator.NiceIterator;

/** @author rbattle */
public class TemporalIndexProcessorTest {
	private static TemporalTestServer testServer;

	@BeforeAll
	public static void beforeAll() {
		testServer = new TemporalTestServer();
	}

	@AfterAll
	public static void afterAll() {
		testServer.close();
	}

	@SuppressWarnings("static-method")
	@BeforeEach
	public void beforeEach() {
		testServer.setupIndex();
	}

	@SuppressWarnings("static-method")
	@AfterEach
	public void afterEach() {
		testServer.removeIndex();
	}

	/** Test method for {@link com.bbn.parliament.jena.graph.index.temporal.TemporalIndexProcessor#tripleAdded(com.hp.hpl.jena.graph.Triple, com.hp.hpl.jena.graph.Graph)} */
	@SuppressWarnings("static-method")
	@Test
	public void testTripleAdded() {
		Resource instant = ResourceFactory.createResource("http://test#dt");
		Literal date = ResourceFactory.createTypedLiteral(
				"2008-04-03T00:00:01Z", TemporalIndexField.INSTANT.getDatatype());
		Calendar c = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
		c.set(2008, 3, 3, 0, 0, 1);
		c.set(Calendar.MILLISECOND, 0);

		long dateValue = c.getTimeInMillis();
		try {
			assertEquals(0, testServer.getIndex().size());
		} catch (IndexException ex) {
			fail(ex.getMessage());
		}
		try	{
			testServer.getModel().add(instant, TemporalTestServer.INSTANT_PF, date);
		} catch (DatatypeFormatException ex) {
			fail(ex.getMessage());
		}
		try {
			assertEquals(1, testServer.getIndex().size());
		} catch (IndexException ex) {
			fail(ex.getMessage());
		}

		Iterator<Record<TemporalExtent>> it = testServer.getIndex().iterator();
		Record<TemporalExtent> nodeExtent = it.next();
		NiceIterator.close(it);

		assertTrue(instant.asNode().equals(nodeExtent.getKey()));
		TemporalExtent extent = nodeExtent.getValue();
		assertTrue(extent instanceof TemporalInstant);
		TemporalInstant answer = (TemporalInstant) extent;
		assertEquals(dateValue, answer.getInstant());
	}

	@SuppressWarnings("static-method")
	@Test
	public void testReadFile() {
		int count = 100;
		StringReader reader = new StringReader(TemporalExtentGenerator
				.generateTestInstantStream(count).toString());
		assertNotNull(reader);
		testServer.getModel().read(reader, "", "N-TRIPLE");
		try {
			//Test reading instances
			assertEquals(count, testServer.getIndex().size());

			testServer.getIndex().clear();
			assertEquals(0, testServer.getIndex().size());
		} catch (IndexException ex) {
			fail(ex.getMessage());
		}

		reader = new StringReader(TemporalExtentGenerator.generateTestIntervalStream(count).toString());
		assertNotNull(reader);
		testServer.getModel().read(reader, "", "N-TRIPLE");
		try	{
			//Test reading intervals
			assertEquals(count, testServer.getIndex().size());
		} catch (IndexException ex) {
			fail(ex.getMessage());
		}
	}
}
