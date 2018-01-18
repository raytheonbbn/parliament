// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.graph.index.temporal.query;

import static org.junit.Assert.assertEquals;

import java.io.Reader;
import java.io.StringReader;

import org.junit.Test;

import com.bbn.parliament.jena.graph.index.temporal.AbstractTemporalTestClass;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.ResultSet;

/** @author dkolas */
public class DuplicateEntriesTest extends AbstractTemporalTestClass {
	private static final String TRIPLES = ""
		+ "@prefix ex:  <http://example.org/example#> .\n"
		+ "@prefix pt:  <http://bbn.com/ParliamentTime#> .\n"
		+ "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n"
		+ "\n"
		+ "ex:thing0 pt:temporalExtent ex:instant0 .\n"
		+ "ex:instant0 pt:asInstant \"2001-07-21T00:00:01\"^^xsd:dateTime .\n"
		+ "\n"
		+ "ex:thing1 pt:temporalExtent ex:instant1 .\n"
		+ "ex:instant1 pt:asInstant \"2001-07-21T00:00:01\"^^xsd:dateTime .\n"
		+ "\n"
		+ "ex:thing2 pt:temporalExtent ex:instant2 .\n"
		+ "ex:instant2 pt:asInstant \"2001-07-21T00:00:02\"^^xsd:dateTime .\n"
		+ "\n"
		+ "ex:thing3 pt:temporalExtent ex:instant3 .\n"
		+ "ex:instant3 pt:asInstant \"2001-07-21T00:00:02\"^^xsd:dateTime .";
	private static final String QUERY = ""
		+ "prefix time: <http://www.w3.org/2006/time#>\n"
		+ "prefix pt:   <http://bbn.com/ParliamentTime#>\n"
		+ "prefix xsd:  <http://www.w3.org/2001/XMLSchema#>\n"
		+ "\n"
		+ "select distinct ?thing where {\n"
		+ "	?latestTime pt:asInstant \"2006-02-16T00:00:01\"^^xsd:dateTime .\n"
		+ "	?thing pt:temporalExtent ?instant .\n"
		+ "	?instant time:before ?latestTime .\n"
		+ "}";

	@Override
	public void doSetup() {
	}

	@SuppressWarnings("static-method")
	@Test
	public void testDuplicateEntries() throws Exception {
		try (Reader rdr = new StringReader(TRIPLES)) {
			model.read(rdr, null, "TURTLE");
		}
		QueryExecution qe = QueryExecutionFactory.create(QUERY, model);
		try {
			ResultSet resultSet = qe.execSelect();
			int count = 0;
			while (resultSet.hasNext()){
				resultSet.next();
				++count;
			}
			assertEquals("Count did not equal expected count.", 4, count);
		} finally {
			qe.close();
		}
	}
}
