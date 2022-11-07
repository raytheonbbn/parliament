package com.bbn.parliament.jena.handler;

import java.io.IOException;
import java.time.Instant;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jena.bridge.ConcurrentRequestController;
import com.bbn.parliament.jena.bridge.ConcurrentRequestLock;
import com.bbn.parliament.jena.graph.KbGraph;
import com.bbn.parliament.jena.graph.KbGraphStore;
import com.bbn.parliament.jena.graph.ModelManager;
import com.bbn.parliament.spring_boot.service.AcceptableMediaType;

public class DatasetExportHandler {
	private static final String ZIP_FILENAME_FORMAT = "parliament-export-%1$s-%2$s.zip";
	private static final Logger LOG = LoggerFactory.getLogger(DatasetExportHandler.class);

	@SuppressWarnings("static-method")
	public void handleRequest(HttpServletResponse resp, AcceptableMediaType contentType,
		String serverName) throws IOException {

		try (ConcurrentRequestLock lock = ConcurrentRequestController.getReadLock()) {
			@SuppressWarnings("unused") int intentionallyUnused = lock.hashCode();
			LOG.info("Exporting entire repository to ZIP file in \"{}\" format.", contentType);

			String zipFilename = ZIP_FILENAME_FORMAT.formatted(serverName, Instant.now());
			String extension = contentType.getRdfFormat().getExtension();

			resp.setContentType("application/zip");
			resp.setHeader("Content-Disposition",
				"inline; filename=\"%1$s\";".formatted(zipFilename));

			try (ZipOutputStream zout = new ZipOutputStream(resp.getOutputStream())) {
				// Write the default graph first
				{
					Model model = ModelManager.inst().getDefaultModel();
					String basename = KbGraphStore.DEFAULT_GRAPH_BASENAME;
					String filename = "%1$s.%2$s".formatted(basename, extension);
					zout.putNextEntry(new ZipEntry(filename));
					model.write(zout, contentType.getRdfFormat().toString());
					zout.closeEntry();
				}

				for (String graphName : ModelManager.inst().getSortedModelNames()) {
					Model model = ModelManager.inst().getModel(graphName);
					// Only export IKbGraphs (and not KbUnionGraphs)
					if (model.getGraph() instanceof KbGraph kbGraph) {
						String basename = kbGraph.getRelativeDirectory();
						String filename = "%1$s.%2$s".formatted(basename, extension);
						zout.putNextEntry(new ZipEntry(filename));
						model.write(zout, contentType.getRdfFormat().toString());
						zout.closeEntry();
					}
				}

				zout.finish();
			}
		}
	}
}
