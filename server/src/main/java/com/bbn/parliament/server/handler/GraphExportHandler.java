// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.server.handler;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar; //LINE ADDED BY CODEX CODING AGENT
import java.util.Objects;
import java.util.zip.ZipEntry; //LINE ADDED BY CODEX CODING AGENT
import java.util.zip.ZipOutputStream; //LINE ADDED BY CODEX CODING AGENT

import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.kb_graph.KbGraph; //LINE ADDED BY CODEX CODING AGENT
import com.bbn.parliament.kb_graph.KbGraphStore;
import com.bbn.parliament.kb_graph.util.FileUtil;
import com.bbn.parliament.server.exception.MissingGraphException;
import com.bbn.parliament.server.graph.ModelManager;
import com.bbn.parliament.server.service.AcceptableMediaType;
import com.bbn.parliament.server.util.ConcurrentRequestController;
import com.bbn.parliament.server.util.ConcurrentRequestLock;

/** @author sallen */
//CLASS UPDATED BY CODEX CODING AGENT
public class GraphExportHandler {
	// %1 = hostname, %2 = datetime
	private static final String ZIP_FILENAME_FORMAT = "parliament-export-%1$s-%2$tY%2$tm%2$td-%2$tH%2$tM%2$tS.zip";
	private static final Logger LOG = LoggerFactory.getLogger(GraphExportHandler.class);

	private final AcceptableMediaType contentType;
	private final String graphName;
	private final String fileName;
	private final boolean exportAll;

	//FUNCTION UPDATED BY CODEX CODING AGENT
	public GraphExportHandler(AcceptableMediaType contentType, String serverName, String graphName) {
		this(contentType, serverName, graphName, false);
	}

	//FUNCTION UPDATED BY CODEX CODING AGENT
	public GraphExportHandler(AcceptableMediaType contentType, String serverName,
		String graphName, boolean useLegacyFilename) {
		this.contentType = Objects.requireNonNull(contentType, "contentType");
		this.graphName = (graphName == null || graphName.isEmpty())
			? KbGraphStore.DEFAULT_GRAPH_BASENAME
			: graphName;
		String basename = FileUtil.encodeStringForFilename(this.graphName);
		this.fileName = useLegacyFilename
			? "%1$s.%2$s".formatted(basename, this.contentType.getPrimaryFileExtension())
			: "%1$s-%2$s.%3$s".formatted(
				serverName, basename, this.contentType.getPrimaryFileExtension());
		this.exportAll = false;
	}

	//FUNCTION ADDED BY CODEX CODING AGENT
	public GraphExportHandler(AcceptableMediaType contentType, String serverName) {
		this.contentType = Objects.requireNonNull(contentType, "contentType");
		this.graphName = null;
		this.fileName = ZIP_FILENAME_FORMAT.formatted(serverName, Calendar.getInstance());
		this.exportAll = true;
	}

	public String getContentDisposition() {
		return "inline; filename=\"%1$s\";".formatted(fileName);
	}

	//FUNCTION UPDATED BY CODEX CODING AGENT
	public void handleRequest(OutputStream out) throws IOException {
		try (ConcurrentRequestLock lock = ConcurrentRequestController.getReadLock()) {
			@SuppressWarnings("unused") int intentionallyUnused = lock.hashCode();
			if (exportAll) {
				exportAll(out);
			} else {
				exportGraph(out);
			}
		}
	}

	//FUNCTION ADDED BY CODEX CODING AGENT
	private void exportGraph(OutputStream out) throws IOException {
		Model model = graphName.equals(KbGraphStore.DEFAULT_GRAPH_BASENAME)
			? ModelManager.inst().getDefaultModel()
			: ModelManager.inst().getModel(graphName);
		if (null == model) {
			throw new IOException(
				new MissingGraphException("Named graph <%1$s> does not exist", graphName));
		}

		LOG.info("Exporting <{}> as {}", graphName, contentType);

		model.write(out, contentType.getRdfLang().getName());

		LOG.info("Export/OK");
	}

	//FUNCTION ADDED BY CODEX CODING AGENT
	private void exportAll(OutputStream out) throws IOException {
		LOG.info("Exporting entire repository to ZIP file as {}", contentType);

		String extension = contentType.getPrimaryFileExtension();
		try (ZipOutputStream zout = new ZipOutputStream(out)) {
			Model defaultModel = ModelManager.inst().getDefaultModel();
			writeZipEntry(zout, defaultModel, KbGraphStore.DEFAULT_GRAPH_BASENAME, extension);

			for (String currentGraphName : ModelManager.inst().getSortedModelNames()) {
				Model model = ModelManager.inst().getModel(currentGraphName);
				if (model.getGraph() instanceof KbGraph kbGraph) {
					writeZipEntry(zout, model, kbGraph.getRelativeDirectory(), extension);
				}
			}

			zout.finish();
		}

		LOG.info("Repository export/OK");
	}

	//FUNCTION ADDED BY CODEX CODING AGENT
	private void writeZipEntry(ZipOutputStream zout, Model model, String basename,
		String extension) throws IOException {
		String filename = "%1$s.%2$s".formatted(basename, extension);
		zout.putNextEntry(new ZipEntry(filename));
		model.write(zout, contentType.getRdfLang().getName());
		zout.closeEntry();
	}
}
