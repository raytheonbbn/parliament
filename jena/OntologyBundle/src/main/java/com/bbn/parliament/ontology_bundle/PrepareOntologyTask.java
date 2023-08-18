// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2023, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.ontology_bundle;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFLanguages;
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

public class PrepareOntologyTask extends DefaultTask {
	private static enum OutputType { FOR_HUMANS, FOR_MACHINES }

	private static final PrefixInfo FILLED_IN_PREFIX = new PrefixInfo("fill", null,
		"http://parliament.semwebcentral.org/filled-in-blank-node#");
	private static final String[] UPDATES_FOR_HUMANS = {
		"deleteOntologyNodes.update",
		"trimStringLiterals.update",
		"deleteEmptyLiterals.update",
		"removeOwlThingAsSuperClass.update",
	};
	private static final String[] UPDATES_FOR_MACHINES = {
		"deleteAnnotationProperties.update",
	};
	private static final String[] REPORTS = {
	};
	private static final String BLANK_PATTERN_UPDATE = "replaceBlankPatternRestrictions.update";
	private static final String BLANK_INVERSE_PROP_UPDATE = "replaceBlankInverseProp.update";
	private static final String BLANK_LIST_UPDATE = "replaceBlankListNodes.update";
	private static final String BLANK_UNION_DOMAIN_AND_RANGE_UPDATE = "replaceBlankUnionDomainAndRange.update";
	private static final String BLANK_RESTR_UPDATE = "replaceBlankRestrictions.update";
	private static final String BLANK_AXIOM_ANN_UPDATE = "replaceBlankAxiomAnnotation.update";
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
	private Property<String> ontVersion;
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
		ontVersion = objFact.property(String.class);
		ontVersion.set(ext.getOntologyVersion());
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

	@Input
	@Optional
	public Property<String> getOntologyVersion() {
		return ontVersion;
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
			runBlankNodeFillers(combinedModel,
				BLANK_AXIOM_ANN_UPDATE,
				BLANK_INVERSE_PROP_UPDATE,
				BLANK_PATTERN_UPDATE,
				BLANK_UNION_DOMAIN_AND_RANGE_UPDATE,
				BLANK_RESTR_UPDATE,
				BLANK_LIST_UPDATE);

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
			if (ontVersion.isPresent()) {
				combinedModel.add(ont, OWL.versionInfo, ontVersion.get());
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
			var queryStr = JavaResource.getAsString(rsrcName);
			try (var qe = QueryExecutionFactory.create(queryStr, combinedModel)) {
				ResultSet rs = qe.execSelect();
				List<String> vars = rs.getResultVars();
				var csvFmt = CSVFormat.Builder.create(CSVFormat.DEFAULT)
					.setHeader(vars.toArray(new String[0]))
					.build();
				try (
					var wtr = Files.newBufferedWriter(reportFile.toPath(), StandardCharsets.UTF_8);
					var csv = new CSVPrinter(wtr, csvFmt);
				) {
					while (rs.hasNext()) {
						QuerySolution qs = rs.next();
						List<String> resultStrings = new ArrayList<>();
						for (String var : vars) {
							RDFNode node = qs.get(var);
							if (node == null) {
								resultStrings.add("");
							} else if (node.isAnon()) {
								resultStrings.add(node.toString());
							} else if (node.isURIResource()) {
								resultStrings.add(combinedModel.qnameFor(node.toString()));
							} else if (node.isLiteral()) {
								resultStrings.add(node.toString());
							} else {
								throw new IllegalStateException("Shouldn't happen");
							}
						}
						csv.printRecord(resultStrings);
					}
				}
			}
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
