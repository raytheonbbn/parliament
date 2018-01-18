// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
package com.bbn.parliament.jena.graph.index.temporal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.StringReader;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.TimeZone;

import org.junit.Test;

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
public class TemporalIndexProcessorTest extends AbstractTemporalTestClass {

	@Override
	public void doSetup() {
	}

	/** Test method for {@link com.bbn.parliament.jena.graph.index.temporal.TemporalIndexProcessor#tripleAdded(com.hp.hpl.jena.graph.Triple, com.hp.hpl.jena.graph.Graph)} */
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
			assertEquals(0, index.size());
		} catch (IndexException e) {
			fail();
		}
		try	{
			model.add(instant, INSTANT_PF, date);
		} catch (DatatypeFormatException e)	{
			fail();
		}
		try {
			assertEquals(1, index.size());
		} catch (IndexException e) {
			fail();
		}

		Iterator<Record<TemporalExtent>> it = index.iterator();
		Record<TemporalExtent> nodeExtent = it.next();
		NiceIterator.close(it);

		assertTrue(instant.asNode().equals(nodeExtent.getKey()));
		TemporalExtent extent = nodeExtent.getValue();
		assertTrue(extent instanceof TemporalInstant);
		TemporalInstant answer = (TemporalInstant) extent;
		assertEquals(dateValue, answer.getInstant());
	}

	// public void testConfigure() {
	// Model model = ModelFactory.createDefaultModel();
	//
	// Resource configuration = model.createResource();
	// configuration.addProperty(TemporalIndexProcessorFactory.TEMPORAL_INDEX_CLASS_PROPERTY,
	// model.createTypedLiteral(MockTemporalIndex.class.getCanonicalName()));
	//
	// index.configure(configuration);
	// assertTrue(index.getIndex() instanceof MockTemporalIndex);
	// }

	@Test
	public void testReadFile() {
		int count = 100;
		StringReader reader = new StringReader(TemporalExtentGenerator
				.generateTestInstantStream(count).toString());
		assertNotNull(reader);
		model.read(reader, "", "N-TRIPLE");
		try {
			//Test reading instances
			assertEquals(count, index.size());

			index.clear();
			assertEquals(0, index.size());
		} catch (IndexException e)	{
			fail();
		}

		reader = new StringReader(TemporalExtentGenerator.generateTestIntervalStream(count).toString());
		assertNotNull(reader);
		model.read(reader, "", "N-TRIPLE");
		try	{
			//Test reading intervals
			assertEquals(count, index.size());
		} catch (IndexException e)	{
			fail();
		}
	}
}
