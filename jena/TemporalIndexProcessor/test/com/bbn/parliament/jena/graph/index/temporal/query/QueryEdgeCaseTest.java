// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.graph.index.temporal.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jena.graph.index.temporal.Constants;
import com.bbn.parliament.jena.graph.index.temporal.TemporalTestServer;
import com.bbn.parliament.jena.graph.index.temporal.pt.TemporalIndexField;
import com.bbn.parliament.jena.joseki.client.CloseableQueryExec;

/** @author dkolas */
public class QueryEdgeCaseTest {
	private static final String NS = "http://foo/#";
	private static final String EXTENT_NS = "http://foo/Event#";
	private static final String COMMON_PREFIXES = TemporalTestServer.COMMON_PREFIXES + """
		prefix foo: <%1$s>
		""".formatted(NS);
	private static final Logger LOG = LoggerFactory.getLogger(QueryEdgeCaseTest.class);

	private static TemporalTestServer testServer;
	private static int thingCounter;

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
		loadContent();
	}

	@SuppressWarnings("static-method")
	@AfterEach
	public void afterEach() {
		testServer.removeIndex();
	}

	protected static void loadContent() {
		thingCounter = 0;
		addThingWithTime(testServer.getModel(), "2001-07-21T00:00:01Z");
		addThingWithTime(testServer.getModel(), "2001-07-21T00:00:01Z");
		addThingWithTime(testServer.getModel(), "2001-07-21T00:00:02Z");
		addThingWithTime(testServer.getModel(), "2001-07-21T00:00:02Z");
		addThingWithTime(testServer.getModel(), "1970-01-01T00:00:00Z");
		addThingWithTime(testServer.getModel(), "2006-02-16T00:00:01Z");
	}

	@SuppressWarnings("static-method")
	@Test
	public void testDuplicateEntries() {
		String query = COMMON_PREFIXES + """
				select distinct ?thing where {
					?thing foo:atTime ?temporal .
					?before	pt:asInstant "2006-02-16T00:00:01Z"^^xsd:dateTime .
					?temporal time:before ?before .
				}
				""";
		try (CloseableQueryExec qExec = new CloseableQueryExec(testServer.getDataset(), query)) {
			ResultSet resultSet = qExec.execSelect();
			checkResults(resultSet, NS+"0", NS+"2", NS+"4", NS+"6", NS+"8");
		}
	}

	@SuppressWarnings("static-method")
	@Test
	/** Tests the query processor's ability to filter through irrelevant triples with similar subjects */
	public void testIndexFilter() {
		String query = COMMON_PREFIXES + """
				select distinct ?thing where {
					?thing foo:atTime ?temporal .
					?before a time:Instant ;
						pt:asInstant "2006-02-16T00:00:01Z"^^xsd:dateTime .
					?temporal a time:Instant ;
						time:before ?before .
				}
				""";
		try (CloseableQueryExec qExec = new CloseableQueryExec(testServer.getDataset(), query)) {
			ResultSet resultSet = qExec.execSelect();
			checkResults(resultSet, NS+"0", NS+"2", NS+"4", NS+"6", NS+"8");
		}
	}

	@SuppressWarnings("static-method")
	@Test
	/** Tests the query processor's ability to filter through irrelevant triples with similar subjects */
	public void PartialIndexQueryTest() {
		String query = COMMON_PREFIXES + """
				select distinct ?thing where {
					?thing foo:atTime ?temporal .
					?before a time:Instant ;
						pt:asInstant "2006-02-16T00:00:01Z"^^xsd:dateTime ;
						?p ?o .
					?temporal a time:Instant ;
						time:before ?before .
				}
				""";
		try (CloseableQueryExec qExec = new CloseableQueryExec(testServer.getDataset(), query)) {
			ResultSet resultSet = qExec.execSelect();
			checkResults(resultSet);
		}
	}

	@SuppressWarnings("static-method")
	@Test
	public void testBlankNodes() {
		String query = COMMON_PREFIXES + """
				select distinct ?thing where {
					?thing foo:atTime ?temporal .
					?temporal time:before [
						pt:asInstant "2006-02-16T00:00:01Z"^^xsd:dateTime ] .
				}
				""";
		try (CloseableQueryExec qExec = new CloseableQueryExec(testServer.getDataset(), query)) {
			ResultSet resultSet = qExec.execSelect();
			checkResults(resultSet, NS+"0", NS+"2", NS+"4", NS+"6", NS+"8");
		}
	}

	@SuppressWarnings("static-method")
	@Test
	public void testUnboundedOperands() {
		String query = COMMON_PREFIXES + """
				select distinct ?thing where {
					?thing foo:atTime ?temporal .
					?interval pt:asInterval ","^^pt:intervalLiteral .
					?interval time:inside ?temporal .
				}
				""";
		try (CloseableQueryExec qExec = new CloseableQueryExec(testServer.getDataset(), query)) {
			ResultSet resultSet = qExec.execSelect();
			checkResults(resultSet, NS+"0", NS+"2", NS+"4", NS+"6", NS+"8", NS+"10");
		}
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
		Set<String> values = new HashSet<>();
		for (String result : results) {
			values.add(result);
		}

		int count = 0;
		try {
			if (results.length > 0) {
				assertTrue(rs.hasNext(), "No results. Expected " + results.length);
			} else {
				assertFalse(rs.hasNext(), "Has results when none are expected");
			}
		} finally {
			while (rs.hasNext()) {
				QuerySolution qs = rs.nextSolution();
				printQuerySolution(qs);
				Resource loc = qs.getResource("thing");
				values.remove(loc.getURI());
				++count;
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
		m.add(temporal, TemporalTestServer.INSTANT_PF,
			ResourceFactory.createTypedLiteral(string, TemporalIndexField.INSTANT.getDatatype()));
	}
}
