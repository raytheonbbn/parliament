// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2023, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.ontology_bundle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.testing.Test;

public class OntologyBundlePlugin implements Plugin<Project> {
	public static final String EXT_NAME = "ontologyBundle";
	public static final String PREP_ONT_TASK = "prepareOntology";
	public static final String NS_CLASSES_TASK = "generateNamespaceClasses";
	public static final String UTIL_CLASSES_TASK = "generateUtilityClasses";
	private static final String MIN_CLASS_COUNT_SYS_PROP = "minClassCount";
	private static final String MIN_PROP_COUNT_SYS_PROP = "minPropCount";
	private static final String MAX_BNODE_COUNT_SYS_PROP = "maxBlankNodeCount";

	@Override
	public void apply(Project project) {
		// Add our extension:
		var ext = project.getExtensions().create(EXT_NAME, OntologyBundleExtension.class);
		ext.setConventions(project);

		if (!project.getPlugins().hasPlugin("java")
				&& !project.getPlugins().hasPlugin("java-library")
				&& !project.getPlugins().hasPlugin("application")) {
			project.getPlugins().apply("java-library");
		}

		// add generated source and resource directories to the main sourceSet:
		project.getPlugins().withType(JavaPlugin.class, javaPlugin -> {
			var javaExtension = project.getExtensions().findByType(JavaPluginExtension.class);

			var main = javaExtension.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
			main.getJava().srcDir(ext.getGeneratedJavaDir().get());
			main.getResources().srcDir(ext.getGeneratedRsrcDir().get());

			var test = javaExtension.getSourceSets().getByName(SourceSet.TEST_SOURCE_SET_NAME);
			test.getJava().srcDir(ext.getGeneratedTestDir().get());
		});

		// Add our tasks:
		var tasks = project.getTasks();
		var prepOntTask = tasks.register(PREP_ONT_TASK, PrepareOntologyTask.class);
		var genNsTask = tasks.register(NS_CLASSES_TASK, NamespaceClassGenerator.class);
		var genUtilTask = tasks.register(UTIL_CLASSES_TASK, UtilityCodeGenerator.class);

		// Wire our two tasks into the standard Java dependency graph:
		genNsTask.get().dependsOn(prepOntTask);
		genUtilTask.get().dependsOn(genNsTask);
		tasks.getByName("compileJava").dependsOn(genUtilTask);
		tasks.getByName("processResources").dependsOn(genUtilTask);

		// Configure the jar task:
		tasks.withType(Jar.class, task -> {
			task.getArchiveBaseName().set(project.getName());
			task.getManifest().getAttributes().put("Implementation-Title", project.getName());
			task.getManifest().getAttributes().put("Implementation-Version", project.getVersion());
			task.doLast(s -> System.out.format(
				"Archive name:  '%1$s'%n", task.getArchiveFileName().get()));
		});

		// Configure the test task:
		tasks.withType(Test.class, task -> {
			task.useJUnitPlatform();
			task.systemProperty(MIN_CLASS_COUNT_SYS_PROP, "1");
			task.systemProperty(MIN_PROP_COUNT_SYS_PROP, "1");
			task.systemProperty(MAX_BNODE_COUNT_SYS_PROP, "0");
		});

		project.afterEvaluate(proj -> {
			proj.getRepositories().add(proj.getRepositories().mavenCentral());

			var depFact = proj.getDependencyFactory();
			var configs = proj.getConfigurations();
			var jenaDep = depFact.create(ext.getJenaDependency().get());
			configs.getByName("api").getDependencies().add(jenaDep);
			var jupDep = depFact.create(ext.getJupiterDependency().get());
			configs.getByName("testImplementation").getDependencies().add(jupDep);
		});
	}
}
