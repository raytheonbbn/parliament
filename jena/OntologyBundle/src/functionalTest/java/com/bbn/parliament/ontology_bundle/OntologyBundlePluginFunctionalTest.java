package com.bbn.parliament.ontology_bundle;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;

/**
 * A simple functional test for the 'com.bbn.parliament.ontology_bundle.OntologyBundle' plugin.
 */
public class OntologyBundlePluginFunctionalTest {
	@SuppressWarnings("static-method")
	@Test
	public void basicBuildTest() throws IOException {
		// Setup the test build
		File testProjectDir = getTestDir();
		testProjectDir.mkdirs();
		writeString(new File(testProjectDir, "settings.gradle"), """
			pluginManagement {
				includeBuild '../../..'
			}

			rootProject.name = 'basicBuildTest'
			""");
		writeString(new File(testProjectDir, "build.gradle"), """
			plugins {
				id 'com.bbn.parliament.ontology_bundle.OntologyBundle'
			}

			group = 'com.bbn.parliament.test'
			version = '0.1.0'

			ontologyBundle {
				prefixes = [
					// prefix, class, namespace
					'bfo, BFO, http://purl.obolibrary.org/obo/bfo.owl#',
					'dc, DC, http://purl.org/dc/elements/1.1/',
					'dct, DCT, http://purl.org/dc/terms/',
					'dul, DUL, http://www.ontologydesignpatterns.org/ont/dul/DUL.owl#',
					'foaf, FOAF, http://xmlns.com/foaf/0.1/',
					'geo, GEO, http://www.opengis.net/ont/geosparql#',
					'gml, GML, http://www.opengis.net/ont/gml#',
					'obo, OBO, http://purl.obolibrary.org/obo/',
					'owl, , http://www.w3.org/2002/07/owl#',
					'rdf, , http://www.w3.org/1999/02/22-rdf-syntax-ns#',
					'rdfs, , http://www.w3.org/2000/01/rdf-schema#',
					'schema, Schema, http://schema.org/',
					'sf, SF, http://www.opengis.net/ont/sf#',
					'skos, SKOS, http://www.w3.org/2004/02/skos/core#',
					'sosa, SOSA, http://www.w3.org/ns/sosa/',
					'ssn, SSN, http://www.w3.org/ns/ssn/',
					'time, Time, http://www.w3.org/2006/time#',
					'vann, VANN, http://purl.org/vocab/vann/',
					'voaf, VOAF, http://purl.org/vocommons/voaf#',
					'xsd, , http://www.w3.org/2001/XMLSchema#',
				]
				ontologySources = fileTree(dir: "$projectDir/../../../test-ontology",
					includes: [ '**/*.ttl', '**/*.rdf', '**/*.owl' ],
					exclude: '**/*-original*'
				)
				ontologyUri = 'http://bbn.com/ix/ontology-bundle/functional-test'
				ontologyVersion = project.version
				generatedCodePackageName = 'com.bbn.ix.ontology_bundle.functional_test'
				//generatedCodeFileHeader = ''
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
		assertTrue(result.getOutput().contains("Hello from plugin 'com.bbn.parliament.ontology_bundle.OntologyBundle'"));
	}

	private static void writeString(File file, String string) throws IOException {
		try (Writer writer = new FileWriter(file, StandardCharsets.UTF_8)) {
			writer.write(string);
		}
	}

	private static File getTestDir() {
		var ste = new Exception().getStackTrace()[1];
		return new File("build/%1$s/%2$s".formatted(
			OntologyBundlePluginFunctionalTest.class.getSimpleName(), ste.getMethodName()));
	}
}
