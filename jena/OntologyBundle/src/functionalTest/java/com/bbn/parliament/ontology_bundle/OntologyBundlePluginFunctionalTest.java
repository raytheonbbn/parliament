package com.bbn.parliament.ontology_bundle;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;

/**
 * A simple functional test for the 'com.bbn.parliament.ontology_bundle.OntologyBundle' plugin.
 */
public class OntologyBundlePluginFunctionalTest {
	@Test
	public void canRunTask() throws IOException {
		// Setup the test build
		File projectDir = new File("build/functionalTest");
		Files.createDirectories(projectDir.toPath());
		writeString(new File(projectDir, "settings.gradle"), "");
		writeString(new File(projectDir, "build.gradle"),
			"plugins { id('com.bbn.parliament.ontology_bundle.OntologyBundle') }");

		// Run the build
		BuildResult result = GradleRunner.create()
			.forwardOutput()
			.withPluginClasspath()
			.withArguments("greeting")
			.withProjectDir(projectDir)
			.build();

		// Verify the result
		assertTrue(result.getOutput().contains("Hello from plugin 'com.bbn.parliament.ontology_bundle.OntologyBundle'"));
	}

	@SuppressWarnings("static-method")
	private void writeString(File file, String string) throws IOException {
		try (Writer writer = new FileWriter(file)) {
			writer.write(string);
		}
	}
}
