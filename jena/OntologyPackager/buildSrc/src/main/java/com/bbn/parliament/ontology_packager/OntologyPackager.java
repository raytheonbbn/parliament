package com.bbn.parliament.ontology_packager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.gradle.api.DefaultTask;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;

public class OntologyPackager extends DefaultTask {
	private static enum OutputType { FOR_HUMANS, FOR_MACHINES }
	private static final String[] UPDATES_FOR_HUMANS = {
		"updates/deleteOntologyNodes.update",
		"updates/trimStringLiterals.update",
		"updates/deleteEmptyLiterals.update",
		"updates/removeOwlThingAsSuperClass.update",
		"updates/deleteTopProperties.update",
	};
	private static final String[] UPDATES_FOR_MACHINES = {
		"updates/deleteAnnotationProperties.update",
	};
	private static final String[] REPORTS = {
	};
	private static final String BLANK_LIST_UPDATE = "updates/replaceBlankListNodes.update";
	private static final String BLANK_UNION_DOMAIN_AND_RANGE_UPDATE = "updates/replaceBlankUnionDomainAndRange.update";
	private static final String BLANK_RESTR_UPDATE = "updates/replaceBlankRestrictions.update";
	private static final String COUNT_BLANK_QUERY = "queries/countBlankNodes.sparql";
	private static final String COUNT_CLASS_QUERY = "queries/countClasses.sparql";
	private static final String COUNT_PROP_QUERY = "queries/countProperties.sparql";
	private static final String COUNT_RESTR_QUERY = "queries/countRestrictions.sparql";

	private String outputSerialiation = null;
	private FileCollection srcFiles = getProject().files();
	private File pfxFile = null;
	private File reportDir = null;
	private File humanOutFile = null;
	private File machineOutFile = null;
	private String ontUri = null;
	private String ontVersion = null;

	@Input
	@Optional
	public String getOutputSerialiation() {
		return outputSerialiation;
	}

	public void setOutputSerialiation(String rdfSerialiation) {
		outputSerialiation = rdfSerialiation;
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

	@InputFile
	public File getPrefixesFile() {
		return pfxFile;
	}

	public void setPrefixesFile(File prefixesFile) {
		pfxFile = prefixesFile;
	}

	@OutputDirectory
	@Optional
	public File getReportDir() {
		return reportDir;
	}

	public void setReportDir(File reportDirectory) {
		reportDir = reportDirectory;
	}

	@OutputFile
	public File getHumanReadableOutputFile() {
		return humanOutFile;
	}

	public void setHumanReadableOutputFile(File humanReadableOutputFile) {
		humanOutFile = humanReadableOutputFile;
	}

	@OutputFile
	public File getMachineReadableOutputFile() {
		return machineOutFile;
	}

	public void setMachineReadableOutputFile(File machineReadableOutputFile) {
		machineOutFile = machineReadableOutputFile;
	}

	@Input
	@Optional
	public String getOntologyUri() {
		return ontUri;
	}

	public void setOntologyUri(String ontologyUri) {
		ontUri = ontologyUri;
	}

	@Input
	@Optional
	public String getOntologyVersion() {
		return ontVersion;
	}

	public void setOntologyVersion(String ontologyVersion) {
		ontVersion = ontologyVersion;
	}

	@TaskAction
	public void run() {
		try {
			PrefixMapping pfxMap = loadPfxFile();
			Model combinedModel = combinedSourceFiles(pfxMap);
			printOntStats(combinedModel, "initial");

			runReports(combinedModel);
			for (String rsrcName : UPDATES_FOR_HUMANS) {
				runUpdate(combinedModel, rsrcName);
			}

			insertOntologyNode(combinedModel);

			printOntStats(combinedModel, "human-readable");
			writeCombinedOutputOntology(combinedModel, OutputType.FOR_HUMANS);

			for (String rsrcName : UPDATES_FOR_MACHINES) {
				runUpdate(combinedModel, rsrcName);
			}

			combinedModel.setNsPrefix("fillb", "http://bbn.com/tbox/buc/infoexploit/filled-blank#");
			runBlankNodeFiller(combinedModel, BLANK_LIST_UPDATE);
			runBlankNodeFiller(combinedModel, BLANK_UNION_DOMAIN_AND_RANGE_UPDATE);
			runBlankNodeFiller(combinedModel, BLANK_RESTR_UPDATE);
			runBlankNodeFiller(combinedModel, BLANK_LIST_UPDATE);

			printOntStats(combinedModel, "machine-readable");
			writeCombinedOutputOntology(combinedModel, OutputType.FOR_MACHINES);
		} catch (IOException ex) {
			throw new TaskExecutionException(this, ex);
		}
	}

	private PrefixMapping loadPfxFile() throws IOException {
		// Load the properties file:
		Properties p = new Properties();
		try (BufferedReader rdr = Files.newBufferedReader(pfxFile.toPath(), StandardCharsets.UTF_8)) {
			p.load(rdr);
		}

		// Transfer the key-value pairs from the properties into a prefix mapping:
		PrefixMapping pm = PrefixMapping.Factory.create();
		pm.setNsPrefixes(PrefixMapping.Standard);
		p.forEach((k, v) -> pm.setNsPrefix(k.toString(), v.toString()));

		// Create a reverse map from namespaces to prefixes:
		Map<String, Set<String>> reverseMap = Util.reverseMap(pm.getNsPrefixMap());

		// Now check for namespaces that have more than one prefix:
		String errors = reverseMap.entrySet().stream()
			.filter(e -> e.getValue().size() > 1)
			.map(e -> { return String.format("<%1$s>:  %2$s", e.getKey(),
				e.getValue().stream().collect(Collectors.joining("', '", "'", "'"))); } )
			.collect(Collectors.joining(String.format("%n   ")));
		if (!errors.isEmpty()) {
			throw new InvalidUserDataException(String.format(
				"The following namespaces have multiple prefixes:%n   %1$s", errors));
		}

		// Finally, save the prefix mapping for later use:
		return pm;
	}

	private Model combinedSourceFiles(PrefixMapping pfxMap) throws IOException {
		Model combinedModel = ModelFactory.createDefaultModel();
		combinedModel.setNsPrefixes(pfxMap);
		for (File f : srcFiles.getFiles()) {
			Lang lang = RDFLanguages.filenameToLang(f.getName());
			if (lang == null) {
				System.out.format("Unrecognized RDF serialization:  '%1$s'%n", f.getPath());
				continue;
			}
			System.out.format("Reading %1$s file '%2$s'%n", lang.getName(), f.getPath());
			try (InputStream in = new FileInputStream(f)) {
				Model inputModel = ModelFactory.createDefaultModel();
				RDFDataMgr.read(inputModel, in, null, lang);
				validateInputModelPrefixes(inputModel, combinedModel, f);
				combinedModel.add(inputModel);
				combinedModel.removeNsPrefix("");
			}
		}
		return combinedModel;
	}

	private void insertOntologyNode(Model combinedModel) {
		if (ontUri != null && !ontUri.isEmpty()) {
			Resource ont = combinedModel.createResource(combinedModel.expandPrefix(ontUri));
			combinedModel.add(ont, RDF.type, OWL.Ontology);
			if (ontVersion != null && !ontVersion.isEmpty()) {
				combinedModel.add(ont, OWL.versionInfo, ontVersion);
			}
		}
	}

	private void validateInputModelPrefixes(Model inputModel, Model combinedModel, File inputFile) {
		// Check for namespaces in the inputModel that are not declared in the combinedModel:
		Collection<String> combinedModelNamespaces = combinedModel.getNsPrefixMap().values();
		String errors = inputModel.getNsPrefixMap().entrySet().stream()
			.filter(e -> !combinedModelNamespaces.contains(e.getValue()))
			.map(e -> String.format("%1$s: <%2$s>", e.getKey(), e.getValue()))
			.collect(Collectors.joining(String.format("%n   ")));
		if (!errors.isEmpty()) {
			throw new InvalidUserDataException(String.format(
				"These namespaces are declared in '%1$s' but missing from '%2$s':%n   %3$s",
				inputFile.getPath(), pfxFile.getPath(), errors));
		}

		// Checks for namespaces declared in both models, but with different prefixes:
		// Create reverse maps from namespaces to prefixes:
		Map<String, Set<String>> reverseInputMap = Util.reverseMap(inputModel.getNsPrefixMap());
		Map<String, Set<String>> reverseCombinedMap = Util.reverseMap(combinedModel.getNsPrefixMap());
		errors = reverseCombinedMap.entrySet().stream()
			// Only pay attention to input namespaces that are in the combined model:
			.filter(e -> reverseInputMap.containsKey(e.getKey()))
			// Ignore the empty prefix in the input model:
			.filter(e -> reverseInputMap.get(e.getKey()).size() != 1 || !reverseInputMap.get(e.getKey()).contains(""))
			// Check for mismatches:
			.filter(e -> !reverseInputMap.get(e.getKey()).containsAll(e.getValue())
				|| !e.getValue().containsAll(reverseInputMap.get(e.getKey())))
			// Toss away the prefixes:
			.map(e -> e.getKey())
			.collect(Collectors.joining(String.format("%n   ")));
		if (!errors.isEmpty()) {
			throw new InvalidUserDataException(String.format(
				"These namespaces have different prefixes in '%1$s' and '%2$s':%n   %3$s",
				inputFile.getPath(), pfxFile.getPath(), errors));
		}
	}

	private static void runBlankNodeFiller(Model combinedModel, String rsrcName) throws IOException {
		String updateName =  getFileNameStem(rsrcName);
		for (long blankNodeCount = getBlankNodeCount(combinedModel);;) {
			runUpdate(combinedModel, rsrcName);
			long newBlankNodeCount = getBlankNodeCount(combinedModel);
			if (newBlankNodeCount < blankNodeCount) {
				System.out.format("%1$s reduced blank node count from %2$d to %3$d%n",
					updateName, blankNodeCount, newBlankNodeCount);
				blankNodeCount = newBlankNodeCount;
			} else {
				break;
			}
		}
	}

	private static void runUpdate(Model combinedModel, String rsrcName) throws IOException {
		System.out.format("Running update '%1$s'%n", rsrcName);
		String updateStr = Util.getRsrcAsString(rsrcName);
		UpdateRequest updateReq = UpdateFactory.create(updateStr);
		Dataset dataset = DatasetFactory.create(combinedModel);
		UpdateProcessor up = UpdateExecutionFactory.create(updateReq, dataset);
		up.execute();
	}

	private void runReports(Model combinedModel) throws IOException {
		if (reportDir != null) {
			reportDir.mkdirs();
			for (String rsrcName : REPORTS) {
				File reportFile = new File(reportDir, getFileNameStem(rsrcName) + ".csv");
				System.out.format("Running report '%1$s'%n", reportFile.getPath());
				String queryStr = Util.getRsrcAsString(rsrcName);
				try (QueryExecution qe = QueryExecutionFactory.create(queryStr, combinedModel)) {
					ResultSet rs = qe.execSelect();
					List<String> vars = rs.getResultVars();
					CSVFormat csvFmt = CSVFormat.DEFAULT.withHeader(vars.toArray(new String[0]));
					try (
						BufferedWriter wtr = Files.newBufferedWriter(reportFile.toPath(), StandardCharsets.UTF_8);
						CSVPrinter csv = new CSVPrinter(wtr, csvFmt);
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
									throw new RuntimeException("Shouldn't happen");
								}
							}
							csv.printRecord(resultStrings);
						}
					}
				}
			}
		}
	}

	private static void printOntStats(Model combinedModel, String label) throws IOException {
		System.out.format("The %1$s ontology contains:%n"
			+ "   %2$d statements%n"
			+ "   %3$d classes%n"
			+ "   %4$d properties%n"
			+ "   %5$d restrictions%n"
			+ "   %6$d blank nodes%n",
			label, combinedModel.size(),
			getCount(combinedModel, COUNT_CLASS_QUERY),
			getCount(combinedModel, COUNT_PROP_QUERY),
			getCount(combinedModel, COUNT_RESTR_QUERY),
			getBlankNodeCount(combinedModel));
	}

	private static long getBlankNodeCount(Model combinedModel) throws IOException {
		return getCount(combinedModel, COUNT_BLANK_QUERY);
	}

	private static long getCount(Model combinedModel, String queryRsrc) throws IOException {
		long result = -1;
		Query query = QueryFactory.create(Util.getRsrcAsString(queryRsrc));
		String varName = query.getResultVars().get(0);
		try (QueryExecution qe = QueryExecutionFactory.create(query, combinedModel)) {
			ResultSet rs = qe.execSelect();
			while (rs.hasNext()) {
				QuerySolution qs = rs.next();
				Literal node = qs.getLiteral(varName);
				if (node != null) {
					result = node.getLong();
				}
			}
		}
		return result;
	}

	private static String getFileNameStem(String fName) {
		String stem = new File(fName).getName();
		int dotIdx = stem.lastIndexOf('.');
		return (dotIdx < 0) ? stem : stem.substring(0, dotIdx);
	}

	private void writeCombinedOutputOntology(Model combinedModel, OutputType outType) throws IOException {
		File outFile = (outType == OutputType.FOR_HUMANS) ? humanOutFile : machineOutFile;
		Lang outLang = (outputSerialiation == null || outputSerialiation.isEmpty())
			? RDFLanguages.filenameToLang(outFile.getName())
			: RDFLanguages.nameToLang(outputSerialiation);
		System.out.format("Writing %1$s file '%2$s'%n", outLang.getName(), outFile.getPath());
		try (OutputStream out = new FileOutputStream(outFile)) {
			if (outLang.equals(Lang.TURTLE)) {
				RDFFormat fmt = (outType == OutputType.FOR_HUMANS) ? RDFFormat.TURTLE_PRETTY : RDFFormat.TURTLE_BLOCKS;
				RDFDataMgr.write(out, combinedModel, fmt);
			} else {
				RDFDataMgr.write(out, combinedModel, outLang);
			}
		}
	}
}
