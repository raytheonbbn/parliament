// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2023, BBN Technologies, Inc.
// All rights reserved.

package org.semwebcentral.parliament.ontology_bundle;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;

import com.bbn.parliament.util.JavaResource;
import com.bbn.parliament.util.QuerySolutionStream;

class PrepareOntologyTask extends DefaultTask {
	private static enum OutputType { FOR_HUMANS, FOR_MACHINES }

	private static final PrefixInfo FILLED_IN_PREFIX = new PrefixInfo("fill", null,
		"http://parliament.semwebcentral.org/filled-in-blank-node#");
	private static final String[] UPDATES_FOR_HUMANS = {
		"deleteOntologyNodes.update",
		"trimStringLiterals.update",
		"deleteEmptyLiterals.update",
		"deleteOwlThingSuperClasses.update",
		"deleteOrphanBlankRestrictions.update",
		"deleteBlankRestrictionsWithNoConstraint.update",
	};
	private static final String[] UPDATES_FOR_MACHINES = {
		"deleteAnnotationProperties.update",
	};
	private static final String[] REPORTS = {
	};
	private static final String[] BLANK_NODE_UPDATES = {
		"replaceBlankXsdRestrictions.update",
		"replaceBlankDatatype.update",
		"replaceBlankShaclDatatype.update",
		"replaceBlankInverseProp.update",
		"replaceBlankUnionDomainAndRange.update",
		"replaceBlankRestrictions.update",
		"replaceBlankAxiomAnnotation.update",
		"replaceBlankGROffering.update",
		"replaceBlankShaclPrefix.update",
		"replaceBlankShaclInversePath.update",
		"replaceBlankPropertyShape.update",
		"replaceBlankSparqlConstraint.update",
		"replaceBlankListNodes.update",
	};
	private static final String COUNT_BLANK_QUERY = "countBlankNodes.sparql";
	private static final String COUNT_CLASS_QUERY = "countClasses.sparql";
	private static final String COUNT_PROP_QUERY = "countProperties.sparql";
	private static final String COUNT_RESTR_QUERY = "countRestrictions.sparql";
	private static final String STATS_FORMAT = """
		The %1$s ontology contains:
			%2$d statements
			%3$d classes
			%4$d properties
			%5$d restrictions
			%6$d blank nodes
		""";

	private FileCollection srcFiles;
	private ListProperty<String> prefixes;
	private RegularFileProperty humanOntFile;
	private RegularFileProperty machineOntFile;
	private DirectoryProperty reportDir;
	private Property<String> ontIri;
	private PrefixFileLoader prefixLoader;

	public PrepareOntologyTask() {
		var objFact = getProject().getObjects();
		var ext = OntologyBundleExtension.getExtension(getProject());
		srcFiles = ext.getOntologySources();
		prefixes = objFact.listProperty(String.class);
		prefixes.set(ext.getPrefixes());

		humanOntFile = objFact.fileProperty();
		humanOntFile.fileProvider(ext.getOntologyForHumansFile());
		machineOntFile = objFact.fileProperty();
		machineOntFile.fileProvider(ext.getOntologyForMachinesFile());

		reportDir = objFact.directoryProperty();
		reportDir.set(ext.getReportDir());
		ontIri = objFact.property(String.class);
		ontIri.set(ext.getOntologyIri());
		prefixLoader = null;
	}

	@SkipWhenEmpty
	@InputFiles
	@PathSensitive(PathSensitivity.NONE)
	public FileCollection getSourceFiles() {
		return srcFiles;
	}

	public void setSourceFiles(FileCollection sourceFiles) {
		srcFiles = sourceFiles;
	}

	public void sources(FileCollection sourceFiles) {
		srcFiles = srcFiles.plus(sourceFiles);
	}

	@Input
	public ListProperty<String> getPrefixes() {
		return prefixes;
	}

	@OutputFile
	public RegularFileProperty getOntFileForHumans() {
		return humanOntFile;
	}

	@OutputFile
	public RegularFileProperty getOntFileForMachines() {
		return machineOntFile;
	}

	@OutputDirectory
	@Optional
	public DirectoryProperty getReportDir() {
		return reportDir;
	}

	@Input
	@Optional
	public Property<String> getOntologyIri() {
		return ontIri;
	}

	@TaskAction
	public void run() {
		try {
			prefixLoader = new PrefixFileLoader(prefixes.get());

			var combinedModel = combineSourceFiles();
			printOntStats(combinedModel, "initial");

			runReports(combinedModel);
			for (var rsrcName : UPDATES_FOR_HUMANS) {
				runUpdate(combinedModel, rsrcName);
			}

			insertOntologyNode(combinedModel);

			printOntStats(combinedModel, "human-readable");
			writeCombinedOntology(combinedModel, OutputType.FOR_HUMANS);

			for (var rsrcName : UPDATES_FOR_MACHINES) {
				runUpdate(combinedModel, rsrcName);
			}

			combinedModel.setNsPrefix(FILLED_IN_PREFIX.prefix(), FILLED_IN_PREFIX.namespace());
			runBlankNodeFillers(combinedModel, BLANK_NODE_UPDATES);

			printOntStats(combinedModel, "machine-readable");
			writeCombinedOntology(combinedModel, OutputType.FOR_MACHINES);
		} catch (IOException ex) {
			throw new TaskExecutionException(this, ex);
		}
	}

	private Model combineSourceFiles() throws IOException {
		var combinedModel = ModelFactory.createDefaultModel();
		prefixLoader.addDeclaredPrefixesTo(combinedModel);
		for (var f : srcFiles.getFiles()) {
			var lang = RDFLanguages.filenameToLang(f.getName());
			if (lang == null) {
				System.out.format("Unrecognized RDF serialization: '%1$s'%n", f.getPath());
				continue;
			}
			System.out.format("Reading %1$s file '%2$s'%n", lang.getName(), f.getPath());
			try (InputStream in = new FileInputStream(f)) {
				var inputModel = ModelFactory.createDefaultModel();
				inputModel.read(in, null, lang.getName());
				prefixLoader.validateInputFilePrefixes(inputModel, f);
				prefixLoader.switchToPreferredPrefixes(inputModel, f);
				combinedModel.add(inputModel);
			}
		}
		return combinedModel;
	}

	private void insertOntologyNode(Model combinedModel) {
		if (ontIri.isPresent()) {
			var ont = combinedModel.createResource(combinedModel.expandPrefix(ontIri.get()));
			combinedModel.add(ont, RDF.type, OWL.Ontology);
			var projectVersion = Objects.toString(getProject().getVersion(), null);
			if (projectVersion != null && !"unspecified".equals(projectVersion)) {
				combinedModel.add(ont, OWL.versionInfo, projectVersion);
			}
		}
	}

	private static void runBlankNodeFillers(Model combinedModel, String... rsrcNames) {
		for (long blankNodeCount = getCount(combinedModel, COUNT_BLANK_QUERY);;) {
			for (var rsrcName : rsrcNames) {
				runUpdate(combinedModel, rsrcName);
			}
			long newBlankNodeCount = getCount(combinedModel, COUNT_BLANK_QUERY);
			if (newBlankNodeCount < blankNodeCount) {
				System.out.format("Reduced blank node count from %1$d to %2$d%n",
					blankNodeCount, newBlankNodeCount);
				blankNodeCount = newBlankNodeCount;
			} else {
				break;
			}
		}
	}

	private static void runUpdate(Model combinedModel, String rsrcName) {
		System.out.format("Running update '%1$s'%n", rsrcName);
		var update = new ParameterizedSparqlString(JavaResource.getAsString(rsrcName));
		update.setIri("_fillNS", FILLED_IN_PREFIX.namespace());
		var updateReq = UpdateFactory.create(update.toString());
		var dataset = DatasetFactory.create(combinedModel);
		UpdateExecutionFactory.create(updateReq, dataset).execute();
	}

	private void runReports(Model combinedModel) throws IOException {
		if (!reportDir.isPresent()) {
			return;
		}
		reportDir.get().getAsFile().mkdirs();
		for (String rsrcName : REPORTS) {
			var reportFile = new File(reportDir.get().getAsFile(),
				getFileNameStem(rsrcName) + ".csv");
			System.out.format("Running report '%1$s'%n", reportFile.getPath());
			var query = QueryFactory.create(JavaResource.getAsString(rsrcName));
			List<String> vars = query.getResultVars();
			var csvFmt = CSVFormat.Builder.create(CSVFormat.DEFAULT)
				.setHeader(vars.toArray(new String[0]))
				.build();
			try (
				var strm = new QuerySolutionStream(query, combinedModel);
				var wtr = new FileWriter(reportFile, StandardCharsets.UTF_8);
				var csv = new CSVPrinter(wtr, csvFmt);
			) {
				strm
					.map(qs -> getSolutionAsStrings(qs, vars, combinedModel))
					.forEach(solutionStrings -> printCsvRow(solutionStrings, csv));
			}
		}
	}

	private static List<String> getSolutionAsStrings(QuerySolution qs, List<String> vars, Model combinedModel) {
		return vars.stream()
			.map(qs::get)
			.map(node -> getNodeAsString(node, combinedModel))
			.collect(Collectors.toUnmodifiableList());
	}

	private static String getNodeAsString(RDFNode node, PrefixMapping pm) {
		if (node == null) {
			return "";
		} else if (node.isAnon() || node.isLiteral()) {
			return node.toString();
		} else if (node.isURIResource()) {
			return pm.qnameFor(node.toString());
		} else {
			throw new IllegalStateException("Shouldn't happen");
		}
	}

	private static void printCsvRow(List<String> solutionStrings, CSVPrinter csv) {
		try {
			csv.printRecord(solutionStrings);
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private static void printOntStats(Model combinedModel, String label) {
		System.out.format(STATS_FORMAT, label, combinedModel.size(),
			getCount(combinedModel, COUNT_CLASS_QUERY),
			getCount(combinedModel, COUNT_PROP_QUERY),
			getCount(combinedModel, COUNT_RESTR_QUERY),
			getCount(combinedModel, COUNT_BLANK_QUERY));
	}

	private static long getCount(Model combinedModel, String queryRsrc) {
		var query = QueryFactory.create(JavaResource.getAsString(queryRsrc));
		var varName = query.getResultVars().get(0);
		try (var strm = new QuerySolutionStream(query, combinedModel)) {
			return strm.map(qs -> qs.getLiteral(varName))
				.filter(literal -> literal != null)
				.map(literal -> literal.getLong())
				.findFirst()
				.orElse(0L);
		}
	}

	private static String getFileNameStem(String fName) {
		String stem = new File(fName).getName();
		int dotIdx = stem.lastIndexOf('.');
		return (dotIdx < 0) ? stem : stem.substring(0, dotIdx);
	}

	private void writeCombinedOntology(Model model, OutputType outType) throws IOException {
		var file = getOutputFile(outType);
		var lang = RDFLanguages.filenameToLang(file.getName());
		System.out.format("Writing %1$s file '%2$s'%n", lang.getName(), file.getPath());
		try (OutputStream out = new FileOutputStream(file)) {
			if (lang.equals(Lang.TURTLE)) {
				RDFDataMgr.write(out, model, getTurtleVariant(outType));
			} else {
				RDFDataMgr.write(out, model, lang);
			}
		}
	}

	private File getOutputFile(OutputType outType) {
		var outFileProperty = (outType == OutputType.FOR_HUMANS)
			? humanOntFile
			: machineOntFile;
		return outFileProperty.get().getAsFile();
	}

	private static RDFFormat getTurtleVariant(OutputType outType) {
		return (outType == OutputType.FOR_HUMANS)
			? RDFFormat.TURTLE_PRETTY
			: RDFFormat.TURTLE_BLOCKS;
	}
}
