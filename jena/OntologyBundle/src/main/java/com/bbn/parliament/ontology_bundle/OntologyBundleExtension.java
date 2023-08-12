// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2023, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.ontology_bundle;

import java.io.File;

import javax.inject.Inject;

import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;

/**
 * Plugin configuration parameters, set through the build script.
 */
public class OntologyBundleExtension {
	public static final String HUMAN_ONT_DEFAULT_FILE = "OntologyForHumans.ttl";
	public static final String MACHINE_ONT_DEFAULT_FILE = "OntologyForMachines.ttl";

	private final Property<String> ontologyForHumansFileName;
	private final Property<String> ontologyForMachinesFileName;
	private final ListProperty<String> prefixes;
	private final RegularFileProperty schemagenConfigFile;
	private final DirectoryProperty reportDir;
	private final ConfigurableFileCollection ontologySources;
	private final Property<String> ontologyUri;
	private final Property<String> ontologyVersion;
	private final Property<String> generatedCodePackageName;
	private final DirectoryProperty generatedJavaDir;
	private final DirectoryProperty generatedRsrcDir;
	private final DirectoryProperty generatedTestDir;
	private final Property<String> jenaDependency;
	private final Property<String> jupiterDependency;

	public static OntologyBundleExtension getExtension(Project project) {
		return project
			.getExtensions()
			.getByType(OntologyBundleExtension.class);
	}

	@Inject
	public OntologyBundleExtension(ObjectFactory objFact) {
		ontologyForHumansFileName = objFact.property(String.class);
		ontologyForMachinesFileName = objFact.property(String.class);
		prefixes = objFact.listProperty(String.class);
		schemagenConfigFile = objFact.fileProperty();
		reportDir = objFact.directoryProperty();
		ontologySources = objFact.fileCollection();
		ontologyUri = objFact.property(String.class);
		ontologyVersion = objFact.property(String.class);
		generatedCodePackageName = objFact.property(String.class);
		generatedJavaDir = objFact.directoryProperty();
		generatedRsrcDir = objFact.directoryProperty();
		generatedTestDir = objFact.directoryProperty();
		jenaDependency = objFact.property(String.class);
		jupiterDependency = objFact.property(String.class);
	}

	public void setConventions(Project project) {
		DirectoryProperty buildDir = project.getLayout().getBuildDirectory();

		ontologyForHumansFileName.convention(HUMAN_ONT_DEFAULT_FILE);
		ontologyForMachinesFileName.convention(MACHINE_ONT_DEFAULT_FILE);
		reportDir.convention(buildDir.dir("reports/ontologyBundle"));
		generatedJavaDir.convention(buildDir.dir("generated/main/java"));
		generatedRsrcDir.convention(buildDir.dir("generated/main/resources"));
		generatedTestDir.convention(buildDir.dir("generated/test/java"));
		jenaDependency.convention("org.apache.jena:jena-arq:3.17.0");
		jupiterDependency.convention("org.junit.jupiter:junit-jupiter:5.9.2");
	}

	public Property<String> getOntologyForHumansFileName() {
		return ontologyForHumansFileName;
	}

	public Property<String> getOntologyForMachinesFileName() {
		return ontologyForMachinesFileName;
	}

	public ListProperty<String> getPrefixes() {
		return prefixes;
	}

	public RegularFileProperty getSchemagenConfigFile() {
		return schemagenConfigFile;
	}

	public DirectoryProperty getReportDir() {
		return reportDir;
	}

	public ConfigurableFileCollection getOntologySources() {
		return ontologySources;
	}

	public Property<String> getOntologyUri() {
		return ontologyUri;
	}

	public Property<String> getOntologyVersion() {
		return ontologyVersion;
	}

	public Property<String> getGeneratedCodePackageName() {
		return generatedCodePackageName;
	}

	public Provider<File> getGeneratedJavaDir() {
		return generatedJavaDir.getAsFile();
	}

	public Provider<File> getGeneratedRsrcDir() {
		return generatedRsrcDir.getAsFile();
	}

	public Provider<File> getGeneratedTestDir() {
		return generatedTestDir.getAsFile();
	}

	public Provider<File> getOntologyForHumansFile() {
		return ontologyForHumansFileName.map(
			fname -> FileUtil.getCodeFile(generatedRsrcDir.get().getAsFile(),
				generatedCodePackageName.get(), fname));
	}

	public Provider<File> getOntologyForMachinesFile() {
		return ontologyForMachinesFileName.map(
			fname -> FileUtil.getCodeFile(generatedRsrcDir.get().getAsFile(),
				generatedCodePackageName.get(), fname));
	}

	public Provider<String> getJenaDependency() {
		return jenaDependency;
	}

	public Provider<String> getJupiterDependency() {
		return jupiterDependency;
	}
}
