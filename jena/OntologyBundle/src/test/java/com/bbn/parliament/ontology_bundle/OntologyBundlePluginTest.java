package com.bbn.parliament.ontology_bundle;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

/**
 * A simple unit test for the 'com.bbn.parliament.ontology_bundle.OntologyBundle' plugin.
 */
public class OntologyBundlePluginTest {
	@SuppressWarnings("static-method")
	@Test
	public void pluginRegistersATask() {
		// Create a test project and apply the plugin
		Project project = ProjectBuilder.builder()
			.withName("ont-bundle-test")
			.build();
		project.getPlugins().apply("com.bbn.parliament.ontology_bundle.ontologyBundle");

		// Verify the result
		assertNotNull(project.getTasks().findByName(OntologyBundlePlugin.PREP_ONT_TASK));
		assertNotNull(project.getTasks().findByName(OntologyBundlePlugin.NS_CLASSES_TASK));
	}
}
