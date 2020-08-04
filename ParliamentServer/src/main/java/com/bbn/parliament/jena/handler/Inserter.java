// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.handler;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jena.bridge.servlet.ServletErrorResponseException;
import com.bbn.parliament.jena.graph.ForgetfulGraph;
import com.bbn.parliament.jena.graph.KbGraphStore;
import com.bbn.parliament.jena.graph.ModelManager;
import com.bbn.parliament.jena.joseki.client.RDFFormat;
import com.bbn.parliament.jena.util.JsonLdRdfReader;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;

public class Inserter {
	/** Interface that defines a method for getting an InputStream from some object. */
	protected interface IInputStreamProvider {
		public InputStream getInputStream() throws IOException;
	}

	private static final Logger LOG = LoggerFactory.getLogger(Inserter.class);

	private String graphName;
	private IInputStreamProvider strmPrvdr;
	private String dataFormat;
	private String base;
	private String filename;
	private boolean verify;
	private boolean importRepo;
	private long numStatements;

	public Inserter(String graphName, IInputStreamProvider strmPrvdr,
		String dataFormat, String base, String verifyString, String importString,
		String filename) {
		this.graphName = graphName;
		this.strmPrvdr = strmPrvdr;
		this.dataFormat = dataFormat;
		this.base = base;
		this.filename = filename;

		verify = !"no".equalsIgnoreCase(verifyString);

		// Default to false, and only set to true if "yes" is passed
		importRepo = "yes".equalsIgnoreCase(importString);
	}

	public long getNumStatements() {
		return numStatements;
	}

	public boolean isImport() {
		return importRepo;
	}

	public String getGraphName() {
		return graphName;
	}

	public String getFileName() {
		return filename;
	}

	public void run() throws IOException, ServletErrorResponseException {
		numStatements = -1;
		if (importRepo) {
			if (LOG.isInfoEnabled()) {
				LOG.info("REPOSITORY IMPORT");
			}
			numStatements = importRepository(strmPrvdr, base);
		} else {
			if (LOG.isInfoEnabled()) {
				LOG.info("FILE OR TEXT INSERT");
				LOG.info("Filename: " + filename);
			}

			// Determine the RDF serialization format by looking at the file extension:
			RDFFormat format = null;
			if ("auto".equalsIgnoreCase(dataFormat)) {
				if (null == filename || filename.length() == 0) {
					throw new ServletErrorResponseException("The serialization format of "
						+ "the RDF was specified to be determined by the filename "
						+ "extension, but the filename was not availible.  Please "
						+ "resubmit with the proper data format specified.");
				} else {
					format = RDFFormat.parseFilename(filename);
					if (RDFFormat.UNKNOWN == format) {
						throw new ServletErrorResponseException("Unable to determine the "
							+ "serialization format of the RDF document from the "
							+ "filename extension.  Please resubmit with the "
							+ "proper data format specified.");
					} else {
						LOG.debug("Mapping input filename '{}' to {} format",
							filename, format);
					}
				}
			} else {
				format = RDFFormat.parseMediaType(dataFormat);
				if (filename != null && RDFFormat.UNKNOWN == format) {
					format = RDFFormat.parseFilename(filename);
				}

				if (RDFFormat.UNKNOWN == format) {
					throw new ServletErrorResponseException("Unsupported data format \"%1$s\"",
						dataFormat);
				}
			}
			// Disable this check for now.  Assume the user knows what he's doing if he wants to add stuff to the Master Graph.
			//checkIfMaster(graphName);

			boolean isDefaultGraph = "".equals(graphName);
			Model model = isDefaultGraph
				? ModelManager.inst().getDefaultModel()
				: ModelManager.inst().getModel(graphName);
			String graphLabel = isDefaultGraph ? "Default Graph" : graphName;

			if (null == model) {
				throw new ServletErrorResponseException(
					"There was no named graph with name \"%1$s\"", graphName);
			} else {
				if (verify) {
					numStatements = verify(strmPrvdr, base, format);
				}
				insert(model, graphLabel, strmPrvdr, format, base);
			}
		}
	}

	protected long importRepository(IInputStreamProvider inStrmPrvdr, String baseUri)
		throws IOException, ServletErrorResponseException {
		long toReturn = 0;

		// First verify that we have a legitimate import
		Model masterGraph = null;
		Set<String> dirNamesSeen = new HashSet<>();
		try (
			InputStream in = inStrmPrvdr.getInputStream();
			final ZipInputStream zin = new ZipInputStream(in);
			) {
			ZipEntry ze = null;
			while ((ze = zin.getNextEntry()) != null) {
				String zipEntryName = ze.getName();
				FileNameDecomposition decomp = new FileNameDecomposition(zipEntryName);
				if (RDFFormat.UNKNOWN == decomp.getFormat()) {
					throw new ServletErrorResponseException("Unsupported file extension "
						+ "on \"%1$s\" -- must be one of 'ttl', 'n3', 'nt', "
						+ "'rdf', 'owl', or 'xml'", zipEntryName);
				}

				IInputStreamProvider entryStrmProvider = getZipStrmProvider(zin);

				// Get the Master Graph separately as a temporary in-memory model
				if (decomp.isMasterGraph()) {
					masterGraph = ModelFactory.createDefaultModel();
					masterGraph.read(entryStrmProvider.getInputStream(), baseUri, decomp.getFormat().toString());
				} else {
					long num = verify(entryStrmProvider, baseUri, decomp.getFormat());
					if (num > 0) {
						toReturn += num;
					}

					if (!decomp.isDefaultGraph()) {
						dirNamesSeen.add(decomp.getDirName());
					}
				}

				zin.closeEntry();
			}
		}

		if (masterGraph == null) {
			throw new ServletErrorResponseException("Archive has no Master Graph");
		}

		// Verify that all the filenames seen are in the Master Graph, and vice-versa
		Map<String, String> dirToGraphNameMap = new HashMap<>();
		StmtIterator it = masterGraph.listStatements(null,
			ResourceFactory.createProperty(KbGraphStore.GRAPH_DIR_PROPERTY),
			(RDFNode) null);
		try {
			while (it.hasNext()) {
				Statement stmt = it.nextStatement();

				String graphNm = stmt.getSubject().getURI();
				String dirName = stmt.getObject().toString();

				if (!dirNamesSeen.contains(dirName)) {
					throw new ServletErrorResponseException("Master Graph contains a "
						+ "directory name (%1$s) not present in the zip file", dirName);
				}

				dirToGraphNameMap.put(dirName, graphNm);
			}
		} finally {
			if (null != it ) {
				it.close();
			}
		}

		if (dirNamesSeen.size() != dirToGraphNameMap.size()) {

			StringBuilder sb = new StringBuilder();
			boolean firstTime = true;
			for (String dirName : dirNamesSeen) {
				if (!dirToGraphNameMap.containsKey(dirName)) {
					if (!firstTime) {
						sb.append(", ");
					}
					sb.append(dirName);
					firstTime = false;
				}
			}

			throw new ServletErrorResponseException("Mismatch between the number of files "
				+ "in the zip file and the number in the Master Graph.  There are extra directories in the zip file: " + sb.toString());
		}

		// Now that we like the input, we can clear the old repo
		LOG.info("Clearing current repository...");
		ModelManager.inst().clearKb();

		Set<String> indexGraphs = new HashSet<>(dirToGraphNameMap.size());
		it = masterGraph.listStatements(null, RDF.type, ResourceFactory.createResource(KbGraphStore.INDEXED_GRAPH));
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
			InputStream in = inStrmPrvdr.getInputStream();
			final ZipInputStream zin = new ZipInputStream(in);
			) {
			ZipEntry ze = null;
			while ((ze = zin.getNextEntry()) != null) {
				FileNameDecomposition decomp = new FileNameDecomposition(ze.getName());
				// We can assume that decomp.getFormat() is not RDFFormat.UNKNOWN
				// because that was checked in the verification loop above.

				IInputStreamProvider entryStrmProvider = getZipStrmProvider(zin);

				if (decomp.isMasterGraph()) {
					// Do nothing (ignore the Master Graph)
				} else if (decomp.isDefaultGraph()) {
					Model model = ModelManager.inst().getDefaultModel();
					insert(model, "Default Graph", entryStrmProvider, decomp.getFormat(), baseUri);
				} else {
					String graphDir = decomp.getDirName();
					String graphNm = dirToGraphNameMap.get(graphDir);
					Model model = ModelManager.inst().createAndAddNamedModel(graphNm, graphDir, indexGraphs.contains(graphNm));
					insert(model, graphNm, entryStrmProvider, decomp.getFormat(), baseUri);
				}

				zin.closeEntry();
			}
		}

		// Add any KbUnionGraphs
		it = masterGraph.listStatements(null, RDF.type, ResourceFactory.createResource(KbGraphStore.UNION_GRAPH_CLASS));
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
						if (KbGraphStore.LEFT_GRAPH_PROPERTY.equals(stmt.getPredicate().getURI())) {
							leftGraphName = ((Resource)stmt.getObject()).getURI();
						}
						if (KbGraphStore.RIGHT_GRAPH_PROPERTY.equals(stmt.getPredicate().getURI())) {
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

		return toReturn;
	}

	private static class FileNameDecomposition {
		private String _dirName;
		private RDFFormat _format;

		public FileNameDecomposition(String fname) {
			String extension = null;
			_dirName = null;
			int dotIndex = fname.lastIndexOf('.');
			if (dotIndex >= 0 && (dotIndex + 1) < fname.length()) {
				extension = fname.substring(dotIndex + 1);
				_dirName = fname.substring(0, dotIndex);
			}

			_format = RDFFormat.parseExtension(extension);
		}

		public String getDirName() {
			return _dirName;
		}

		public boolean isMasterGraph() {
			return _dirName.equalsIgnoreCase(AbstractHandler.MASTER_GRAPH_BASENAME)
				|| _dirName.equalsIgnoreCase(AbstractHandler.OLD_MASTER_GRAPH_BASENAME);
		}

		public boolean isDefaultGraph() {
			return _dirName.equalsIgnoreCase(AbstractHandler.DEFAULT_GRAPH_BASENAME);
		}

		public RDFFormat getFormat() {
			return _format;
		}
	}

	private static IInputStreamProvider getZipStrmProvider(final ZipInputStream zin) {
		return new IInputStreamProvider() {
			@Override
			public InputStream getInputStream() throws IOException {
				return new FilterInputStream(zin) {
					@Override
					public void close() throws IOException {
						// Do nothing
					}
				};
			}
		};
	}

	/**
	 * Verifies the statements contained in the IInputStreamProvider. If the statements are
	 * not valid, we throw a RuntimeException.
	 *
	 * @return the number of statements in the stream
	 */
	@SuppressWarnings("static-method")
	protected long verify(IInputStreamProvider inStrmPrvdr, String baseUri, RDFFormat format)
		throws IOException {
		long numStmts = 0;
		long start = Calendar.getInstance().getTimeInMillis();
		Model syntaxVerifier = ModelFactory.createModelForGraph(new ForgetfulGraph());
		try (InputStream in = inStrmPrvdr.getInputStream()) {
			if (format == RDFFormat.JSON_LD) {
				syntaxVerifier.setReaderClassName(JsonLdRdfReader.formatName, JsonLdRdfReader.class.getName());
			}
			syntaxVerifier.read(in, baseUri, format.toString());
			numStmts = syntaxVerifier.size();

			if (LOG.isInfoEnabled()) {
				long end = Calendar.getInstance().getTimeInMillis();
				LOG.info(String.format("Verified %1$d statements in %2$.3f seconds",
					numStmts, (end - start) / 1000.0));
			}
		}
		return numStmts;
	}

	/** Inserts the statements from the InputStream into the given Model. */
	@SuppressWarnings("static-method")
	protected void insert(Model model, String graphLabel, IInputStreamProvider inStrmPrvdr,
		RDFFormat format, String baseUri) throws IOException {
		long start = Calendar.getInstance().getTimeInMillis();
		try (InputStream in = inStrmPrvdr.getInputStream()) {
			if (format == RDFFormat.JSON_LD) {
				model.setReaderClassName(JsonLdRdfReader.formatName, JsonLdRdfReader.class.getName());
			}
			model.read(in, baseUri, format.toString());

			if (LOG.isInfoEnabled()) {
				long end = Calendar.getInstance().getTimeInMillis();
				LOG.info(String.format("Added statements to \"%1$s\" in %2$.3f seconds",
					graphLabel, (end - start) / 1000.0));
			}
		}
	}
}
