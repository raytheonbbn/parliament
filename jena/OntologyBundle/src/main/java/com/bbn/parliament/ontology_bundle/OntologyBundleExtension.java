package com.bbn.parliament.ontology_bundle;

import java.io.File;

import javax.inject.Inject;

import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

/**
 * Plugin configuration parameters, set through the build script.
 *
 * @author iemmons
 */
public class OntologyBundleExtension {
	private final Property<String> humanReadableOntologyFileName;
	private final Property<String> machineReadableOntologyFileName;
	private final RegularFileProperty prefixFile;
	private final RegularFileProperty schemagenConfigFile;
	private final DirectoryProperty reportsDir;
	private final ConfigurableFileCollection ontologySources;
	private final Property<String> ontologyUri;
	private final Property<String> ontologyVersion;
	private final Property<String> generatedCodePackageName;
	private final Property<String> generatedCodeFileHeader;

	@Inject
	public OntologyBundleExtension(ObjectFactory objectFactory) {
		humanReadableOntologyFileName = objectFactory.property(String.class);
		machineReadableOntologyFileName = objectFactory.property(String.class);
		prefixFile = objectFactory.fileProperty();
		schemagenConfigFile = objectFactory.fileProperty();
		reportsDir = objectFactory.directoryProperty();
		ontologySources = objectFactory.fileCollection();
		ontologyUri = objectFactory.property(String.class);
		ontologyVersion = objectFactory.property(String.class);
		generatedCodePackageName = objectFactory.property(String.class);
		generatedCodeFileHeader = objectFactory.property(String.class);
	}

	public void setConventions(Project project) {
		humanReadableOntologyFileName.convention("Ontology-HumanReadable.ttl");
		machineReadableOntologyFileName.convention("Ontology-MachineReadable.ttl");
		reportsDir.convention(project.getLayout().getBuildDirectory().dir("reports/ontologyBundle"));
	}

	public File getPrefixFile() {
		return prefixFile.get().getAsFile();
	}

	public File getSchemagenConfigFile() {
		return schemagenConfigFile.get().getAsFile();
	}

	public File getReportsDir() {
		Directory result = reportsDir.getOrNull();
		return (result == null) ? null : result.getAsFile();
	}
}
