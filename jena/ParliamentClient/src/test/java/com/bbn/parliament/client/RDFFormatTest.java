// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2015, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.apache.jena.riot.Lang;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/** @author sallen */
public class RDFFormatTest {
	private static Stream<Arguments> testParseArgs() {
		return Stream.of(
			Arguments.of(RDFFormat.RDFXML,	"RDF/XML"),
			Arguments.of(RDFFormat.RDFXML,	"RDF/XML-ABBREV"),
			Arguments.of(RDFFormat.TURTLE,	"TURTLE"),
			Arguments.of(RDFFormat.TURTLE,	"TTL"),
			Arguments.of(RDFFormat.NTRIPLES,	"N-TRIPLES"),
			Arguments.of(RDFFormat.NTRIPLES,	"NTRIPLES"),
			Arguments.of(RDFFormat.N3,			"N3"),
			Arguments.of(RDFFormat.JSON_LD,	"JSON-LD"),
			Arguments.of(RDFFormat.ZIP,		"ZIP"),
			Arguments.of(RDFFormat.UNKNOWN,	"UNKNOWN"),
			Arguments.of(RDFFormat.RDFXML,	"  rdf/xml"),
			Arguments.of(RDFFormat.RDFXML,	"Rdf/XML-Abbrev  "),
			Arguments.of(RDFFormat.TURTLE,	"\tTurTLE"),
			Arguments.of(RDFFormat.TURTLE,	"ttl\t"),
			Arguments.of(RDFFormat.NTRIPLES,	" \tN-Triples"),
			Arguments.of(RDFFormat.NTRIPLES,	"ntriples\t  "),
			Arguments.of(RDFFormat.N3,			"\t  \tn3   "),
			Arguments.of(RDFFormat.JSON_LD,	"json-ld"),
			Arguments.of(RDFFormat.ZIP,		"Zip"),
			Arguments.of(RDFFormat.UNKNOWN,	"Unknown"),
			Arguments.of(RDFFormat.UNKNOWN,	"\t xYzzY   ")
			);
	}

	@SuppressWarnings("static-method")
	@ParameterizedTest
	@MethodSource("testParseArgs")
	public void testParse(RDFFormat expectedFormat, String stringToParse) {
		assertEquals(expectedFormat, RDFFormat.parse(stringToParse));
	}


	@SuppressWarnings("static-method")
	@Test
	public void testParseOfNull() {
		try {
			RDFFormat.parse(null);
			assertTrue(false, "Should have thrown an NPE");
		} catch (NullPointerException ex) {
			// Do nothing
		}
	}


	private static Stream<Arguments> testParseJenaFormatStringArgs() {
		return Stream.of(
			Arguments.of(RDFFormat.RDFXML,	"RDF/XML"),
			Arguments.of(RDFFormat.RDFXML,	"RDF/XML-ABBREV"),
			Arguments.of(RDFFormat.TURTLE,	"TURTLE"),
			Arguments.of(RDFFormat.TURTLE,	"TTL"),
			Arguments.of(RDFFormat.NTRIPLES,	"N-TRIPLES"),
			Arguments.of(RDFFormat.NTRIPLES,	"NTRIPLES"),
			Arguments.of(RDFFormat.N3,			"N3"),
			Arguments.of(RDFFormat.JSON_LD,	"JSON-LD")
			);
	}

	@SuppressWarnings("static-method")
	@ParameterizedTest
	@MethodSource("testParseJenaFormatStringArgs")
	public void testParseJenaFormatString(RDFFormat expectedFormat, String stringToParse) {
		assertEquals(expectedFormat, RDFFormat.parseJenaFormatString(stringToParse));
	}


	@SuppressWarnings("static-method")
	@Test
	public void testParseJenaFormatStringIllegalValues() {
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


	private static Stream<Arguments> testParseFilenameArgs() {
		return Stream.of(
			Arguments.of(RDFFormat.N3,			"foo.n3"),
			Arguments.of(RDFFormat.N3,			"foo.N3"),
			Arguments.of(RDFFormat.N3,			"a thing.n3"),
			Arguments.of(RDFFormat.TURTLE,			"blah.ttl"),
			Arguments.of(RDFFormat.NTRIPLES,			"blah.nt"),
			Arguments.of(RDFFormat.RDFXML,			"thing.rdf"),
			Arguments.of(RDFFormat.RDFXML,			"thing 2.rdf"),
			Arguments.of(RDFFormat.RDFXML,			"thing.owl"),
			Arguments.of(RDFFormat.RDFXML,			"thing.xml"),
			Arguments.of(RDFFormat.NTRIPLES,			"thing.nt"),
			Arguments.of(RDFFormat.N3,			"thing.rdf.n3"),
			Arguments.of(RDFFormat.N3,			".n3"),
			Arguments.of(RDFFormat.JSON_LD,			"foo.jsonld"),
			Arguments.of(RDFFormat.ZIP,			"foo.zip"),
			Arguments.of(RDFFormat.UNKNOWN,			"thing.txt"),
			Arguments.of(RDFFormat.UNKNOWN,			"boo.fww"),
			Arguments.of(RDFFormat.UNKNOWN,			""),
			Arguments.of(RDFFormat.UNKNOWN,			".")
			);
	}

	@SuppressWarnings("static-method")
	@ParameterizedTest
	@MethodSource("testParseFilenameArgs")
	public void testParseFilename(RDFFormat expectedFormat, String stringToParse) {
		assertEquals(expectedFormat, RDFFormat.parseFilename(stringToParse));
	}


	@SuppressWarnings("static-method")
	@Test
	public void testParseFilenameOfNull() {
		try {
			RDFFormat.parseFilename((String) null);
			assertTrue(false, "Should have thrown an NPE");
		} catch (NullPointerException ex) {
			// Do nothing
		}
	}


	private static Stream<Arguments> testParseMediaTypeArgs() {
		return Stream.of(
			Arguments.of(RDFFormat.RDFXML,	"application/rdf+xml"),
			Arguments.of(RDFFormat.TURTLE,	"   text/turtle ; charset=\"UTF-8\""),
			Arguments.of(RDFFormat.TURTLE,	"application/X-TURTLE"),
			Arguments.of(RDFFormat.NTRIPLES,	"application/n-triples"),
			Arguments.of(RDFFormat.NTRIPLES,	"TEXT/PLAIN; charset=us-ascii"),
			Arguments.of(RDFFormat.N3,			"text/n3"),
			Arguments.of(RDFFormat.JSON_LD,	"application/ld+json"),
			Arguments.of(RDFFormat.ZIP,		"application/zip"),
			Arguments.of(RDFFormat.UNKNOWN,	"xyzzy"),
			Arguments.of(RDFFormat.UNKNOWN,	""),
			Arguments.of(RDFFormat.UNKNOWN,	null)
			);
	}

	@SuppressWarnings("static-method")
	@ParameterizedTest
	@MethodSource("testParseMediaTypeArgs")
	public void testParseMediaType(RDFFormat expectedFormat, String stringToParse) {
		assertEquals(expectedFormat, RDFFormat.parseMediaType(stringToParse));
	}

	@SuppressWarnings("static-method")
	@ParameterizedTest
	@EnumSource(RDFFormat.class)
	public void testJenaLangCompatibility(RDFFormat format) {
		if (format == RDFFormat.ZIP || format == RDFFormat.UNKNOWN) {
			return;
		}
		Lang lang = format.getLang();

		assertEquals(lang.getContentType().getContentTypeStr(), format.getMediaType());

		SortedSet<String> expectedContentTypes = new TreeSet<>(lang.getAltContentTypes());
		SortedSet<String> actualContentTypes = new TreeSet<>(format.getMediaTypes());
		assertEquals(expectedContentTypes, actualContentTypes);

		assertEquals(lang.getFileExtensions().get(0), format.getExtension());

		SortedSet<String> expectedExtensions = new TreeSet<>(lang.getFileExtensions());
		SortedSet<String> actualExtensions = new TreeSet<>(format.getExtensions());
		assertEquals(expectedExtensions, actualExtensions);

		assertEquals(lang.getLabel(), format.getFormatStrs().get(0));
		assertEquals(lang.getName(), format.getFormatStrs().get(0));

		SortedSet<String> expectedNames = new TreeSet<>(lang.getAltNames());
		SortedSet<String> actualNames = new TreeSet<>(format.getFormatStrs());
		assertEquals(expectedNames, actualNames);
	}
}
