// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2023, BBN Technologies, Inc.
// All rights reserved.

package org.semwebcentral.parliament.ontology_bundle;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import org.apache.jena.riot.RDFLanguages;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;

import com.bbn.parliament.util.JavaResource;

class NamespaceClassGenerator extends DefaultTask {
	private static final String SCHEMAGEN_RSRC_NAME = "schemagenConfig.ttl";

	private ListProperty<String> prefixes;
	private RegularFileProperty schemagenConfig;
	private RegularFileProperty ontFile;
	private DirectoryProperty outputDir;
	private Property<String> generatedCodePackageName;

	public NamespaceClassGenerator() {
		var objFact = getProject().getObjects();
		var ext = OntologyBundleExtension.getExtension(getProject());
		prefixes = objFact.listProperty(String.class);
		prefixes.set(ext.getPrefixes());
		schemagenConfig = objFact.fileProperty();
		schemagenConfig.set(ext.getSchemagenConfigFile());
		ontFile = objFact.fileProperty();
		ontFile.fileProvider(ext.getOntologyForHumansFile());
		outputDir = objFact.directoryProperty();
		outputDir.fileProvider(ext.getGeneratedJavaDir());
		generatedCodePackageName = objFact.property(String.class);
		generatedCodePackageName.set(ext.getGeneratedCodePackageName());
	}

	@Input
	public ListProperty<String> getPrefixes() {
		return prefixes;
	}

	@InputFile
	@Optional
	public RegularFileProperty getSchemagenConfigFile() {
		return schemagenConfig;
	}

	@InputFile
	public RegularFileProperty getOntologyFile() {
		return ontFile;
	}

	@OutputDirectory
	public DirectoryProperty getOutputDirectory() {
		return outputDir;
	}

	@Input
	public Property<String> getGeneratedCodePackageName() {
		return generatedCodePackageName;
	}

	@TaskAction
	public void run() {
		try {
			var prefixLoader = new PrefixFileLoader(prefixes.get());
			var clsNameToNamespaceMap = prefixLoader.getClsNameToNamespaceMap();
			var configPath = schemagenConfig.isPresent()
				? schemagenConfig.get().getAsFile()
				: copyConfigRsrcToTempFile();
			var inputFile = ontFile.get().getAsFile();
			var lang = RDFLanguages.filenameToLang(inputFile.getName());
			for (var mapEntry : clsNameToNamespaceMap.entrySet()) {
				var clsName = mapEntry.getKey();
				var ns = mapEntry.getValue();
				System.out.format("Generating %1$s.java%n", clsName);
				var args = new ArrayList<String>();
				args.add("-c");
				args.add(configPath.getPath());
				args.add("-i");
				args.add(inputFile.getPath());
				args.add("-a");
				args.add(ns);
				args.add("-n");
				args.add(clsName);
				args.add("-e");
				args.add(lang.getName());
				args.add("--package");
				args.add(generatedCodePackageName.get());
				args.add("-o");
				args.add(outputDir.get().getAsFile().getPath());
				jena.schemagen.main(args.toArray(new String[0]));
			}
		} catch (IOException ex) {
			throw new TaskExecutionException(this, ex);
		}
	}

	private File copyConfigRsrcToTempFile() throws IOException {
		var buildDir = getProject().getLayout().getBuildDirectory();
		var tmpDir = new File(buildDir, "tmp");
		var tmpFile = new File(tmpDir, SCHEMAGEN_RSRC_NAME);
		tmpDir.mkdirs();
		try (
			InputStream is = JavaResource.getAsStream(SCHEMAGEN_RSRC_NAME);
			OutputStream os = new FileOutputStream(tmpFile);
		) {
			is.transferTo(os);
		}
		return tmpFile;
	}
}
