// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.graph.index.temporal.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.Reader;
import java.io.StringReader;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bbn.parliament.jena.graph.index.temporal.Constants;
import com.bbn.parliament.jena.graph.index.temporal.TemporalTestServer;
import com.bbn.parliament.jena.joseki.client.CloseableQueryExec;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.vocabulary.XSD;

/** @author dkolas */
public class DuplicateEntriesTest {
	private static final String TRIPLES = """
		@prefix ex:  <http://example.org/example#> .
		@prefix pt:  <%1$s> .
		@prefix xsd: <%2$s> .
		ex:thing0 pt:temporalExtent ex:instant0 .
		ex:instant0 pt:asInstant "2001-07-21T00:00:01"^^xsd:dateTime .
		ex:thing1 pt:temporalExtent ex:instant1 .
		ex:instant1 pt:asInstant "2001-07-21T00:00:01"^^xsd:dateTime .
		ex:thing2 pt:temporalExtent ex:instant2 .
		ex:instant2 pt:asInstant "2001-07-21T00:00:02"^^xsd:dateTime .
		ex:thing3 pt:temporalExtent ex:instant3 .
		ex:instant3 pt:asInstant "2001-07-21T00:00:02"^^xsd:dateTime .
		""".formatted(Constants.PT_NS, XSD.getURI());
	private static final String QUERY = """
		prefix time: <%1$s>
		prefix pt:   <%2$s>
		prefix xsd:  <%3$s>
		select distinct ?thing where {
			?latestTime pt:asInstant "2006-02-16T00:00:01"^^xsd:dateTime .
			?thing pt:temporalExtent ?instant .
			?instant time:before ?latestTime .
		}
		""".formatted(Constants.OT_NS, Constants.PT_NS, XSD.getURI());

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

	@SuppressWarnings("static-method")
	@Test
	public void testDuplicateEntries() throws Exception {
		try (Reader rdr = new StringReader(TRIPLES)) {
			testServer.getModel().read(rdr, null, "TURTLE");
		}
		try (CloseableQueryExec qe = new CloseableQueryExec(testServer.getDataset(), QUERY)) {
			ResultSet resultSet = qe.execSelect();
			int count = 0;
			while (resultSet.hasNext()){
				resultSet.next();
				++count;
			}
			assertEquals(4, count, "Count did not equal expected count.");
		}
	}
}
