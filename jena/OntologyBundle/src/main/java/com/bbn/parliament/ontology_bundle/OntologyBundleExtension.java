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
 *
 * @author iemmons
 */
public class OntologyBundleExtension {
	private final Property<String> ontologyForHumansFileName;
	private final Property<String> ontologyForMachinesFileName;
	private final ListProperty<String> prefixes;
	private final RegularFileProperty schemagenConfigFile;
	private final DirectoryProperty reportDir;
	private final ConfigurableFileCollection ontologySources;
	private final Property<String> ontologyUri;
	private final Property<String> ontologyVersion;
	private final Property<String> generatedCodePackageName;
	private final Property<String> generatedCodeFileHeader;
	private final DirectoryProperty generatedJavaDir;
	private final DirectoryProperty generatedRsrcDir;

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
		generatedCodeFileHeader = objFact.property(String.class);
		generatedJavaDir = objFact.directoryProperty();
		generatedRsrcDir = objFact.directoryProperty();
	}

	public void setConventions(Project project) {
		DirectoryProperty buildDir = project.getLayout().getBuildDirectory();

		ontologyForHumansFileName.convention("OntologyForHumans.ttl");
		ontologyForMachinesFileName.convention("OntologyForMachines.ttl");
		reportDir.convention(buildDir.dir("reports/ontologyBundle"));
		generatedJavaDir.convention(buildDir.dir("generated/java"));
		generatedRsrcDir.convention(buildDir.dir("generated/resources"));
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

	public Property<String> getGeneratedCodeFileHeader() {
		return generatedCodeFileHeader;
	}

	public Provider<File> getGeneratedJavaDir() {
		return generatedJavaDir.getAsFile();
	}

	public Provider<File> getGeneratedRsrcDir() {
		return generatedRsrcDir.getAsFile();
	}

	public Provider<File> getOntologyForHumansFile() {
		return ontologyForHumansFileName.map(
			fname -> new File(generatedRsrcDir.get().getAsFile(), fname));
	}

	public Provider<File> getOntologyForMachinesFile() {
		return ontologyForMachinesFileName.map(
			fname -> new File(generatedRsrcDir.get().getAsFile(), fname));
	}
}
