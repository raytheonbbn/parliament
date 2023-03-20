package com.bbn.parliament.ontology_bundle;

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
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;
import org.openjena.riot.Lang;

import com.bbn.parliament.util.CloseableQueryExecution;
import com.bbn.parliament.util.JavaResource;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.update.GraphStore;
import com.hp.hpl.jena.update.GraphStoreFactory;
import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateProcessor;
import com.hp.hpl.jena.update.UpdateRequest;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;

public class OntologyBundlerTask extends DefaultTask {
	private static enum OutputType { FOR_HUMANS, FOR_MACHINES }
	private static final String[] UPDATES_FOR_HUMANS = {
		"updates/deleteOntologyNodes.update",
		"updates/trimStringLiterals.update",
		"updates/deleteEmptyLiterals.update",
		"updates/removeOwlThingAsSuperClass.update",
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

	private final OntologyBundleExtension ontBundleExt = getProject().getExtensions()
		.getByType(OntologyBundleExtension.class);
	private FileCollection srcFiles = getProject().files();
	private File humanOutFile = null;
	private File machineOutFile = null;
	private String ontUri = null;
	private String ontVersion = null;

	private PrefixFileLoader prefixLoader;

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
			prefixLoader = new PrefixFileLoader(ontBundleExt.getPrefixFile());
			Model combinedModel = combineSourceFiles();
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

	private Model combineSourceFiles() throws IOException {
		Model combinedModel = ModelFactory.createDefaultModel();
		prefixLoader.addDeclaredPrefixesTo(combinedModel);
		for (File f : srcFiles.getFiles()) {
			Lang lang = Lang.guess(f.getName());
			if (lang == null) {
				System.out.format("Unrecognized RDF serialization:  '%1$s'%n", f.getPath());
				continue;
			}
			System.out.format("Reading %1$s file '%2$s'%n", lang.getName(), f.getPath());
			try (InputStream in = new FileInputStream(f)) {
				Model inputModel = ModelFactory.createDefaultModel();
				inputModel.read(in, null, lang.getName());
				prefixLoader.validateInputFilePrefixes(inputModel, f);
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

	private static void runBlankNodeFiller(Model combinedModel, String rsrcName) {
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

	private static void runUpdate(Model combinedModel, String rsrcName) {
		System.out.format("Running update '%1$s'%n", rsrcName);
		String updateStr = JavaResource.getAsString(rsrcName);
		UpdateRequest updateReq = UpdateFactory.create(updateStr);
		GraphStore graphStore = GraphStoreFactory.create(combinedModel);
		UpdateProcessor up = UpdateExecutionFactory.create(updateReq, graphStore);
		up.execute();
	}

	private void runReports(Model combinedModel) throws IOException {
		File reportsDir = ontBundleExt.getReportsDir();
		if (reportsDir == null) {
			return;	// This shouldn't happen
		}
		reportsDir.mkdirs();
		for (String rsrcName : REPORTS) {
			File reportFile = new File(reportsDir, getFileNameStem(rsrcName) + ".csv");
			System.out.format("Running report '%1$s'%n", reportFile.getPath());
			String queryStr = JavaResource.getAsString(rsrcName);
			try (CloseableQueryExecution qe = new CloseableQueryExecution(queryStr, combinedModel)) {
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

	private static void printOntStats(Model combinedModel, String label) {
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

	private static long getBlankNodeCount(Model combinedModel) {
		return getCount(combinedModel, COUNT_BLANK_QUERY);
	}

	private static long getCount(Model combinedModel, String queryRsrc) {
		long result = -1;
		Query query = QueryFactory.create(JavaResource.getAsString(queryRsrc));
		String varName = query.getResultVars().get(0);
		try (CloseableQueryExecution qe = new CloseableQueryExecution(query, combinedModel)) {
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
		Lang outLang = Lang.guess(outFile.getName(), Lang.TURTLE);
		System.out.format("Writing %1$s file '%2$s'%n", outLang.getName(), outFile.getPath());
		try (OutputStream out = new FileOutputStream(outFile)) {
			combinedModel.write(out, outLang.getName(), null);
		}
	}
}
