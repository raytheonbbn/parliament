// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2023, BBN Technologies, Inc.
// All rights reserved.

package org.semwebcentral.parliament.ontology_bundle;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.semwebcentral.parliament.util.JavaResource;

class UtilityCodeGenerator extends DefaultTask {
	private static final String ONT_UTIL_RSRC = "OntAccess.java.txt";
	private static final String ONT_UTIL_TEST_RSRC = "OntAccessTest.java.txt";

	private final Property<String> generatedCodePackageName;
	private final Property<String> ontologyForHumansFileName;
	private final Property<String> ontologyForMachinesFileName;
	private final DirectoryProperty generatedJavaDir;
	private final DirectoryProperty generatedTestDir;

	@Input
	public Property<String> getGeneratedCodePackageName() {
		return generatedCodePackageName;
	}

	@Input
	public Property<String> getOntologyForHumansFileName() {
		return ontologyForHumansFileName;
	}

	@Input
	public Property<String> getOntologyForMachinesFileName() {
		return ontologyForMachinesFileName;
	}

	@OutputDirectory
	public DirectoryProperty getGeneratedJavaDir() {
		return generatedJavaDir;
	}

	@OutputDirectory
	public DirectoryProperty getGeneratedTestDir() {
		return generatedTestDir;
	}

	public UtilityCodeGenerator() {
		var objFact = getProject().getObjects();
		var ext = OntologyBundleExtension.getExtension(getProject());
		generatedCodePackageName = objFact.property(String.class);
		generatedCodePackageName.set(ext.getGeneratedCodePackageName());
		ontologyForHumansFileName = objFact.property(String.class);
		ontologyForHumansFileName.set(ext.getOntologyForHumansFileName());
		ontologyForMachinesFileName = objFact.property(String.class);
		ontologyForMachinesFileName.set(ext.getOntologyForMachinesFileName());
		generatedJavaDir = objFact.directoryProperty();
		generatedJavaDir.fileProvider(ext.getGeneratedJavaDir());
		generatedTestDir = objFact.directoryProperty();
		generatedTestDir.fileProvider(ext.getGeneratedTestDir());
	}

	@TaskAction
	public void run() {
		var ontAccessCode = JavaResource.getAsString(ONT_UTIL_RSRC);
		var genPackage = generatedCodePackageName.get();
		ontAccessCode = replace(ontAccessCode, getClass().getPackageName(), genPackage);
		ontAccessCode = replace(ontAccessCode, OntologyBundleExtension.HUMAN_ONT_DEFAULT_FILE,
			ontologyForHumansFileName.get());
		ontAccessCode = replace(ontAccessCode, OntologyBundleExtension.MACHINE_ONT_DEFAULT_FILE,
			ontologyForMachinesFileName.get());
		writeRsrcAsCodeFile(ontAccessCode, ONT_UTIL_RSRC, genPackage,
			generatedJavaDir);

		var ontAccessTestCode = JavaResource.getAsString(ONT_UTIL_TEST_RSRC);
		ontAccessTestCode = replace(ontAccessTestCode, getClass().getPackageName(), genPackage);
		writeRsrcAsCodeFile(ontAccessTestCode, ONT_UTIL_TEST_RSRC, genPackage,
			generatedTestDir);
	}

	private static String replace(String initialString, String toReplace, String replacement) {
		return initialString.replaceAll(
			Pattern.quote(toReplace),
			Matcher.quoteReplacement(replacement));
	}

	private static void writeRsrcAsCodeFile(String code, String rsrcName,
			String packageName, Provider<Directory> srcDir) {
		var fileName = FileUtil.getFileNameStem(rsrcName);
		var file = FileUtil.getCodeFile(srcDir.get().getAsFile(), packageName, fileName);
		FileUtil.writeString(file, code);
	}
}
