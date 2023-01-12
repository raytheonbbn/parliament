// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.handler;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jena.bridge.ConcurrentRequestController;
import com.bbn.parliament.jena.bridge.ConcurrentRequestLock;
import com.bbn.parliament.jena.exception.MissingGraphException;
import com.bbn.parliament.jena.graph.KbGraphStore;
import com.bbn.parliament.jena.graph.ModelManager;
import com.bbn.parliament.jena.util.FileUtil;
import com.bbn.parliament.spring_boot.service.AcceptableMediaType;

/** @author sallen */
public class GraphExportHandler {
	private static final Logger LOG = LoggerFactory.getLogger(GraphExportHandler.class);

	private final AcceptableMediaType contentType;
	private final String graphName;
	private final String fileName;

	public GraphExportHandler(AcceptableMediaType contentType, String serverName, String graphName) {
		this.contentType = Objects.requireNonNull(contentType, "contentType");
		this.graphName = (graphName == null || graphName.isEmpty())
			? KbGraphStore.DEFAULT_GRAPH_BASENAME
			: graphName;
		this.fileName = "%1$s-%2$s.%3$s".formatted(
			serverName,
			FileUtil.encodeStringForFilename(this.graphName),
			this.contentType.getRdfFormat().getExtension());
	}

	public String getContentDisposition() {
		return "inline; filename=\"%1$s\";".formatted(fileName);
	}

	public void handleRequest(OutputStream out) throws IOException {
		try (ConcurrentRequestLock lock = ConcurrentRequestController.getReadLock()) {
			@SuppressWarnings("unused") int intentionallyUnused = lock.hashCode();
			Model model = graphName.equals(KbGraphStore.DEFAULT_GRAPH_BASENAME)
				? ModelManager.inst().getDefaultModel()
				: ModelManager.inst().getModel(graphName);
			if (null == model) {
				throw new IOException(
					new MissingGraphException("Named graph <%1$s> does not exist", graphName));
			}

			LOG.info("Exporting <{}> as {}", graphName, contentType);

			model.write(out, contentType.getRdfFormat().toString());

			LOG.info("Export/OK");
		}
	}
}
