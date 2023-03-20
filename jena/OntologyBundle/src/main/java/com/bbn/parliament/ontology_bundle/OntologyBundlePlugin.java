package com.bbn.parliament.ontology_bundle;

import java.io.File;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;

public class OntologyBundlePlugin implements Plugin<Project> {
	@Override
	public void apply(Project project) {
		// Add our extension:
		project.getExtensions().create("ontologyBundle", OntologyBundleExtension.class);
		OntologyBundleExtension ext = (OntologyBundleExtension)
			project.getExtensions().getByName("ontologyBundle");
		ext.setConventions(project);

		// add generated source and resource directories to the main sourceSet:
		File genJavaDir = new File(project.getBuildDir(), "generated/java");
		File genRsrcDir = new File(project.getBuildDir(), "generated/resources");
		project.getPlugins().withType(JavaPlugin.class, javaPlugin -> {
			JavaPluginConvention javaConvention = project.getConvention().getPlugin(JavaPluginConvention.class);
			SourceSet main = javaConvention.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
			main.getJava().srcDir(genJavaDir);
			main.getResources().srcDir(genRsrcDir);
		});

		// Add our tasks:
		project.getTasks().register("bundleOntology", OntologyBundlerTask.class, task -> {
			task.doLast(s -> System.out.println("Hello from plugin " + getClass().getName()));
		});
		project.getTasks().register("generateNamespaceClasses", NamespaceClassGenerator.class, task -> {
			task.dependsOn(project.getTasks().getByName("bundleOntology"));
			task.doLast(s -> System.out.println("Hello from plugin " + getClass().getName()));
		});

		// make compileJava and processResources depend on generateNamespaceClasses:
		project.getTasks().getByName("compileJava").dependsOn(
			project.getTasks().getByName("generateNamespaceClasses"));
		project.getTasks().getByName("processResources").dependsOn(
			project.getTasks().getByName("generateNamespaceClasses"));

		// Configure the jar task:






	}
}
