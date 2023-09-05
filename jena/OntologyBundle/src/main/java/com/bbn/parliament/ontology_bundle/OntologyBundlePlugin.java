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

/**
 * Ontology Bundle is a Gradle plugin that packages (bundles) an ontology,
 * possibly spanning many files, into a form that makes it easy to use in a
 * Java-based software project. More specifically, a Gradle project that uses
 * this plugin takes as input a collection of RDF files (in any RDF
 * serialization format) containing an ontology, together with some
 * configuration parameters specified in the Gradle build script, and produces a
 * Java jar containing the following outputs:
 * <ul>
 * <li>A single Java resource containing the union of all the ontology input
 * files. This is called the "human-readable ontology file."
 * <li>A second Java resource containing a processed version of the
 * human-readable ontology file, called the "machine-readable ontology file."
 * (See below for details.)
 * <li>For each RDF prefix, Java class file that contains static constants for
 * the terms in the associated vocabulary. For example, if your ontology defines
 * a class foo:MyClass, your Java code can refer to that class as Foo.MyClass
 * with no possibility of a misspelling of the class IRI. (See below for
 * details.)
 * <li>A small Java class, called OntUtil, with convenience methods for loading
 * either ontology resource. The resources may be accessed either as an
 * InputStream or as an in-memory Jena Model. (This Model is non-inferencing,
 * but the caller can easily wrap it in an inferencing Model if they wish.)
 * <li>Tests that verify the merged ontology files.
 * </ul>
 * <h1>Preparation of the Human- and Machine-Readable Ontologies
 * <p>
 * The human-readable ontology file is the result of merging the various input
 * ontology files together, with a few changes to make things tidier:
 * <ul>
 * <li>The individual ontology declarations from the constituent ontologies are
 * replaced with a single declaration representing the combined ontology.
 * <li>White space is stripped from the start and end of string literals, and
 * statements with empty literals are deleted.
 * <li>Explicit declarations that a class is a subclass of owl:Thing are
 * deleted.
 * <li>Property restrictions that are not connected to another class or that are
 * missing their value or cardinality constraint are deleted.
 * <li>The human-readable ontology file is serialized with the most friendly
 * formatting options to make it as readable as possible.
 * </ul>
 * <p>
 * The machine-readable ontology file has two additional changes applied to
 * prepare it to be used in a running software system.
 * <p>
 * First, values for a variety of annotation properties whose purpose is
 * documentation are deleted, since these are rarely used at run time. Note that
 * the declarations of these properties are not removed — only statements using
 * these properties to document the ontology elements are deleted. Also,
 * rdfs:label, skos:altLabel, and skos:prefLabel values are preserved, since
 * human-readable names for ontology elements are useful at run-time.
 * <p>
 * Second, and most importantly, the Ontology Bundle plugin attempts to replace
 * all blank nodes (such as property restrictions and RDF list nodes) with IRI
 * nodes. The goal is to ensure that if the ontology is inserted into a semantic
 * graph store multiple times (perhaps with minor changes), then blank nodes are
 * not duplicated. For instance, if an ontology containing a property
 * restriction blank node is inserted twice, then there will be two property
 * restriction in the database, which slows down inference. If an ontology is
 * re-inserted again and again over a period of time, this load on the inference
 * engine can grow prohibitively large.
 * <p>
 * The key to replacing the blank nodes with IRIs is to ensure that upon each
 * replacement, the same IRI is used. To do this, Ontology Bundle forms the IRI
 * from the cryptographic hash of the identifying property values of the
 * original blank node. For instance, for a property restriction the value of
 * the owl:onProperty statement, the value or cardinality constraint, and the
 * predicate used in the constraint are combined and hashed, and then a
 * namespace is prepended to form a new IRI that replaces the blank node. Two
 * additional notes about this replacement:
 * <ul>
 * <li>The RDF prefix "fill:" is added to the machine-readable ontology with the
 * namespace used for the IRIs that replace the blank nodes.
 * <li>The SPARQL Update statements used to replace the blank nodes are a bit
 * touchy in the sense that it is not difficult to create an ontology for which
 * they will not work. If after running Ontology Bundle on your ontology blank
 * nodes remain, please file a bug report <i>that includes your ontology or a
 * snippet of it that reproduces the problem.</i>
 * </ul>
 * <h1>Generation of the Namespace Classes
 * <p>
 * The Ontology Bundle plugin uses Jena's SchemaGen tool
 * ({@link https://jena.apache.org/documentation/tools/schemagen.html}) to
 * create a Java namespace class for each declared namespace whose class name is
 * not blank in the prefixes declaration in the Gradle build script. This tool
 * creates a Java class containing every declared OWL class, property, datatype,
 * and instance in the associated namespace. These classes can be used to
 * eliminate a particularly pernicious type of coding error, namely misspellings
 * of IRIs in the Java code. Furthermore, if you use these classes everywhere in
 * place of hard-coded IRIs and then change an IRI, the Java compiler will show
 * you exactly where you need to make corresponding changes in the code.
 */
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
			var jupDep = depFact.create(ext.getJunitJupiterDependency().get());
			configs.getByName("testImplementation").getDependencies().add(jupDep);
		});
	}
}
