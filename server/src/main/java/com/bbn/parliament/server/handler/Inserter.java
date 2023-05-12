// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.server.handler;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.kb_graph.KbGraphStore;
import com.bbn.parliament.server.exception.DataFormatException;
import com.bbn.parliament.server.exception.MissingGraphException;
import com.bbn.parliament.server.graph.ModelManager;

public final class Inserter {
	private static final Property GRAPH_DIR_PROPERTY = ResourceFactory.createProperty(KbGraphStore.GRAPH_DIR_PROPERTY.getURI());
	private static final Resource INDEXED_GRAPH = ResourceFactory.createResource(KbGraphStore.INDEXED_GRAPH.getURI());
	private static final Resource UNION_GRAPH_CLASS = ResourceFactory.createResource(KbGraphStore.UNION_GRAPH_CLASS.getURI());
	private static final Logger LOG = LoggerFactory.getLogger(Inserter.class);

	private final boolean importRepository;
	private final String graphName;
	private final String dataFormat;
	private final String fileName;
	private final VerifyOption verifyOption;
	private final Lang rdfLang;
	private final String baseUri;
	private final Supplier<InputStream> streamSupplier;
	private long numStatements;

	public static Inserter newGraphInserter(String graphName, String dataFormat,
			String fileName, VerifyOption verifyOption, String baseUri,
			Supplier<InputStream> streamSupplier) throws DataFormatException {
		return new Inserter(graphName, dataFormat, fileName, verifyOption, baseUri,
			streamSupplier);
	}

	public static Inserter newRepositoryInserter(String baseUri,
			Supplier<InputStream> streamSupplier) {
		return new Inserter(baseUri, streamSupplier);
	}

	private Inserter(String baseUri, Supplier<InputStream> streamSupplier) {
		importRepository = true;
		this.graphName = null;
		this.dataFormat = null;
		this.fileName = null;
		this.verifyOption = null;
		rdfLang = null;
		this.baseUri = baseUri;
		this.streamSupplier = Objects.requireNonNull(streamSupplier, "streamSupplier");
		numStatements = 0;
	}

	private Inserter(String graphName, String dataFormat, String fileName,
		VerifyOption verifyOption, String baseUri, Supplier<InputStream> streamSupplier)
			throws DataFormatException {
		importRepository = false;
		this.graphName = graphName;
		this.dataFormat = dataFormat;
		this.fileName = fileName;
		this.verifyOption = Objects.requireNonNull(verifyOption, "verifyOption");
		rdfLang = Inserter.getRdfLang(this.dataFormat, this.fileName);
		this.baseUri = baseUri;
		this.streamSupplier = Objects.requireNonNull(streamSupplier, "streamSupplier");
		numStatements = 0;
	}

	public long getNumStatements() {
		return numStatements;
	}

	public boolean isImport() {
		return importRepository;
	}

	public String getFileName() {
		return fileName;
	}

	public void run() throws IOException, DataFormatException, MissingGraphException {
		numStatements = 0;
		if (importRepository) {
			importRepository();
		} else {
			importGraph();
		}
	}

	//TODO: refactor this method into smaller units
	private void importRepository() throws IOException, DataFormatException {
		LOG.info("Repository import");

		// First verify that we have a legitimate import
		Model masterGraph = null;
		Set<String> dirNamesSeen = new HashSet<>();
		try (
			InputStream in = streamSupplier.get();
			ZipInputStream zin = new ZipInputStream(in);
		) {
			ZipEntry ze = null;
			while ((ze = zin.getNextEntry()) != null) {
				String zipEntryName = ze.getName();
				FileNameDecomposition decomp = new FileNameDecomposition(zipEntryName);
				if (decomp.getLang() == null) {
					var extensions = RDFLanguages.getRegisteredLanguages().stream()
						.filter(lang -> !RDFLanguages.sameLang(lang, Lang.RDFNULL))
						.flatMap(lang -> lang.getFileExtensions().stream())
						.collect(Collectors.toCollection(TreeSet::new));
					var extList = extensions.stream()
						.collect(Collectors.joining("', '"));
					throw new DataFormatException(
						"Unsupported file extension on \"%1$s\": Must be one of '%2$s'",
						zipEntryName, extList);
				}

				Supplier<InputStream> entryStrmProvider = getZipStrmProvider(zin);

				// Get the Master Graph separately as a temporary in-memory model
				if (decomp.isMasterGraph()) {
					masterGraph = ModelFactory.createDefaultModel();
					try (InputStream entryStream = entryStrmProvider.get()) {
						masterGraph.read(entryStream, baseUri, decomp.getLang().getName());
					}
				} else {
					long num = verify(entryStrmProvider, decomp.getLang());
					if (num > 0) {
						numStatements += num;
					}

					if (!decomp.isDefaultGraph()) {
						dirNamesSeen.add(decomp.getDirName());
					}
				}

				zin.closeEntry();
			}
		}

		if (masterGraph == null) {
			throw new DataFormatException("Archive has no Master Graph");
		}

		// Verify that all the filenames seen are in the Master Graph, and vice-versa
		Map<String, String> dirToGraphNameMap = new HashMap<>();
		StmtIterator it = masterGraph.listStatements(null, GRAPH_DIR_PROPERTY, (RDFNode) null);
		try {
			while (it.hasNext()) {
				Statement stmt = it.nextStatement();

				String graphNm = stmt.getSubject().getURI();
				String dirName = stmt.getObject().toString();

				if (!dirNamesSeen.contains(dirName)) {
					throw new DataFormatException("""
						The Master Graph contains a directory name (%1$s) not present in \
						the zip file""", dirName);
				}

				dirToGraphNameMap.put(dirName, graphNm);
			}
		} finally {
			if (null != it ) {
				it.close();
			}
		}

		if (dirNamesSeen.size() != dirToGraphNameMap.size()) {
			String extraDirs = dirNamesSeen.stream()
				.filter(dirName -> !dirToGraphNameMap.containsKey(dirName))
				.collect(Collectors.joining(", "));
			throw new DataFormatException("""
				Mismatch between the number of files in the zip file and the number in the \
				Master Graph.  There are extra directories in the zip file: %1$s""", extraDirs);
		}

		// Now that we like the input, we can clear the old repo
		LOG.info("Clearing current repository...");
		ModelManager.inst().clearKb();

		Set<String> indexGraphs = new HashSet<>(dirToGraphNameMap.size());
		it = masterGraph.listStatements(null, RDF.type, INDEXED_GRAPH);
		try {
			while (it.hasNext()) {
				indexGraphs.add(it.next().getSubject().getURI());
			}
		} finally {
			if (null != it) {
				it.close();
			}
		}
		// Insert the new data
		try (
			InputStream in = streamSupplier.get();
			ZipInputStream zin = new ZipInputStream(in);
		) {
			ZipEntry ze = null;
			while ((ze = zin.getNextEntry()) != null) {
				FileNameDecomposition decomp = new FileNameDecomposition(ze.getName());
				// We can assume that decomp.getFormat() is not null because that
				// was checked in the verification loop above.

				Supplier<InputStream> entryStrmProvider = getZipStrmProvider(zin);

				if (decomp.isMasterGraph()) {
					// Do nothing (ignore the Master Graph)
				} else if (decomp.isDefaultGraph()) {
					Model model = ModelManager.inst().getDefaultModel();
					insert(model, KbGraphStore.DEFAULT_GRAPH_BASENAME, entryStrmProvider, decomp.getLang());
				} else {
					String graphDir = decomp.getDirName();
					String graphNm = dirToGraphNameMap.get(graphDir);
					Model model = ModelManager.inst().createAndAddNamedModel(graphNm, graphDir, indexGraphs.contains(graphNm));
					insert(model, graphNm, entryStrmProvider, decomp.getLang());
				}

				zin.closeEntry();
			}
		}

		// Add any KbUnionGraphs
		it = masterGraph.listStatements(null, RDF.type, UNION_GRAPH_CLASS);
		try {
			while (it.hasNext()) {
				Resource subject = it.next().getSubject();
				String graphNm = subject.getURI();
				String leftGraphName = null;
				String rightGraphName = null;

				StmtIterator it2 = masterGraph.listStatements(subject, null, (RDFNode) null);
				try {
					while (it2.hasNext()) {
						Statement stmt = it2.next();
						if (KbGraphStore.LEFT_GRAPH_PROPERTY.equals(stmt.asTriple().getPredicate())) {
							leftGraphName = ((Resource)stmt.getObject()).getURI();
						}
						if (KbGraphStore.RIGHT_GRAPH_PROPERTY.equals(stmt.asTriple().getPredicate())) {
							rightGraphName = ((Resource)stmt.getObject()).getURI();
						}
					}
				} finally {
					if (null != it2) {
						it2.close();
					}
				}

				ModelManager.inst().createAndAddKbUnionGraph(graphNm, leftGraphName, rightGraphName);
			}
		} finally {
			if (null != it) {
				it.close();
			}
		}
	}

	private static class FileNameDecomposition {
		private Lang fileLang;
		private String dirName;

		public FileNameDecomposition(String fname) {
			fileLang = RDFLanguages.pathnameToLang(fname);
			dirName = null;
			int dotIndex = fname.lastIndexOf('.');
			if (dotIndex >= 0 && (dotIndex + 1) < fname.length()) {
				dirName = fname.substring(0, dotIndex);
			}
		}

		public Lang getLang() {
			return fileLang;
		}

		public String getDirName() {
			return dirName;
		}

		public boolean isMasterGraph() {
			return dirName.equalsIgnoreCase(KbGraphStore.MASTER_GRAPH_DIR)
				|| dirName.equalsIgnoreCase(KbGraphStore.OLD_MASTER_GRAPH_DIR);
		}

		public boolean isDefaultGraph() {
			return dirName.equalsIgnoreCase(KbGraphStore.DEFAULT_GRAPH_BASENAME);
		}
	}

	private static Supplier<InputStream> getZipStrmProvider(ZipInputStream zin) {
		return () -> new FilterInputStream(zin) {
			@Override
			public void close() throws IOException {
				// Do nothing
			}
		};
	}

	public void importGraph() throws IOException, MissingGraphException {
		LOG.info("File or text insert from file '{}'", fileName);

		boolean isDefaultGraph = (graphName == null || graphName.isEmpty());
		Model model = isDefaultGraph
			? ModelManager.inst().getDefaultModel()
			: ModelManager.inst().getModel(graphName);
		String graphLabel = isDefaultGraph ? KbGraphStore.DEFAULT_GRAPH_BASENAME : graphName;

		if (null == model) {
			throw new MissingGraphException("There is no graph named \"%1$s\"", graphName);
		}

		if (verifyOption == VerifyOption.VERIFY) {
			numStatements = verify(streamSupplier, rdfLang);
		}

		insert(model, graphLabel, streamSupplier, rdfLang);
	}

	/**
	 * Verifies the statements contained in the Supplier<InputStream>. If the statements are
	 * not valid, we throw a RuntimeException.
	 *
	 * @return the number of statements in the stream
	 */
	private long verify(Supplier<InputStream> inputStreamSupplier, Lang strmLang) throws IOException {
		long numStmts = 0;
		long start = Calendar.getInstance().getTimeInMillis();
		Model syntaxVerifier = ModelFactory.createModelForGraph(new ForgetfulGraph());
		try (InputStream in = inputStreamSupplier.get()) {
			syntaxVerifier.read(in, baseUri, strmLang.getName());
			numStmts = syntaxVerifier.size();

			if (LOG.isInfoEnabled()) {
				long end = Calendar.getInstance().getTimeInMillis();
				LOG.info("Verified %1$d statements in %2$.3f seconds".formatted(
					numStmts, (end - start) / 1000.0));
			}
		}
		return numStmts;
	}

	/** Inserts the statements from the InputStream into the given Model. */
	private void insert(Model model, String graphLabel, Supplier<InputStream> inputStreamSupplier,
		Lang strmLang) throws IOException {

		long start = Calendar.getInstance().getTimeInMillis();
		try (InputStream in = inputStreamSupplier.get()) {
			model.read(in, baseUri, strmLang.getName());

			if (LOG.isInfoEnabled()) {
				long end = Calendar.getInstance().getTimeInMillis();
				LOG.info("Added statements to \"%1$s\" in %2$.3f seconds".formatted(
					graphLabel, (end - start) / 1000.0));
			}
		}
	}

	/** Use the dataFormat and file extension to determine the RDF serialization format */
	private static Lang getRdfLang(String dataFormat, String fileName) throws DataFormatException {
		if (dataFormat == null || dataFormat.isEmpty() || "auto".equalsIgnoreCase(dataFormat)) {
			if (null == fileName || fileName.isEmpty()) {
				throw new DataFormatException("""
					The serialization format of the RDF was specified to be determined by \
					the fileName extension, but the fileName was not available.  Please \
					resubmit with the proper data format specified.""");
			}

			var lang = RDFLanguages.pathnameToLang(fileName);
			if (lang == null) {
				throw new DataFormatException("""
					Unable to determine the serialization format of the RDF document from \
					the fileName extension.  Please resubmit with the proper data format \
					specified.""");
			}
			LOG.debug("Mapping input fileName '{}' to {} format", fileName, lang);
			return lang;
		} else {
			var lang = RDFLanguages.contentTypeToLang(dataFormat);
			if (lang ==null && null != fileName && !fileName.isEmpty()) {
				lang = RDFLanguages.pathnameToLang(fileName);
			}
			if (lang == null) {
				throw new DataFormatException("Unsupported data format \"%1$s\"", dataFormat);
			}
			return lang;
		}
	}
}