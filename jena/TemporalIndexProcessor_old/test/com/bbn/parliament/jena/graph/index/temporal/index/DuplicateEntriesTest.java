// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.graph.index.temporal.index;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;
import org.junit.Test;

import com.bbn.parliament.jena.graph.index.temporal.AbstractTemporalTestClass;
import com.bbn.parliament.jena.graph.index.temporal.Constants;

/** @author dkolas */
public class DuplicateEntriesTest extends AbstractTemporalTestClass {
	private static final String NS = "http://foo/#";
	private static final String INSTANT_NS = "http://foo/Instant#";
	private static final String INSTANT_1 = "2001-07-21T00:00:01";
	private static final String INSTANT_2 = "2001-07-21T00:00:02";
	private static final String QUERY = """
		prefix foo:  <%1$s>
		prefix ins:  <%2$s>
		prefix time: <%3$s>
		prefix xsd:  <%4$s#>
		select ?thing where {
			?thing foo:atTime ?temporal .
			?before a time:DateTimeInterval ;
				time:xsdDateTime "2006-02-16T00:00:01"^^xsd:dateTime .
			?temporal a time:DateTimeInterval ;
				time:xsdDateTime ?retired ;
				time:intervalBefore ?before .
		}
		""".formatted(NS, INSTANT_NS, Constants.TIME_NS, XSDDatatype.XSD);

	private static final Resource dtInterval = ResourceFactory.createResource(
		Constants.TIME_NS + "DateTimeInterval");
	private static final Property atTime = ResourceFactory.createProperty(NS + "atTime");
	private static final Property xsdDateTime = ResourceFactory.createProperty(
		Constants.TIME_NS + "xsdDateTime");

	private static int counter = -1;

	@Test
	public void testDuplicateEntries() throws Exception {
		addThingWithTime(model, INSTANT_1);
		addThingWithTime(model, INSTANT_1);
		addThingWithTime(model, INSTANT_2);
		addThingWithTime(model, INSTANT_2);

		LOG.debug("DuplicateEntriesTest query:\n{}", QUERY);
		try (QueryExecution qe = QueryExecutionFactory.create(QUERY, model)) {
			ResultSet resultSet = qe.execSelect();
			int count = 0;
			while (resultSet.hasNext()){
				resultSet.next();
				++count;
			}
			assertEquals("Count did not equal expected count.", 4, count);
		}
	}

	private static void addThingWithTime(Model model, String lexicalForm) {
		Resource thing = ResourceFactory.createResource(NS + (++counter));
		Resource temporal = ResourceFactory.createResource(INSTANT_NS + (++counter));
		Literal lit = ResourceFactory.createTypedLiteral(lexicalForm, XSDDatatype.XSDdateTime);

		model.add(thing, atTime, temporal);
		model.add(temporal, RDF.type, dtInterval);
		model.add(temporal, xsdDateTime, lit);
	}
}
