package com.bbn.parliament.ontology_bundle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.bundling.Jar;

public class OntologyBundlePlugin implements Plugin<Project> {
	public static final String EXT_NAME = "ontologyBundle";
	public static final String PREP_ONT_TASK = "prepareOntology";
	public static final String NS_CLASSES_TASK = "generateNamespaceClasses";

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
		});

		// Add our tasks:
		project.getTasks().register(PREP_ONT_TASK, PrepareOntologyTask.class);
		project.getTasks().register(NS_CLASSES_TASK, NamespaceClassGenerator.class, task -> {
			task.dependsOn(project.getTasks().getByName(PREP_ONT_TASK));
		});

		// Make compileJava and processResources depend on generateNamespaceClasses:
		var genNsTask = project.getTasks().getByName(NS_CLASSES_TASK);
		project.getTasks().getByName("compileJava").dependsOn(genNsTask);
		project.getTasks().getByName("processResources").dependsOn(genNsTask);

		// Configure the jar task:
		var jarTask = (Jar) project.getTasks().getByName("jar");
		jarTask.getArchiveBaseName().set(project.getName());
		jarTask.getManifest().getAttributes().put("Implementation-Title", project.getName());
		jarTask.getManifest().getAttributes().put("Implementation-Version", project.getVersion());
		jarTask.doLast(s -> System.out.format(
			"Archive name:  '%1$s'%n", jarTask.getArchiveFileName().get()));
	}
}
