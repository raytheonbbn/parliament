package com.bbn.parliament.ontology_packager;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.gradle.api.DefaultTask;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;
import org.gradle.process.ExecResult;

public class ResourceClassGenerator extends DefaultTask {
	private static final String HEADER_RSRC_NAME = "schemagenHeader.txt";

	private File pfxFile;
	private List<String> pfxToGen;
	private File schemagenConfig;
	private File ontFile;
	private File outputDir;

	public ResourceClassGenerator() {
		pfxFile = null;
		pfxToGen = null;
		schemagenConfig = null;
		ontFile = null;
		outputDir = null;
	}

	@InputFile
	public File getPrefixesFile() {
		return pfxFile;
	}

	public void setPrefixesFile(File prefixesFile) {
		pfxFile = prefixesFile;
	}

	@Input
	public List<String> getDesiredPrefixes() {
		return Collections.unmodifiableList(pfxToGen);
	}

	public void setDesiredPrefixes(List<String> desiredPrefixes) {
		pfxToGen = desiredPrefixes;
	}

	@InputFile
	public File getSchemagenConfigFile() {
		return schemagenConfig;
	}

	public void setSchemagenConfigFile(File schemagenConfigFile) {
		schemagenConfig = schemagenConfigFile;
	}

	@InputFile
	public File getOntologyFile() {
		return ontFile;
	}

	public void setOntologyFile(File ontologyFile) {
		ontFile = ontologyFile;
	}

	@OutputDirectory
	public File getOutputDirectory() {
		return outputDir;
	}

	public void setOutputDirectory(File outputDirectory) {
		outputDir = outputDirectory;
	}

	@TaskAction
	public void run() {
		try {
			Map<String, String> pfxMap = loadPfxFile();
			String header = Util.getRsrcAsString(HEADER_RSRC_NAME);
			for (String pfx : pfxToGen) {
				String ns = pfxMap.get(pfx);
				if (ns == null) {
					throw new InvalidUserDataException(String.format("Unrecognized namespace prefix '%1$s'", pfx));
				}
				String clsName = prefixToClassName(pfx);
				System.out.format("Generating %1$s.java%n", clsName);
				ExecResult execResult = getProject().javaexec(spec -> {
					spec.setMain("jena.schemagen");
					spec.setClasspath(getProject().getConfigurations().getByName("runtimeClasspath").fileCollection(dep -> true));
					spec.args("-c", schemagenConfig.getPath());
					spec.args("-i", ontFile.getPath());
					spec.args("-a", ns);
					spec.args("-n", clsName);
					spec.args("-o", outputDir);
					spec.args("--header", header);
				});
				execResult.assertNormalExitValue();
			}
		} catch (IOException ex) {
			throw new TaskExecutionException(this, ex);
		}
	}

	private Map<String, String> loadPfxFile() throws IOException {
		Properties p = new Properties();
		try (BufferedReader rdr = Files.newBufferedReader(pfxFile.toPath(), StandardCharsets.UTF_8)) {
			p.load(rdr);
		}
		return p.entrySet().stream().collect(Collectors.toMap(
			e -> e.getKey().toString(),	// key mapper
			e -> e.getValue().toString(),	// value mapper
			(v1, v2) -> v1,					// merge function -- not actually needed
			() -> new TreeMap<>()));		// map supplier
	}

	private static String prefixToClassName(String pfx) {
		StringBuilder buffer = new StringBuilder();
		for (int i = 0; i < pfx.length(); ++i) {
			char c = pfx.charAt(i);
			buffer.append((i == 0) ? Character.toUpperCase(c) : c);
		}
		return buffer.toString();
	}
}
