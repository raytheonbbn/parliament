// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
package com.bbn.parliament.jena.graph.index.temporal;

import java.io.StringReader;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.TimeZone;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.util.iterator.NiceIterator;
import org.apache.jena.vocabulary.RDF;

import com.bbn.parliament.jena.graph.index.IndexException;
import com.bbn.parliament.jena.graph.index.Record;
import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalExtent;
import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalInstant;

/** @author rbattle */
public class TemporalIndexProcessorTest extends AbstractTemporalTestClass {
	private static final Property DATE_TIME_PROP = ResourceFactory.createProperty(
		Constants.DATE_TIME.getURI());
	private static final Resource DATE_TIME_INTERVAL_TYPE = ResourceFactory.createResource(
		Constants.DATE_TIME_INTERVAL.getURI());

	/** Test method for {@link com.bbn.parliament.jena.graph.index.temporal.TemporalIndexProcessor#tripleAdded(com.hp.hpl.jena.graph.Triple, com.hp.hpl.jena.graph.Graph)} */
	public void testTripleAdded() {
		Resource interval = ResourceFactory.createResource("http://test#dt");
		Literal date = ResourceFactory.createTypedLiteral(
			"2008-04-03T00:00:01Z", XSDDatatype.XSDdateTime);
		Calendar c = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
		c.set(2008, 3, 3, 0, 0, 1);
		c.set(Calendar.MILLISECOND, 0);

		long dateValue = c.getTimeInMillis();
		try {
			assertEquals(0, index.size());
		} catch (IndexException e) {
			fail();
		}

		model.add(interval, RDF.type, DATE_TIME_INTERVAL_TYPE);
		try {
			assertEquals(0, index.size());
		} catch (IndexException e) {
			fail();
		}

		model.add(interval, DATE_TIME_PROP, date);
		try {
			assertEquals(1, index.size());
		} catch (IndexException e) {
			fail();
		}

		Iterator<Record<TemporalExtent>> it = index.iterator();
		Record<TemporalExtent> nodeExtent = it.next();
		NiceIterator.close(it);

		assertTrue(interval.asNode().equals(nodeExtent.getKey()));
		TemporalExtent extent = nodeExtent.getValue();
		assertTrue(extent instanceof TemporalInstant);
		TemporalInstant instant = (TemporalInstant) extent;
		assertEquals(dateValue, instant.getInstant());
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

	public void testReadFile() {
		int count = 100;
		StringReader reader = new StringReader(TemporalInstantGenerator
			.generateTestStream(count).toString());
		assertNotNull(reader);
		model.read(reader, "", "N-TRIPLE");
		try {
			assertEquals(count, index.size());
		} catch (IndexException e) {
			fail();
		}
	}
}
