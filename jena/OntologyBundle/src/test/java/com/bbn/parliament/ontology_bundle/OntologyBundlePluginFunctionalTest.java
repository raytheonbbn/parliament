// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2023, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.ontology_bundle;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * A simple functional test for the Ontology Bundle plugin.
 */
public class OntologyBundlePluginFunctionalTest {
	private static final String SETTINGS_GRADLE = """
		pluginManagement {
			includeBuild '../../..'
		}

		rootProject.name = '%1$s%2$s'
		""";
	private static final String BUILD_GRADLE = """
		plugins {
			id 'com.bbn.parliament.OntologyBundle'
		}

		group = 'com.bbn.parliament.test'
		version = '0.1.0'

		OntologyBundle {
			prefixes = [
				// prefix, class (blank for none), namespace
				%1$s
				'owl, , http://www.w3.org/2002/07/owl#',
				'rdf, , http://www.w3.org/1999/02/22-rdf-syntax-ns#',
				'rdfs, , http://www.w3.org/2000/01/rdf-schema#',
				'xsd, , http://www.w3.org/2001/XMLSchema#',
			]
			ontologySources = fileTree(dir: "$projectDir/../../../../test-ontology",
				includes: [ '%2$s' ],
				//exclude: '**/*-original*'
			)
			ontologyIri = 'http://bbn.com/ix/OntologyBundle/FunctionalTest'
			generatedCodePackageName = 'com.bbn.parliament.ontology_bundle.functional_test'
		}
		""";

	private static Stream<Arguments> ontTestParams() {
		return Stream.of(
			Arguments.of("bfo", "BFO", "bfo.owl", """
				'bfo, BFO, http://purl.obolibrary.org/obo/bfo.owl#',
				'dc, , http://purl.org/dc/elements/1.1/',
				'foaf, , http://xmlns.com/foaf/0.1/',
				'obo, OBO, http://purl.obolibrary.org/obo/',
				"""),
			Arguments.of("CreativeCommons", "CC", "CreativeCommons.rdf", """
				'cc, CC, http://creativecommons.org/ns#',
				"""),
			Arguments.of("dc", "DC", "dublin-core/**/*.ttl", """
				'dc, DC, http://purl.org/dc/elements/1.1/',
				'dcam, DCAM, http://purl.org/dc/dcam/',
				'dcat, , http://www.w3.org/ns/dcat#',
				'dct, DCT, http://purl.org/dc/terms/',
				'skos, , http://www.w3.org/2004/02/skos/core#',
				"""),
			Arguments.of("dul", "DUL", "DUL.owl", """
				'dc, , http://purl.org/dc/elements/1.1/',
				'dul, DUL, http://www.ontologydesignpatterns.org/ont/dul/DUL.owl#',
				"""),
			Arguments.of("foaf", "FOAF", "foaf.rdf", """
				'dc, , http://purl.org/dc/elements/1.1/',
				'foaf, FOAF, http://xmlns.com/foaf/0.1/',
				'vs, , http://www.w3.org/2003/06/sw-vocab-status/ns#',
				'wot, , http://xmlns.com/wot/0.1/',
				"""),
			Arguments.of("geosparql", "GEO", "geosparql-1_0_1/**/*.rdf", """
				'dc, , http://purl.org/dc/elements/1.1/',
				'geo, GEO, http://www.opengis.net/ont/geosparql#',
				'gml, GML, http://www.opengis.net/ont/gml#',
				'sf, SF, http://www.opengis.net/ont/sf#',
				'skos, , http://www.w3.org/2004/02/skos/core#',
				"""),
			Arguments.of("GoodRelations", "GR", "GoodRelations.owl", """
				'dc, , http://purl.org/dc/elements/1.1/',
				'dct, , http://purl.org/dc/terms/',
				'foaf, , http://xmlns.com/foaf/0.1/',
				'gr, GR, http://purl.org/goodrelations/v1#',
				"""),
			Arguments.of("MarineTLO", "MarineTLO", "marine-tlo/**/*.owl", """
				'dc, , http://purl.org/dc/elements/1.1/',
				'dct, , http://purl.org/dc/terms/',
				'foaf, , http://xmlns.com/foaf/0.1/',
				'imarinetlo, IMarineTLO, http://www.ics.forth.gr/isl/ontology/iMarineTLO/',
				'marinetlo, MarineTLO, http://www.ics.forth.gr/isl/ontology/MarineTLO/',
				'skos, , http://www.w3.org/2004/02/skos/core#',
				'vs, , http://www.w3.org/2003/06/sw-vocab-status/ns#',
				"""),
			Arguments.of("mo", "MO", "musicontology.ttl", """
				'ao, , http://purl.org/ontology/ao/core#',
				'bio, , http://purl.org/vocab/bio/0.1/',
				'cc, , http://web.resource.org/cc/',
				'dc, , http://purl.org/dc/elements/1.1/',
				'dct, , http://purl.org/dc/terms/',
				'event, , http://purl.org/NET/c4dm/event.owl#',
				'foaf, , http://xmlns.com/foaf/0.1/',
				'frbr, , http://purl.org/vocab/frbr/core#',
				'geo, , http://www.w3.org/2003/01/geo/wgs84_pos#',
				'keys, , http://purl.org/NET/c4dm/keys.owl#',
				'mo, MO, http://purl.org/ontology/mo/',
				'time, , http://www.w3.org/2006/time#',
				'vann, , http://purl.org/vocab/vann/',
				'vs, , http://www.w3.org/2003/06/sw-vocab-status/ns#',
				'wot, , http://xmlns.com/wot/0.1/',
				"""),
			Arguments.of("owl-time", "Time", "owl-time.ttl", """
				'dct, , http://purl.org/dc/terms/',
				'time, Time, http://www.w3.org/2006/time#',
				'skos, , http://www.w3.org/2004/02/skos/core#',
				"""),
			Arguments.of("Prov-O", "Prov", "prov-o.ttl", """
				'prov, Prov, http://www.w3.org/ns/prov#',
				"""),
			Arguments.of("QUDT", "QUDT", "qudt/**/*.ttl", """
				'constant, Constant, http://qudt.org/vocab/constant/',
				'cc, , http://creativecommons.org/ns#',
				'cur, Cur, http://qudt.org/vocab/currency/',
				'datatype, DataType, http://qudt.org/vocab/datatype/',
				'dc, , http://purl.org/dc/elements/1.1/',
				'dct, , http://purl.org/dc/terms/',
				'dtype, , http://www.linkedmodel.org/schema/dtype#',
				'mc, , http://www.linkedmodel.org/owl/schema/core#',
				'nist, , http://physics.nist.gov/cuu/',
				'oecc, , http://www.oegov.org/models/common/cc#',
				'org, , http://www.w3.org/ns/org#',
				'prefix, Prefix, http://qudt.org/vocab/prefix/',
				'prov, , http://www.w3.org/ns/prov#',
				'qkdv, QKDV, http://qudt.org/vocab/dimensionvector/',
				'quantitykind, QuantityKind, http://qudt.org/vocab/quantitykind/',
				'qudt, QUDT, http://qudt.org/schema/qudt/',
				'qudt-refdata, QudtRefData, http://qudt.org/vocab/refdata/',
				'qudt-type, QudtType, http://qudt.org/vocab/type/',
				'sh, , http://www.w3.org/ns/shacl#',
				'skos, , http://www.w3.org/2004/02/skos/core#',
				'soqk, SOQK, http://qudt.org/vocab/soqk/',
				'sou, SOU, http://qudt.org/vocab/sou/',
				'unit, Unit, http://qudt.org/vocab/unit/',
				'vaem, , http://www.linkedmodel.org/schema/vaem#',
				'voag, , http://voag.linkedmodel.org/schema/voag#',
				"""),
			Arguments.of("schema.org", "Schema", "schemaorg-current-http.ttl", """
				'dcat, , http://www.w3.org/ns/dcat#',
				'dcmitype, , http://purl.org/dc/dcmitype/',
				'dct, , http://purl.org/dc/terms/',
				'foaf, , http://xmlns.com/foaf/0.1/',
				'schema, Schema, http://schema.org/',
				'skos, , http://www.w3.org/2004/02/skos/core#',
				'void, , http://rdfs.org/ns/void#',
				"""),
			Arguments.of("SIOC", "SIOC", "sioc.owl", """
				'dct, , http://purl.org/dc/terms/',
				'foaf, , http://xmlns.com/foaf/0.1/',
				'sioc, SIOC, http://rdfs.org/sioc/ns#',
				'vs, , http://www.w3.org/2003/06/sw-vocab-status/ns#',
				'wot, , http://xmlns.com/wot/0.1/',
				"""),
			Arguments.of("SKOS", "SKOS", "skos.rdf", """
				'dct, , http://purl.org/dc/terms/',
				'skos, SKOS, http://www.w3.org/2004/02/skos/core#',
				"""),
			Arguments.of("SSN", "SSN", "ssn/**/*.ttl", """
				'dct, , http://purl.org/dc/terms/',
				'foaf, , http://xmlns.com/foaf/0.1/',
				'schema, , http://schema.org/',
				'skos, , http://www.w3.org/2004/02/skos/core#',
				'sosa, SOSA, http://www.w3.org/ns/sosa/',
				'ssn, SSN, http://www.w3.org/ns/ssn/',
				'time, , http://www.w3.org/2006/time#',
				'vann, , http://purl.org/vocab/vann/',
				'voaf, , http://purl.org/vocommons/voaf#',
				"""));
	}

	@SuppressWarnings("static-method")
	@ParameterizedTest
	@MethodSource("ontTestParams")
	public void bundleTest(String testLabel, String genClassName, String filesToInclude, String prefixes) {
		// Setup the test build
		var methodName = getTestMethodName();
		var testProjectDir = getTestDir(methodName, testLabel);
		var settingsFile = new File(testProjectDir, "settings.gradle");
		var buildFile = new File(testProjectDir, "build.gradle");
		FileUtil.writeString(settingsFile, SETTINGS_GRADLE.formatted(
			methodName, testLabel));
		FileUtil.writeString(buildFile, BUILD_GRADLE.formatted(prefixes, filesToInclude));

		// Run the build
		BuildResult result = GradleRunner.create()
			.forwardOutput()
			.withPluginClasspath()
			.withArguments("clean", "build")
			.withProjectDir(testProjectDir)
			.build();

		// Verify the result
		assertTrue(result.getOutput().contains("Generating %1$s.java".formatted(genClassName)));
		assertTrue(result.getOutput().contains("BUILD SUCCESSFUL in"));
		assertTrue(Pattern
			.compile("(?m)^Writing [a-zA-Z0-9_]+ file '.*OntologyForMachines\\.ttl'$")
			.matcher(result.getOutput())
			.find());
	}

	private static File getTestDir(String methodName, String testLabel) {
		return new File("build/%1$s/%2$s/%3$s".formatted(
			OntologyBundlePluginFunctionalTest.class.getSimpleName(), methodName, testLabel));
	}

	private static String getTestMethodName() {
		return new Exception().getStackTrace()[1].getMethodName();
	}
}
