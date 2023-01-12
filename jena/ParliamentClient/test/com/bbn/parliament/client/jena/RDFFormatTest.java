// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2015, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.client.jena;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** @author sallen */
public class RDFFormatTest {
	/** Test for {@link com.bbn.parliament.jena.util.RDFFormat#parse(java.lang.String)}. */
	@SuppressWarnings("static-method")
	@Test
	public void testParse() {
		assertEquals(RDFFormat.RDFXML,   RDFFormat.parse("RDF/XML"));
		assertEquals(RDFFormat.RDFXML,   RDFFormat.parse("RDF/XML-ABBREV"));
		assertEquals(RDFFormat.TURTLE,   RDFFormat.parse("TURTLE"));
		assertEquals(RDFFormat.TURTLE,   RDFFormat.parse("TTL"));
		assertEquals(RDFFormat.NTRIPLES, RDFFormat.parse("N-TRIPLES"));
		assertEquals(RDFFormat.NTRIPLES, RDFFormat.parse("NTRIPLES"));
		assertEquals(RDFFormat.N3,       RDFFormat.parse("N3"));
		assertEquals(RDFFormat.JSON_LD,  RDFFormat.parse("JSON-LD"));
		assertEquals(RDFFormat.ZIP,      RDFFormat.parse("ZIP"));
		assertEquals(RDFFormat.UNKNOWN,  RDFFormat.parse("UNKNOWN"));
		assertEquals(RDFFormat.RDFXML,   RDFFormat.parse("  rdf/xml"));
		assertEquals(RDFFormat.RDFXML,   RDFFormat.parse("Rdf/XML-Abbrev  "));
		assertEquals(RDFFormat.TURTLE,   RDFFormat.parse("\tTurTLE"));
		assertEquals(RDFFormat.TURTLE,   RDFFormat.parse("ttl\t"));
		assertEquals(RDFFormat.NTRIPLES, RDFFormat.parse(" \tN-Triples"));
		assertEquals(RDFFormat.NTRIPLES, RDFFormat.parse("ntriples\t  "));
		assertEquals(RDFFormat.N3,       RDFFormat.parse("\t  \tn3   "));
		assertEquals(RDFFormat.JSON_LD,  RDFFormat.parse("json-ld"));
		assertEquals(RDFFormat.ZIP,      RDFFormat.parse("Zip"));
		assertEquals(RDFFormat.UNKNOWN,  RDFFormat.parse("Unknown"));
		assertEquals(RDFFormat.UNKNOWN,  RDFFormat.parse("\t xYzzY   "));

		try {
			RDFFormat.parse(null);
			assertTrue(false, "Should have thrown an NPE");
		} catch (NullPointerException ex) {
			// Do nothing
		}
	}

	/** Test for {@link com.bbn.parliament.jena.util.RDFFormat#parse(java.lang.String)}. */
	@SuppressWarnings("static-method")
	@Test
	public void testParseJenaFormatString() {
		assertEquals(RDFFormat.RDFXML,   RDFFormat.parseJenaFormatString("RDF/XML"));
		assertEquals(RDFFormat.RDFXML,   RDFFormat.parseJenaFormatString("RDF/XML-ABBREV"));
		assertEquals(RDFFormat.TURTLE,   RDFFormat.parseJenaFormatString("TURTLE"));
		assertEquals(RDFFormat.TURTLE,   RDFFormat.parseJenaFormatString("TTL"));
		assertEquals(RDFFormat.NTRIPLES, RDFFormat.parseJenaFormatString("N-TRIPLES"));
		assertEquals(RDFFormat.NTRIPLES, RDFFormat.parseJenaFormatString("NTRIPLES"));
		assertEquals(RDFFormat.N3,       RDFFormat.parseJenaFormatString("N3"));

		try {
			RDFFormat.parseJenaFormatString("JSON-LD");
			assertTrue(false, "Should have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException ex) {
			System.out.println(ex.getMessage());
		}

		try {
			RDFFormat.parseJenaFormatString("ZIP");
			assertTrue(false, "Should have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException ex) {
			System.out.println(ex.getMessage());
		}

		try {
			RDFFormat.parseJenaFormatString("UNKNOWN");
			assertTrue(false, "Should have thrown an IllegalArgumentException");
		} catch (IllegalArgumentException ex) {
			System.out.println(ex.getMessage());
		}
	}

	/** Test for {@link com.bbn.parliament.jena.util.RDFFormat#parseFilename(java.lang.String)}. */
	@SuppressWarnings("static-method")
	@Test
	public void testParseFilename() {
		assertEquals(RDFFormat.N3,       RDFFormat.parseFilename("foo.n3"));
		assertEquals(RDFFormat.N3,       RDFFormat.parseFilename("foo.N3"));
		assertEquals(RDFFormat.N3,       RDFFormat.parseFilename("a thing.n3"));
		assertEquals(RDFFormat.TURTLE,   RDFFormat.parseFilename("blah.ttl"));
		assertEquals(RDFFormat.NTRIPLES, RDFFormat.parseFilename("blah.nt"));
		assertEquals(RDFFormat.RDFXML,   RDFFormat.parseFilename("thing.rdf"));
		assertEquals(RDFFormat.RDFXML,   RDFFormat.parseFilename("thing 2.rdf"));
		assertEquals(RDFFormat.RDFXML,   RDFFormat.parseFilename("thing.owl"));
		assertEquals(RDFFormat.RDFXML,   RDFFormat.parseFilename("thing.xml"));
		assertEquals(RDFFormat.NTRIPLES, RDFFormat.parseFilename("thing.nt"));
		assertEquals(RDFFormat.N3,       RDFFormat.parseFilename("thing.rdf.n3"));
		assertEquals(RDFFormat.N3,       RDFFormat.parseFilename(".n3"));
		assertEquals(RDFFormat.JSON_LD,  RDFFormat.parseFilename("foo.jsonld"));
		assertEquals(RDFFormat.JSON_LD,  RDFFormat.parseFilename("foo.json-ld"));
		assertEquals(RDFFormat.JSON_LD,  RDFFormat.parseFilename("foo.json_ld"));
		assertEquals(RDFFormat.JSON_LD,  RDFFormat.parseFilename("foo.json+ld"));
		assertEquals(RDFFormat.ZIP,      RDFFormat.parseFilename("foo.zip"));
		assertEquals(RDFFormat.UNKNOWN,  RDFFormat.parseFilename("thing.txt"));
		assertEquals(RDFFormat.UNKNOWN,  RDFFormat.parseFilename("boo.fww"));
		assertEquals(RDFFormat.UNKNOWN,  RDFFormat.parseFilename(""));
		assertEquals(RDFFormat.UNKNOWN,  RDFFormat.parseFilename("."));

		try {
			RDFFormat.parseFilename((String) null);
			assertTrue(false, "Should have thrown an NPE");
		} catch (NullPointerException ex) {
			// Do nothing
		}
	}

	/** Test for {@link com.bbn.parliament.jena.util.RDFFormat#parseFilename(java.lang.String)}. */
	@SuppressWarnings("static-method")
	@Test
	public void testParseMediaType() {
		assertEquals(RDFFormat.RDFXML,   RDFFormat.parseMediaType("application/rdf+xml"));
		assertEquals(RDFFormat.TURTLE,   RDFFormat.parseMediaType("   text/turtle ; charset=\"UTF-8\""));
		assertEquals(RDFFormat.TURTLE,   RDFFormat.parseMediaType("application/X-TURTLE"));
		assertEquals(RDFFormat.NTRIPLES, RDFFormat.parseMediaType("application/n-triples"));
		assertEquals(RDFFormat.NTRIPLES, RDFFormat.parseMediaType("TEXT/PLAIN; charset=us-ascii"));
		assertEquals(RDFFormat.N3,       RDFFormat.parseMediaType("text/n3"));
		assertEquals(RDFFormat.JSON_LD,  RDFFormat.parseMediaType("application/ld+json"));
		assertEquals(RDFFormat.JSON_LD,  RDFFormat.parseMediaType("application/json"));
		assertEquals(RDFFormat.ZIP,      RDFFormat.parseMediaType("application/zip"));
		assertEquals(RDFFormat.UNKNOWN,  RDFFormat.parseMediaType("xyzzy"));
		assertEquals(RDFFormat.UNKNOWN,  RDFFormat.parseMediaType(""));
		assertEquals(RDFFormat.UNKNOWN,  RDFFormat.parseMediaType(null));
	}
}
