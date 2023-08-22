// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2023, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.ontology_bundle;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.regex.Pattern;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;

/**
 * A simple functional test for the 'com.bbn.parliament.ontology_bundle.OntologyBundle' plugin.
 */
public class OntologyBundlePluginFunctionalTest {
	@SuppressWarnings("static-method")
	@Test
	public void basicBuildTest() {
		// Setup the test build
		File testProjectDir = getTestDir();
		testProjectDir.mkdirs();
		FileUtil.writeString(new File(testProjectDir, "settings.gradle"), """
			pluginManagement {
				includeBuild '../../..'
			}

			rootProject.name = 'basicBuildTest'
			""");
		FileUtil.writeString(new File(testProjectDir, "build.gradle"), """
			plugins {
				id 'com.bbn.parliament.ontology_bundle.OntologyBundle'
			}

			group = 'com.bbn.parliament.test'
			version = '0.1.0'

			ontologyBundle {
				prefixes = [
					// prefix, class (blank for none), namespace
					'ao, , http://purl.org/ontology/ao/core#',
					'bfo, BFO, http://purl.obolibrary.org/obo/bfo.owl#',
					'bio, , http://purl.org/vocab/bio/0.1/',
					'cc, , http://web.resource.org/cc/',
					'dc, DC, http://purl.org/dc/elements/1.1/',
					'dcam, DCAM, http://purl.org/dc/dcam/',
					'dcat, , http://www.w3.org/ns/dcat#',
					'dcmitype, , http://purl.org/dc/dcmitype/',
					'dct, DCT, http://purl.org/dc/terms/',
					'dul, DUL, http://www.ontologydesignpatterns.org/ont/dul/DUL.owl#',
					'event, , http://purl.org/NET/c4dm/event.owl#',
					'foaf, FOAF, http://xmlns.com/foaf/0.1/',
					'frbr, , http://purl.org/vocab/frbr/core#',
					'geo, GEO, http://www.opengis.net/ont/geosparql#',
					'geo_old, , http://www.w3.org/2003/01/geo/wgs84_pos#',
					'gml, GML, http://www.opengis.net/ont/gml#',
					'gr, GR, http://purl.org/goodrelations/v1#',
					'keys, , http://purl.org/NET/c4dm/keys.owl#',
					'mo, MO, http://purl.org/ontology/mo/',
					'obo, OBO, http://purl.obolibrary.org/obo/',
					'owl, , http://www.w3.org/2002/07/owl#',
					'provo, ProvO, http://www.w3.org/ns/prov#',
					'rdf, , http://www.w3.org/1999/02/22-rdf-syntax-ns#',
					'rdfs, , http://www.w3.org/2000/01/rdf-schema#',
					'schema, Schema, http://schema.org/',
					'sf, SF, http://www.opengis.net/ont/sf#',
					'sioc, SIOC, http://rdfs.org/sioc/ns#',
					'skos, SKOS, http://www.w3.org/2004/02/skos/core#',
					'sosa, SOSA, http://www.w3.org/ns/sosa/',
					'ssn, SSN, http://www.w3.org/ns/ssn/',
					'time, Time, http://www.w3.org/2006/time#',
					'vann, , http://purl.org/vocab/vann/',
					'voaf, , http://purl.org/vocommons/voaf#',
					'void, , http://rdfs.org/ns/void#',
					'vs, , http://www.w3.org/2003/06/sw-vocab-status/ns#',
					'wot, , http://xmlns.com/wot/0.1/',
					'xsd, , http://www.w3.org/2001/XMLSchema#',
				]
				ontologySources = fileTree(dir: "$projectDir/../../../test-ontology",
					includes: [ '**/*.ttl', '**/*.rdf', '**/*.owl' ],
					//exclude: '**/*-original*'
				)
				ontologyIri = 'http://bbn.com/ix/ontology-bundle/functional-test'
				ontologyVersion = project.version
				generatedCodePackageName = 'com.bbn.ix.ontology_bundle.functional_test'
			}
			""");

		// Run the build
		BuildResult result = GradleRunner.create()
			.forwardOutput()
			.withPluginClasspath()
			.withArguments("clean", "build")
			.withProjectDir(testProjectDir)
			.build();

		// Verify the result
		assertTrue(result.getOutput().contains("Generating GEO.java"));
		assertTrue(result.getOutput().contains("BUILD SUCCESSFUL in"));
		assertTrue(Pattern
			.compile("(?m)^Writing [a-zA-Z0-9_]+ file '.*/OntologyForMachines\\.ttl'$")
			.matcher(result.getOutput())
			.find());
	}

	private static File getTestDir() {
		var ste = new Exception().getStackTrace()[1];
		return new File("build/%1$s/%2$s".formatted(
			OntologyBundlePluginFunctionalTest.class.getSimpleName(), ste.getMethodName()));
	}
}
