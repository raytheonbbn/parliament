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
 * Configuration parameters for the Ontology Bundle plugin.
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
	private final Property<String> ontologyIri;
	private final Property<String> ontologyVersion;
	private final Property<String> generatedCodePackageName;
	private final DirectoryProperty generatedJavaDir;
	private final DirectoryProperty generatedRsrcDir;
	private final DirectoryProperty generatedTestDir;
	private final Property<String> jenaVersion;
	private final Property<String> junitJupiterVersion;

	/**
	 * A utility method to retrieve the Ontology Bundle extension from the project.
	 *
	 * @param project The Gradle project from which to retrieve the extension
	 * @return The extension object
	 */
	public static OntologyBundleExtension getExtension(Project project) {
		return project
			.getExtensions()
			.getByType(OntologyBundleExtension.class);
	}

	/**
	 * Initializes an Ontology Bundle extension object and sets conventions.
	 *
	 * @param project The Gradle project, typically injected by Gradle
	 * @param objFact The object factory, typically injected by Gradle
	 */
	@Inject
	public OntologyBundleExtension(Project project, ObjectFactory objFact) {
		var buildDir = project.getLayout().getBuildDirectory();

		ontologyForHumansFileName = objFact.property(String.class)
			.convention(HUMAN_ONT_DEFAULT_FILE);
		ontologyForMachinesFileName = objFact.property(String.class)
			.convention(MACHINE_ONT_DEFAULT_FILE);
		prefixes = objFact.listProperty(String.class);
		schemagenConfigFile = objFact.fileProperty();
		reportDir = objFact.directoryProperty()
			.convention(buildDir.dir("reports/ontologyBundle"));
		ontologySources = objFact.fileCollection();
		ontologyIri = objFact.property(String.class);
		ontologyVersion = objFact.property(String.class);
		generatedCodePackageName = objFact.property(String.class);
		generatedJavaDir = objFact.directoryProperty()
			.convention(buildDir.dir("generated/main/java"));
		generatedRsrcDir = objFact.directoryProperty()
			.convention(buildDir.dir("generated/main/resources"));
		generatedTestDir = objFact.directoryProperty()
			.convention(buildDir.dir("generated/test/java"));
		jenaVersion = objFact.property(String.class)
			.convention("3.17.0");
		junitJupiterVersion = objFact.property(String.class)
			.convention("5.9.2");
	}

	/**
	 * The file name used for the merged, human-readable ontology file. The file
	 * extension determines the RDF serialization that will be used. The default
	 * value is "OntologyForHumans.ttl".
	 *
	 * @return Human-readable ontology file name
	 */
	public Property<String> getOntologyForHumansFileName() {
		return ontologyForHumansFileName;
	}

	/**
	 * The file name used for the merged, machine-readable ontology file. The file
	 * extension determines the RDF serialization that will be used. The default
	 * value is "OntologyForMachines.ttl".
	 *
	 * @return Human-readable ontology file name
	 */
	public Property<String> getOntologyForMachinesFileName() {
		return ontologyForMachinesFileName;
	}

	/**
	 * The list of RDF prefixes to use for the merged ontology. A prefix must be
	 * present in this list for every namespace used in the input ontology files,
	 * although the prefix assigned here to each namespace may be different than the
	 * one that appears in the original ontology file.
	 * <p>
	 * This property is a list of strings, each of the form
	 *
	 * <pre>{@code
	 *    prefix, class-name, namespace-iri
	 * }</pre>
	 *
	 * where:
	 * <ul>
	 * <li>"prefix" is the RDF prefix that will be used in the combined ontology
	 * files.
	 * <li>"class-name" is the name given to the generated Java namespace class. If
	 * this is empty, the class will not be generated. (Note that in this case the
	 * comma must still be present.) This entry may seem redundant at first (why not
	 * just use the prefix itself?), but it gives control over the capitalization of
	 * the class name and allows for prefixes with characters that are illegal in
	 * Java identifiers.
	 * <li>"namespace-iri" is the IRI of the namespace.
	 * </ul>
	 *
	 * The prefixes, class names, and namespace IRIs given in this list must be
	 * unique. In other words, no prefix, class name, or namespace IRI may be
	 * repeated in this list.
	 *
	 * @return The list of RDF prefixes to be used in the merged ontology
	 */
	public ListProperty<String> getPrefixes() {
		return prefixes;
	}

	/**
	 * If the configuration parameters in this class do not provide enough control
	 * over SchemaGen's generation of the namespace classes, then you may provide
	 * your own SchemaGen configuration file. See
	 * {@link https://jena.apache.org/documentation/tools/schemagen.html} for
	 * details.
	 *
	 * @return The path to a custom SchemaGen configuration file to use instead of
	 *         Ontology Bundle's internal default.
	 */
	public RegularFileProperty getSchemagenConfigFile() {
		return schemagenConfigFile;
	}

	/**
	 * The directory into which Ontology Bundle will generate reports. By default,
	 * this is set to {@code build/reports/ontologyBundle} relative to the project
	 * directory.
	 *
	 * @return The directory used for reporting
	 */
	public DirectoryProperty getReportDir() {
		return reportDir;
	}

	/**
	 * The set of RDF files containing the ontology to be bundled. Gradle provides
	 * powerful facilities for specifying this conveniently. For instance:
	 *
	 * <pre>{@code
	 *    ontologyBundle &lbrace;
	 *       ...
	 *
	 *       ontologySources = fileTree(dir: "$projectDir/../../../../test-ontology",
	 *          includes: [ '&ast;&ast;/&ast;.ttl', '&ast;&ast;/&ast;.owl' ],
	 *          exclude: '&ast;&ast;/&ast;-experimental&ast;'
	 *       )
	 *
	 *       ...
	 *    &rbrace;
	 * }</pre>
	 *
	 * @return The collection of RDF files to be bundled
	 */
	public ConfigurableFileCollection getOntologySources() {
		return ontologySources;
	}

	/**
	 * The IRI used to identify the bundled ontology as a whole.
	 *
	 * @return The ontology IRI for the bundled ontology
	 */
	public Property<String> getOntologyIri() {
		return ontologyIri;
	}

	/**
	 * The version to be given to the bundled ontology
	 *
	 * @return The version of the bundled ontology
	 */
	public Property<String> getOntologyVersion() {
		return ontologyVersion;
	}

	/**
	 * The Java package name used for the generated Java code. This includes both
	 * the namespaces classes and the {@code OntUtil} class.
	 *
	 * @return The Java package name for the generated Java code
	 */
	public Property<String> getGeneratedCodePackageName() {
		return generatedCodePackageName;
	}

	/**
	 * A read-only property specifying the directory for generated Java code. This
	 * is set to {@code build/generated/main/java}.
	 *
	 * @return The directory for generated Java code
	 */
	public Provider<File> getGeneratedJavaDir() {
		return generatedJavaDir.getAsFile();
	}

	/**
	 * A read-only property specifying the directory for generated Java resources.
	 * This is set to {@code build/generated/main/resources}.
	 *
	 * @return The directory for generated Java resources
	 */
	public Provider<File> getGeneratedRsrcDir() {
		return generatedRsrcDir.getAsFile();
	}

	/**
	 * A read-only property specifying the directory for generated Java test code.
	 * This is set to {@code build/generated/test/java}.
	 *
	 * @return The directory for generated Java test code
	 */
	public Provider<File> getGeneratedTestDir() {
		return generatedTestDir.getAsFile();
	}

	/**
	 * A read-only property specifying the path name of the human-readable ontology
	 * file. This is formed from the {@code generatedRsrcDir} and
	 * {@code ontologyForHumansFileName} properties.
	 *
	 * @return The path name of the human-readable ontology file
	 */
	public Provider<File> getOntologyForHumansFile() {
		return ontologyForHumansFileName.map(
			fname -> FileUtil.getCodeFile(generatedRsrcDir.get().getAsFile(),
				generatedCodePackageName.get(), fname));
	}

	/**
	 * A read-only property specifying the path name of the machine-readable
	 * ontology file. This is formed from the {@code generatedRsrcDir} and
	 * {@code ontologyForMachinesFileName} properties.
	 *
	 * @return The path name of the machine-readable ontology file
	 */
	public Provider<File> getOntologyForMachinesFile() {
		return ontologyForMachinesFileName.map(
			fname -> FileUtil.getCodeFile(generatedRsrcDir.get().getAsFile(),
				generatedCodePackageName.get(), fname));
	}

	/**
	 * The version of the Jena libraries that the generated jar will depend upon.
	 * This is set to 3.17.0 by default.
	 *
	 * @return The version of the Jena libraries used
	 */
	public Property<String> getJenaVersion() {
		return jenaVersion;
	}

	/**
	 * The version of the Junit Jupiter libraries that the generated tests will use.
	 * This is set to 5.9.2 by default.
	 *
	 * @return The version of the Junit Jupiter libraries used
	 */
	public Property<String> getJunitJupiterVersion() {
		return junitJupiterVersion;
	}

	/**
	 * A read-only property specifying the complete Gradle dependency specification
	 * for the Jena libraries that the generated jar will depend upon.
	 *
	 * @return The dependency specification of the Jena libraries used
	 */
	public Provider<String> getJenaDependency() {
		return jenaVersion.map(
			version -> "org.apache.jena:jena-arq:%1$s".formatted(version));
	}

	/**
	 * A read-only property specifying the complete Gradle dependency specification
	 * for the Junit Jupiter libraries that the generated tests will use.
	 *
	 * @return The dependency specification of the Junit Jupiter libraries used
	 */
	public Provider<String> getJunitJupiterDependency() {
		return junitJupiterVersion.map(
			version -> "org.junit.jupiter:junit-jupiter:%1$s".formatted(version));
	}
}
