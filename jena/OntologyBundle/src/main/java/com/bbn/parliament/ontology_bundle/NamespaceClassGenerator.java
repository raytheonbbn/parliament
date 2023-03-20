package com.bbn.parliament.ontology_bundle;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;
import org.gradle.process.ExecResult;

import com.bbn.parliament.util.JavaResource;

public class NamespaceClassGenerator extends DefaultTask {
	private static final String HEADER_RSRC_NAME = "schemagenHeader.txt";

	private final OntologyBundleExtension ontBundleExt = getProject().getExtensions()
		.getByType(OntologyBundleExtension.class);

	private File ontFile = null;
	private File outputDir = null;

	@InputFile
	public File getOntologyFile() {
		return ontFile;
	}

	public void setOntologyFile(File ontologyFile) {
		ontFile = ontologyFile;
	}

	@OutputDirectory
	public File getOutputDirectory() {
		return outputDir;
	}

	public void setOutputDirectory(File outputDirectory) {
		outputDir = outputDirectory;
	}

	@TaskAction
	public void run() {
		try {
			PrefixFileLoader prefixLoader = new PrefixFileLoader(ontBundleExt.getPrefixFile());
			Map<String, String> clsNameToNamespaceMap = prefixLoader.getClsNameToNamespaceMap();
			String header = JavaResource.getAsString(HEADER_RSRC_NAME);
			for (Map.Entry<String, String> entry : clsNameToNamespaceMap.entrySet()) {
				String clsName = entry.getKey();
				String ns = entry.getValue();
				System.out.format("Generating %1$s.java%n", clsName);
				ExecResult execResult = getProject().javaexec(spec -> {
					spec.setMain("jena.schemagen");
					spec.setClasspath(getProject().getConfigurations().getByName("runtimeClasspath").fileCollection(dep -> true));
					spec.args("-c", ontBundleExt.getSchemagenConfigFile().getPath());
					spec.args("-i", ontFile.getPath());
					spec.args("-a", ns);
					spec.args("-n", clsName);
					spec.args("-o", outputDir);
					spec.args("--header", header);
				});
				execResult.assertNormalExitValue();
			}
		} catch (IOException ex) {
			throw new TaskExecutionException(this, ex);
		}
	}
}
