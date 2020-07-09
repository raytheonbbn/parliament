// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.handler;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.joseki.Joseki;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jena.graph.KbGraph;
import com.bbn.parliament.jena.graph.ModelManager;
import com.bbn.parliament.jena.joseki.bridge.ActionRouter;
import com.bbn.parliament.jena.joseki.bridge.servlet.ServletErrorResponseException;
import com.bbn.parliament.jena.joseki.bridge.util.HttpServerUtil;
import com.bbn.parliament.jena.joseki.client.RDFFormat;
import com.hp.hpl.jena.rdf.model.Model;

/** @author sallen */
public class ExportHandler extends AbstractHandler {
	private static final String P_FORMAT = "dataFormat";
	private static final String P_GRAPH = "graph";
	private static final String P_EXPORT_ALL = "exportAll";
	private static final String[] DOS_DEVICE_NAMES = { "AUX", "CLOCK$", "COM1",
		"COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9", "CON",
		"LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9",
		"NUL", "PRN" };
	private static final char[] INVALID_URL_CHARS = { '*', ':', '<', '>', '?',
		'\\', '/', '"', '|' };

	// %1 = hostname, %2 = datetime
	private static final String ZIP_FILENAME_FORMAT = "parliament-export-%1$s-%2$tY%2$tm%2$td-%2$tH%2$tM%2$tS.zip";

	// Use one logger.
	private static final Logger LOG = LoggerFactory.getLogger(ExportHandler.class);

	/*
	 * (non-Javadoc)
	 * @see com.bbn.parliament.jena.joseki.josekibridge.AbstractHandler#getLog()
	 */
	@Override
	protected Logger getLog() {
		return LOG;
	}

	/*
	 * (non-Javadoc)
	 * @see com.bbn.parliament.jena.joseki.josekibridge.AbstractHandler#handleFormURLEncodedRequest(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void handleFormURLEncodedRequest(HttpServletRequest req,
		HttpServletResponse resp) throws IOException, ServletErrorResponseException {
		String graphName = HttpServerUtil.getParameter(req, P_GRAPH, "");
		String dataFormat = HttpServerUtil.getParameter(req, P_FORMAT, "RDF/XML");
		String exportAll = HttpServerUtil.getParameter(req, P_EXPORT_ALL, "no");

		handleRequest(req, resp, graphName, dataFormat, exportAll);
	}

	/*
	 * (non-Javadoc)
	 * @see com.bbn.parliament.jena.joseki.josekibridge.AbstractHandler#handleMultipartFormRequest(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void handleMultipartFormRequest(HttpServletRequest req,
		HttpServletResponse resp) throws ServletErrorResponseException {
		throw new ServletErrorResponseException("'multipart/form data' requests are not "
			+ "supported by this handler.");
	}

	protected void handleRequest(HttpServletRequest req, HttpServletResponse resp,
		String graphName, String dataFormat, String exportAllStr)
			throws IOException, ServletErrorResponseException {
		// Default to false
		boolean exportAll = "yes".equalsIgnoreCase(exportAllStr);

		RDFFormat format = RDFFormat.parse(dataFormat);
		if (format == RDFFormat.UNKNOWN) {
			throw new ServletErrorResponseException("Unsupported data format \"%1$s\"", dataFormat);
		}

		ActionRouter.getReadLock();
		try {
			if (exportAll) {
				writeResponse(req, resp, format);
			}
			else {
				boolean isDefaultGraph = "".equals(graphName);
				Model model = isDefaultGraph ? ModelManager.inst().getDefaultModel() : ModelManager.inst().getModel(graphName);

				if (null == model) {
					throw new ServletErrorResponseException(
						"There was no named graph with name \"%1$s\"", graphName);
				}
				else {
					writeResponse(req, resp, model, graphName, format);
				}

				LOG.info("Export finished!");
			}
		} finally {
			ActionRouter.releaseReadLock();
		}
	}

	/**
	 * Exports a single named graph.
	 *
	 * @param resp The HttpServletResponse to respond on.
	 * @param model The graph to export.
	 * @param graphName The name of the graph to export.
	 * @param dataFormat The RDF serialization format.
	 */
	protected void writeResponse(HttpServletRequest req, HttpServletResponse resp,
		Model model, String graphName, RDFFormat dataFormat) throws IOException {

		String graphLabel = (graphName.length() == 0) ? DEFAULT_GRAPH_BASENAME : graphName;

		LOG.info("Exporting <{}> in \"{}\" format.", graphLabel, dataFormat);

		String basename = encodeUriForFilename(graphLabel);
		String extension = dataFormat.getExtension();
		String filename = String.format("%1$s.%2$s", basename, extension);

		String writerMimeType = "text/plain";
		switch (dataFormat) {
		case N3:
			writerMimeType = Joseki.contentTypeN3;
			break;
		case TURTLE:
			writerMimeType = Joseki.contentTypeTurtle;
			break;
		case NTRIPLES:
			writerMimeType = Joseki.contentTypeNTriples;
			break;
		case RDFXML:
			writerMimeType = Joseki.contentTypeRDFXML;
			break;
		case UNKNOWN:
		default:
			// Do nothing
			break;
		}

		resp.setContentType(writerMimeType);
		resp.setHeader("Content-Disposition",
			String.format("inline; filename=\"%1$s\";", filename));

		@SuppressWarnings("resource")
		ServletOutputStream out = resp.getOutputStream();
		writeModel(out, model, dataFormat);
		out.flush();
	}

	/**
	 * Exports a set of graphs in a zip archive.
	 *
	 * @param resp The HttpServletResponse to respond on.
	 * @param dataFormat The RDF serialization format.
	 */
	protected void writeResponse(HttpServletRequest req, HttpServletResponse resp,
		RDFFormat dataFormat) throws IOException {
		LOG.info("Exporting entire repository to ZIP file in \"{}\" format.", dataFormat);

		String hostname = req.getServerName();
		String zipFilename = String.format(ZIP_FILENAME_FORMAT, hostname,
			Calendar.getInstance());
		String extension = dataFormat.getExtension();

		resp.setContentType("application/zip");
		resp.setHeader("Content-Disposition",
			String.format("inline; filename=\"%1$s\";", zipFilename));

		try (ZipOutputStream zout = new ZipOutputStream(resp.getOutputStream())) {
			// Write the default graph first
			{
				Model model = ModelManager.inst().getDefaultModel();
				String basename = DEFAULT_GRAPH_BASENAME;
				String filename = String.format("%1$s.%2$s", basename, extension);
				zout.putNextEntry(new ZipEntry(filename));
				writeModel(zout, model, dataFormat);
				zout.closeEntry();
			}

			for (String graphName : ModelManager.inst().getSortedModelNames()) {
				Model model = ModelManager.inst().getModel(graphName);
				// Only export IKbGraphs (and not KbUnionGraphs)
				if (model.getGraph() instanceof KbGraph) {
					String basename = ((KbGraph) model.getGraph()).getRelativeDirectory();
					String filename = String.format("%1$s.%2$s", basename, extension);
					zout.putNextEntry(new ZipEntry(filename));
					writeModel(zout, model, dataFormat);
					zout.closeEntry();
				}
			}

			zout.finish();
		}
	}

	@SuppressWarnings("static-method")
	protected void writeModel(OutputStream out, Model model, RDFFormat dataFormat) {
		model.write(out, dataFormat.toString());
	}

	/**
	 * Encodes a URI into a valid file name.  We replace any invalid characters
	 * with an underscore.
	 *
	 * @param uri URI to encode.
	 * @return A string that can be used as a valid filename.
	 */
	public static String encodeUriForFilename(String uri) {
		// Some helpful comments here:
		// http://stackoverflow.com/questions/62771/how-check-if-given-string-is-legal-allowed-file-name-under-windows

		// These DOS device names are not allowed
		for (String dos : DOS_DEVICE_NAMES) {
			if (uri.trim().equalsIgnoreCase(dos)) {
				return "_" + uri.trim();
			}
		}

		StringBuffer sb = new StringBuffer(uri.length());
		for (int i = 0; i < uri.length(); ++i) {
			char c = uri.charAt(i);

			boolean isInvalidChar = false;
			for (char invalidChar : INVALID_URL_CHARS) {
				if (c == invalidChar) {
					isInvalidChar = true;
					break;
				}
			}

			int cInt = c;
			if ((cInt >= 0x0 && cInt <= 0x1F) || isInvalidChar) {
				//sb.append("%" + NTriplesUtil.toHexString(cInt, 2));
				sb.append('_');
			} else {
				sb.append(c);
			}
		}

		return sb.toString();
	}
}
