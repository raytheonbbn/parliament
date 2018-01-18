// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.graph.index.temporal.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

import com.bbn.parliament.jena.graph.index.temporal.AbstractTemporalTestClass;
import com.bbn.parliament.jena.graph.index.temporal.Constants;
import com.bbn.parliament.jena.graph.index.temporal.pt.TemporalIndexField;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.RDF;

/** @author dkolas */
public class QueryEdgeCaseTest extends AbstractTemporalTestClass {
	private static final String NS = "http://foo/#";
	private static final String EXTENT_NS = "http://foo/Event#";

	private static int thingCounter;

	@Override
	public void doSetup() {
		loadContent();
	}

	protected static void loadContent() {
		thingCounter = 0;
		addThingWithTime(model, "2001-07-21T00:00:01Z");
		addThingWithTime(model, "2001-07-21T00:00:01Z");
		addThingWithTime(model, "2001-07-21T00:00:02Z");
		addThingWithTime(model, "2001-07-21T00:00:02Z");
		addThingWithTime(model, "1970-01-01T00:00:00Z");
		addThingWithTime(model, "2006-02-16T00:00:01Z");
	}

	@SuppressWarnings("static-method")
	@Test
	public void testDuplicateEntries() {
		String query = COMMON_PREFIXES
				+ "PREFIX foo: <"+NS+">\n"
				+ "SELECT DISTINCT ?thing\n"
				+ "WHERE {\n"
				+ "		?thing foo:atTime ?temporal .\n"
				+ "		?before	pt:asInstant \"2006-02-16T00:00:01Z\"^^xsd:dateTime .\n"
				+ "		?temporal time:before ?before .\n"
				+ "}";
		qExec = QueryExecutionFactory.create(query, graphStore.toDataset());
		ResultSet resultSet = qExec.execSelect();
		checkResults(resultSet, NS+"0", NS+"2", NS+"4", NS+"6", NS+"8");
	}

	@SuppressWarnings("static-method")
	@Test
	/** Tests the query processor's ability to filter through irrelevant triples with similar subjects */
	public void testIndexFilter() {
		String query = COMMON_PREFIXES
				+ "PREFIX foo: <"+NS+">\n"
				+ "SELECT DISTINCT ?thing\n"
				+ "WHERE {\n"
				+ "		?thing foo:atTime ?temporal .\n"
				+ "		?before a time:Instant ;\n"
				+ "			pt:asInstant \"2006-02-16T00:00:01Z\"^^xsd:dateTime .\n"
				+ "		?temporal a time:Instant ;\n"
				+ "			time:before ?before .\n"
				+ "}";
		qExec = QueryExecutionFactory.create(query, graphStore.toDataset());
		ResultSet resultSet = qExec.execSelect();
		checkResults(resultSet, NS+"0", NS+"2", NS+"4", NS+"6", NS+"8");
	}

	@SuppressWarnings("static-method")
	@Test
	/** Tests the query processor's ability to filter through irrelevant triples with similar subjects */
	public void PartialIndexQueryTest() {
		String query = COMMON_PREFIXES
				+ "PREFIX foo: <"+NS+">\n"
				+ "SELECT DISTINCT ?thing\n"
				+ "WHERE {\n"
				+ "		?thing foo:atTime ?temporal .\n"
				+ "		?before a time:Instant ;\n"
				+ "			pt:asInstant \"2006-02-16T00:00:01Z\"^^xsd:dateTime ;\n"
				+ "			?p ?o .\n"
				+ "		?temporal a time:Instant ;\n"
				+ "			time:before ?before .\n"
				+ "}";
		qExec = QueryExecutionFactory.create(query, graphStore.toDataset());
		ResultSet resultSet = qExec.execSelect();
		checkResults(resultSet);
	}

	@SuppressWarnings("static-method")
	@Test
	public void testBlankNodes() {
		String query = COMMON_PREFIXES
				+ "PREFIX foo: <"+NS+">\n"
				+ "SELECT DISTINCT ?thing\n"
				+ "WHERE {\n"
				+ "		?thing foo:atTime ?temporal .\n"
				+ "		?temporal time:before [\n"
				+ "			pt:asInstant \"2006-02-16T00:00:01Z\"^^xsd:dateTime ] .\n"
				+ "}";
		qExec = QueryExecutionFactory.create(query, graphStore.toDataset());
		ResultSet resultSet = qExec.execSelect();
		checkResults(resultSet, NS+"0", NS+"2", NS+"4", NS+"6", NS+"8");
	}

	@SuppressWarnings("static-method")
	@Test
	public void testUnboundedOperands() {
		String query = COMMON_PREFIXES
				+ "PREFIX foo: <"+NS+">\n"
				+ "SELECT DISTINCT ?thing\n"
				+ "WHERE {\n"
				+ "		?thing foo:atTime ?temporal .\n"
				+ "		?interval pt:asInterval \",\"^^<" + Constants.PT_TIME_INTERVAL + "> .\n"
				+ "		?interval time:inside ?temporal .\n"
				+ "}";
		qExec = QueryExecutionFactory.create(query, graphStore.toDataset());
		ResultSet resultSet = qExec.execSelect();
		checkResults(resultSet, NS+"0", NS+"2", NS+"4", NS+"6", NS+"8", NS+"10");
	}

	private static void printQuerySolution(QuerySolution querySolution) {
		if (LOG.isInfoEnabled()) {
			LOG.info("QuerySolution:");
			for (Iterator<String> it = querySolution.varNames(); it.hasNext();) {
				String var = it.next();
				LOG.info("{} -> {}", var, querySolution.get(var));
			}
		}
	}

	private static void checkResults(ResultSet rs, String... results) {
		List<String> values = new ArrayList<>(Arrays.asList(results));
		int count = 0;
		try {
			if (results.length > 0) {
				assertTrue("No results. Expected " + results.length, rs.hasNext());
			} else {
				assertFalse("Has results when none are expected", rs.hasNext());
			}
		} finally {
			while (rs.hasNext()) {
				QuerySolution qs = rs.nextSolution();
				printQuerySolution(qs);
				Resource loc = qs.getResource("thing");
				values.remove(loc.getURI());
				count++;
			}
			if (values.size() > 0) {
				LOG.warn("Did not find the following values: {}", values);
			}
			assertEquals(results.length, count);
			assertEquals(0, values.size());
		}
	}

	private static void addThingWithTime(Model m, String string) {
		Resource thing = m.createResource(NS+(thingCounter++));
		Resource temporal = m.createResource(EXTENT_NS+(thingCounter++));
		Property atTime = m.createProperty(NS+"atTime");

		m.add(thing, atTime, temporal);
		m.add(temporal, RDF.type, m.createResource(Constants.OT_INSTANT.getURI()));
		m.add(temporal, INSTANT_PF,
			ResourceFactory.createTypedLiteral(string, TemporalIndexField.INSTANT.getDatatype()));
	}
}
