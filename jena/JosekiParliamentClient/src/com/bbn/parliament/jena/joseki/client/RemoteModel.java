// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.joseki.client;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 * @author dkolas
 * @author sallen
 */
public class RemoteModel {
	/** %1 is the host name, %2 is the port */
	public static final String DEFAULT_SPARQL_ENDPOINT_URL = "http://%1$s:%2$s/parliament/sparql";
	/** %1 is the host name, %2 is the port */
	public static final String DEFAULT_UPDATE_ENDPOINT_URL = "http://%1$s:%2$s/parliament/update";
	/** %1 is the host name, %2 is the port */
	public static final String DEFAULT_BULK_ENDPOINT_URL = "http://%1$s:%2$s/parliament/bulk";

	public static final String PARLIAMENT_NS = "http://parliament.semwebcentral.org/parliament#";
	public static final String RESULT_INDIVIDUAL = PARLIAMENT_NS + "Result";
	public static final String ERROR_CLASS = PARLIAMENT_NS + "Error";
	public static final String EXCEPTION_TRACE_PROPERTY = PARLIAMENT_NS + "exceptionTrace";

	protected static final String P_CLEAR_ALL = "clearAll";
	protected static final String P_GRAPH = "graph";
	protected static final String P_PERFORM_CLEAR = "performClear";
	protected static final String P_VERIFY = "verifyData";

	private static final int BUFFER_SIZE = 16 * 1024;

	private static final Pattern INSERT_RESULT_PATTERN = Pattern.compile(
		"Insert operation successful\\.  ([0-9]+) statements added", Pattern.CASE_INSENSITIVE);

	private String _sparqlEndpointUrl;
	private String _bulkEndpointUrl;
	private int _bufferSize = 2048;
	private Map<String, Object> _defaultParams;

	/**
	 * Create a new RemoteModel, pointing to an enhanced Joseki endpoint.
	 *
	 * @param sparqlEndpointUrl The SPARQL endpoint URL (usually "http://host/parliament/sparql").
	 * @param bulkEndpointUrl The Bulk endpoint URL (usually "http://host/parliament/bulk").
	 * @param defaultParams A set of HTTP form parameters that are added to each server request
	 */
	public RemoteModel(String sparqlEndpointUrl, String bulkEndpointUrl, Map<String, Object> defaultParams) {
		this._sparqlEndpointUrl = sparqlEndpointUrl;
		this._bulkEndpointUrl = bulkEndpointUrl;
		this._defaultParams = new HashMap<>(defaultParams);
	}

	/**
	 * Create a new RemoteModel, pointing to an enhanced Joseki endpoint.
	 *
	 * @param sparqlEndpointUrl The SPARQL endpoint URL (usually "http://host/parliament/sparql").
	 * @param bulkEndpointUrl The Bulk endpoint URL (usually "http://host/parliament/bulk").
	 */
	public RemoteModel(String sparqlEndpointUrl, String bulkEndpointUrl) {
		this._sparqlEndpointUrl = sparqlEndpointUrl;
		this._bulkEndpointUrl = bulkEndpointUrl;
		this._defaultParams = Collections.emptyMap();
	}


	/**
	 * Issue update query to the remote KB
	 *
	 * @param updateQuery A SPARQL/Update query string
	 */
	public void updateQuery(String updateQuery) throws IOException
	{
		Map<String, Object> params = new HashMap<>();
		params.putAll(_defaultParams);
		params.put("update", updateQuery);
		try (InputStream respStrm = sendRequest(params)) {
			// Do nothing -- there is no response
		}
	}

	/**
	 * Issue update query to the remote KB
	 *
	 * @param updateQuery A SPARQL/Update query object
	 */
	public void updateQuery(Query updateQuery) throws IOException
	{
		updateQuery(updateQuery.toString());
	}

	/**
	 * Execute a select query on the remote repository.
	 *
	 * @param selectQuery the query to execute
	 * @return the ResultSet answer.
	 */
	@SuppressWarnings("resource")
	public ResultSet selectQuery(String selectQuery) throws IOException {
		Map<String, Object> params = new HashMap<>();
		params.putAll(_defaultParams);
		params.put("query", selectQuery);
		InputStream results = sendRequest(params);
		return ResultSetFactory.fromXML(results);
	}

	/**
	 * Execute a select query on the remote repository.
	 *
	 * @param selectQuery the query to execute
	 * @return the ResultSet answer.
	 */
	public ResultSet selectQuery(Query selectQuery) throws IOException {
		return selectQuery(selectQuery.toString());
	}

	/**
	 * Execute an ask query on the remote repository.
	 *
	 * @param askQuery The query to execute, in compiled Jena form
	 * @return the boolean answer to the query
	 */
	public boolean askQuery(Query askQuery) throws IOException {
		return askQuery(askQuery.toString());
	}

	/**
	 * Execute an ask query on the remote repository.
	 *
	 * @param askQuery The query to execute, in String form
	 * @return the boolean answer to the query
	 */
	public boolean askQuery(String askQuery) throws IOException {
		Map<String, Object> params = new HashMap<>();
		params.putAll(_defaultParams);
		params.put("query", askQuery);
		boolean result = false;
		try (
			InputStream is = sendRequest(params);
			Reader isrdr = new InputStreamReader(is, StandardCharsets.UTF_8);
			BufferedReader bufferedReader = new BufferedReader(isrdr);
		) {
			String line = null;
			while ((line = bufferedReader.readLine()) != null) {
				int i = line.indexOf("<boolean>");
				if (line.length() > i + 12 && line.substring(i + 9, i + 13).equals("true")) {
					result = true;
				}
			}
		}
		return result;
	}

	/**
	 * Execute a construct query on the remote repository.
	 *
	 * @param constructQuery the query to execute
	 * @return the Model answer.
	 */
	public Model constructQuery(String constructQuery) throws IOException {
		Map<String, Object> params = new HashMap<>();
		params.putAll(_defaultParams);
		params.put("query", constructQuery);
		Model model = ModelFactory.createDefaultModel();
		try (InputStream respStrm = sendRequest(params)) {
			model.read(respStrm, "");
		}
		return model;
	}

	/**
	 * Execute a construct query on the remote repository.
	 *
	 * @param constructQuery the query to execute
	 * @return the Model answer.
	 */
	public Model constructQuery(Query constructQuery) throws IOException {
		return constructQuery(constructQuery.toString());
	}

	/**
	 * Insert data into the default graph of the remote KB.
	 *
	 * @param serializedStatements The statements in serialized form
	 * @param format Format of the data.
	 * @param base The base uri to be used when converting relative URI's to absolute URI's.
	 * The base URI may be null if there are no relative URIs to convert.
	 * @param verifyData Whether or not to verify that the data is valid before inserting statements
	 * @return The number of statements inserted
	 */
	public long insertStatements(String serializedStatements, RDFFormat format, String base,
		boolean verifyData) throws IOException {
		return internalInsertStatements(serializedStatements, format, base, "", verifyData);
	}

	/**
	 * Insert data into the default graph of the remote KB.
	 *
	 * @param serializedStatements The statements in serialized form
	 * @param format Format of the data.
	 * @param base The base uri to be used when converting relative URI's to absolute URI's.
	 * The base URI may be null if there are no relative URIs to convert.
	 * @param verifyData Whether or not to verify that the data is valid before inserting statements
	 * @return The number of statements inserted
	 */
	public long insertStatements(String serializedStatements, String format, String base,
		boolean verifyData) throws IOException {
		return internalInsertStatements(serializedStatements,
			RDFFormat.parseJenaFormatString(format), base, "", verifyData);
	}

	/**
	 * Insert data into the remote KB in the specified named graph.
	 *
	 * @param serializedStatements The statements in serialized form
	 * @param format Format of the data.
	 * @param base The base uri to be used when converting relative URI's to absolute URI's.
	 * The base URI may be null if there are no relative URIs to convert.
	 * @param namedGraphURI The uri of the graph to insert into
	 * @param verifyData Whether or not to verify that the data is valid before inserting statements
	 * @return The number of statements inserted
	 */
	public long insertStatements(String serializedStatements, RDFFormat format, String base,
		String namedGraphURI, boolean verifyData) throws IOException {
		return internalInsertStatements(serializedStatements, format, base, namedGraphURI, verifyData);
	}

	/**
	 * Insert data into the remote KB in the specified named graph.
	 *
	 * @param serializedStatements The statements in serialized form
	 * @param format Format of the data.
	 * @param base The base uri to be used when converting relative URI's to absolute URI's.
	 * The base URI may be null if there are no relative URIs to convert.
	 * @param namedGraphURI The uri of the graph to insert into
	 * @param verifyData Whether or not to verify that the data is valid before inserting statements
	 * @return The number of statements inserted
	 */
	public long insertStatements(String serializedStatements, String format, String base,
		String namedGraphURI, boolean verifyData) throws IOException {
		return internalInsertStatements(serializedStatements,
			RDFFormat.parseJenaFormatString(format), base, namedGraphURI, verifyData);
	}

	/**
	 * Insert data into the default graph of the remote KB.
	 *
	 * @param serializedStatements Input stream containing the statements in
	 *        serialized form
	 * @param format Format of the data.
	 * @param base The base uri to be used when converting relative URI's to absolute URI's.
	 * The base URI may be null if there are no relative URIs to convert.
	 * @param verifyData Whether or not to verify that the data is valid before inserting statements
	 * @return The number of statements inserted
	 */
	public long insertStatements(InputStream serializedStatements, RDFFormat format,
		String base, boolean verifyData) throws IOException {
		return internalInsertStatements(serializedStatements, format, base, "", verifyData);
	}

	/**
	 * Insert data into the default graph of the remote KB.
	 *
	 * @param serializedStatements Input stream containing the statements in
	 *        serialized form
	 * @param format Format of the data.
	 * @param base The base uri to be used when converting relative URI's to absolute URI's.
	 * The base URI may be null if there are no relative URIs to convert.
	 * @param verifyData Whether or not to verify that the data is valid before inserting statements
	 * @return The number of statements inserted
	 */
	public long insertStatements(InputStream serializedStatements, String format,
		String base, boolean verifyData) throws IOException {
		return internalInsertStatements(serializedStatements,
			RDFFormat.parseJenaFormatString(format), base, "", verifyData);
	}

	/**
	 * Insert data into the remote KB in the specified named graph.
	 *
	 * @param serializedStatements Input stream containing the statements in
	 * serialized form
	 * @param base The base uri to be used when converting relative URI's to absolute URI's.
	 * The base URI may be null if there are no relative URIs to convert.
	 * @param format Format of the data.
	 * @param namedGraphURI The uri of the graph to insert into
	 * @param verifyData Whether or not to verify that the data is valid before inserting statements
	 * @return The number of statements inserted
	 */
	public long insertStatements(InputStream serializedStatements, RDFFormat format,
		String base, String namedGraphURI, boolean verifyData) throws IOException {
		return internalInsertStatements(serializedStatements, format, base, namedGraphURI, verifyData);
	}

	/**
	 * Insert data into the remote KB in the specified named graph.
	 *
	 * @param serializedStatements Input stream containing the statements in
	 * serialized form
	 * @param base The base uri to be used when converting relative URI's to absolute URI's.
	 * The base URI may be null if there are no relative URIs to convert.
	 * @param format Format of the data.
	 * @param namedGraphURI The uri of the graph to insert into
	 * @param verifyData Whether or not to verify that the data is valid before inserting statements
	 * @return The number of statements inserted
	 */
	public long insertStatements(InputStream serializedStatements, String format,
		String base, String namedGraphURI, boolean verifyData) throws IOException {
		return internalInsertStatements(serializedStatements,
			RDFFormat.parseJenaFormatString(format), base, namedGraphURI, verifyData);
	}

	/**
	 * Insert data into the default graph of the remote KB.
	 *
	 * @param model A Jena Model containing the statements to be inserted.
	 * @return The number of statements inserted
	 */
	public long insertStatements(Model model) throws IOException {
		return insertStatements(model, "");
	}

	/**
	 * Insert data into the remote KB in the specified named graph.
	 *
	 * @param model A Jena Model containing the statements to be inserted.
	 * @param namedGraphURI The uri of the graph to insert into
	 * @return The number of statements inserted
	 */
	public long insertStatements(final Model model, String namedGraphURI) throws IOException {
		RDFFormat format = RDFFormat.NTRIPLES;
		// Pipes can be tricky.  Both in and out must be created in the main thread to prevent
		// the main thread reading before the pipe has been connected.  Also, the worker thread must
		// close the PipedOutputStream before it exits, else the PipedInputStream will throw.
		try (
			PipedInputStream pipeExtractor = new PipedInputStream(16 * 1024);
			PipedOutputStream pipeInserter = new PipedOutputStream(pipeExtractor);
		) {
			Thread t = new Thread(() -> {
					try (OutputStream os = pipeInserter) {	// ensure closure
						model.write(os, format.toString());
						os.flush();	// not really necessary, since the buffer is in the PipedInputStream
					} catch (Throwable ex) {
						System.err.format("Pipe error %1$s:  %2$s%n", ex.getClass().getSimpleName(), ex.getMessage());
						ex.printStackTrace();
					}
				});
			t.setDaemon(true);
			t.start();
			return internalInsertStatements(pipeExtractor, format, null, namedGraphURI, false);
		}
	}

	/**
	 * Insert data into the remote KB in the specified named graph.
	 *
	 * @param serializedStatements The statements in serialized form.  This should
	 * be either a String or an InputStream.
	 * @param format Format of the data.
	 * @param base The base uri to be used when converting relative URI's to absolute URI's.
	 * The base URI may be null if there are no relative URIs to convert.
	 * @param namedGraphURI The uri of the graph to insert into
	 * @return The number of statements inserted
	 */
	protected long internalInsertStatements(Object serializedStatements, RDFFormat format,
		String base, String namedGraphURI, boolean verifyData) throws IOException {
		Map<String, Object> params = new HashMap<>();
		if (format != null) {
			params.put("dataFormat", format);
		}
		if (base != null) {
			params.put("base", base);
		}
		if (namedGraphURI != null && !namedGraphURI.isEmpty()) {
			params.put("graph", namedGraphURI);
		}
		params.put(P_VERIFY, verifyData ? "yes" : "no");
		params.put("statements", serializedStatements);

		byte[] buffer = new byte[BUFFER_SIZE];
		try (
			InputStream respStrm = sendBulkRequest(params, "insert", true);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
		) {
			for (;;) {
				int bytesRead = respStrm.read(buffer, 0, buffer.length);
				if (bytesRead < 0) {
					break;
				} else if (bytesRead > 0) {
					baos.write(buffer, 0, bytesRead);
				}
			}
			String responseBody =  baos.toString("UTF-8");
			Matcher m = INSERT_RESULT_PATTERN.matcher(responseBody);
			long result = -1;
			if (m.find()) {
				String numStmtsStr = m.group(1);
				result = Long.parseLong(numStmtsStr);
			}
			return result;
		}
	}

	/**
	 * Delete data from the default graph in the remote KB.
	 *
	 * @param serializedStatements The statements in serialized String form
	 * @param format Format of the data.
	 */
	public void deleteStatements(String serializedStatements, RDFFormat format)
		throws IOException {
		deleteStatements(serializedStatements, format, "");
	}

	/**
	 * Delete data from the default graph in the remote KB.
	 *
	 * @param serializedStatements The statements in serialized String form
	 * @param format Format of the data.
	 */
	public void deleteStatements(String serializedStatements, String format)
		throws IOException {
		deleteStatements(serializedStatements, RDFFormat.parseJenaFormatString(format), "");
	}

	/**
	 * Delete data from the default graph in the remote KB.
	 *
	 * @param serializedStatements Input stream containing the statements in
	 *        serialized form
	 * @param format Format of the data.
	 */
	public void deleteStatements(InputStream serializedStatements, RDFFormat format)
		throws IOException {
		deleteStatements(serializedStatements, format, "");
	}

	/**
	 * Delete data from the default graph in the remote KB.
	 *
	 * @param serializedStatements Input stream containing the statements in
	 *        serialized form
	 * @param format Format of the data.
	 */
	public void deleteStatements(InputStream serializedStatements, String format)
		throws IOException {
		deleteStatements(serializedStatements, RDFFormat.parseJenaFormatString(format), "");
	}

	/**
	 * Delete data from the remote KB from the specified named graph.
	 *
	 * @param serializedStatements The statements in serialized form
	 * @param format Format of the data.
	 * @param namedGraphURI The URI of the graph to delete from
	 */
	public void deleteStatements(String serializedStatements, RDFFormat format,
		String namedGraphURI) throws IOException {
		InputStream in = new ByteArrayInputStream(serializedStatements.getBytes());
		deleteStatements(in, format, namedGraphURI);
	}

	/**
	 * Delete data from the remote KB from the specified named graph.
	 *
	 * @param serializedStatements The statements in serialized form
	 * @param format Format of the data.
	 * @param namedGraphURI The URI of the graph to delete from
	 */
	public void deleteStatements(String serializedStatements, String format,
		String namedGraphURI) throws IOException {
		deleteStatements(serializedStatements, RDFFormat.parseJenaFormatString(format),
			namedGraphURI);
	}

	/**
	 * Delete data from the remote KB from the specified named graph.
	 *
	 * @param serializedStatements Input stream containing the statements in
	 *        serialized form
	 * @param format Format of the data.
	 * @param namedGraphURI The uri of the graph to delete from
	 */
	public void deleteStatements(InputStream serializedStatements, RDFFormat format,
		String namedGraphURI) throws IOException {
		Model tempModel = ModelFactory.createDefaultModel();
		tempModel.read(serializedStatements, "", format.toString());
		deleteStatements(tempModel, namedGraphURI);
	}

	/**
	 * Delete data from the remote KB from the specified named graph.
	 *
	 * @param serializedStatements Input stream containing the statements in
	 *        serialized form
	 * @param format Format of the data.
	 * @param namedGraphURI The uri of the graph to delete from
	 */
	public void deleteStatements(InputStream serializedStatements, String format,
		String namedGraphURI) throws IOException {
		deleteStatements(serializedStatements, RDFFormat.parseJenaFormatString(format),
			namedGraphURI);
	}

	/**
	 * Delete data from the default graph in the remote KB.
	 *
	 * @param model A Jena Model containing the statements to be deleted.
	 */
	public void deleteStatements(Model model) throws IOException {
		deleteStatements(model, "");
	}

	/**
	 * Delete data from the remote KB from the specified named graph.
	 *
	 * @param model A Jena Model containing the statements to be deleted.
	 * @param namedGraphURI The uri of the graph to delete from
	 */
	public void deleteStatements(Model model, String namedGraphURI) throws IOException {
		StringWriter wtr = new StringWriter();

		wtr.write("delete data { ");
		if (namedGraphURI != null && !namedGraphURI.isEmpty()) {
			wtr.write("graph <");
			wtr.write(namedGraphURI);
			wtr.write("> { ");
		}
		model.write(wtr, RDFFormat.NTRIPLES.toString());
		if (namedGraphURI != null && !namedGraphURI.isEmpty()) {
			wtr.write(" }");
		}
		wtr.write(" }");

		Map<String, Object> params = new HashMap<>();
		params.put("update", wtr.toString());
		try (InputStream respStrm = sendRequest(params)) {
			// Do nothing -- there is no response
		}
	}

	/**
	 * Create a new named graph in the repository.
	 *
	 * @param namedGraphURI The uri of the named graph to create
	 */
	public void createNamedGraph(String namedGraphURI) throws IOException {
		createNamedGraph(namedGraphURI, false);
	}

	/**
	 * Create a new named graph in the repository.
	 *
	 * @param namedGraphURI The uri of the named graph to create
	 * @param silent The SPARQL/Update service generates an error if the graph referred by
	 * the URI already exists unless the SILENT is set to true, then no error is generated
	 * and execution of the sequence of SPARQL/Update operations continues.
	 */
	public void createNamedGraph(String namedGraphURI, boolean silent) throws IOException {
		String query = String.format("create %1$s graph <%2$s>", silent ? "silent" : "", namedGraphURI);
		this.updateQuery(query);
	}

	/**
	 * Drops the specified named graph from the repository (this can handle dropping Union graphs as well).
	 *
	 * @param namedGraphURI The uri of the named graph to drop
	 */
	public void dropNamedGraph(String namedGraphURI) throws IOException {
		dropNamedGraph(namedGraphURI, false);
	}

	/**
	 * Drops the specified named graph from the repository (this can handle dropping Union graphs as well).
	 *
	 * @param namedGraphURI The uri of the named graph to drop
	 * @param silent The SPARQL/Update service, by default, is expected to generate an error
	 * if the specified named graph does not exist. If SILENT is true, this error is ignored
	 * and execution of a sequence of SPARQL/Update operations continues.
	 */
	public void dropNamedGraph(String namedGraphURI, boolean silent) throws IOException {
		String query = String.format("drop %1$s graph <%2$s>", silent ? "silent" : "", namedGraphURI);
		this.updateQuery(query);
	}

	/**
	 * Create a new Union named graph in the repository.
	 *
	 * @param namedUnionGraphURI The uri of the named graph to create
	 * @param leftGraphURI The first named graph in the union
	 * @param rightGraphURI The second named graph in the union
	 */
	public void createNamedUnionGraph(String namedUnionGraphURI,
		String leftGraphURI, String rightGraphURI) throws IOException {

		String query = """
			prefix parPF: <java:com.bbn.parliament.jena.pfunction.>
			insert {} where {
				<%1$s> parPF:createUnionGraph ( <%2$s> <%3$s> ) .
			}
			""".formatted(namedUnionGraphURI, leftGraphURI, rightGraphURI);
		this.updateQuery(query);
	}

	/**
	 * Get an iterator of the available named graphs within the system.
	 *
	 * @return an Iterator of Strings representing uris for the named graphs
	 */
	public Iterator<String> getAvailableNamedGraphs() throws IOException {
		String query = "SELECT ?g WHERE { GRAPH ?g { } }";

		ResultSet results = selectQuery(query);
		return new NamedGraphResults("g", results);
	}

	/**
	 * Clears all of the statements in the given named graph.
	 * Note: does not remove the named graph itself.
	 *
	 * @param namedGraphURI URI of the named graph to clear
	 */
	public void clear(String namedGraphURI) throws IOException {
		Map<String, Object> params = new HashMap<>();
		params.put(P_GRAPH, namedGraphURI);
		params.put(P_PERFORM_CLEAR, "yes");

		try (InputStream respStrm = sendBulkRequest(params, "clear", false)) {
			// Do nothing -- there is no response
		}
	}

	/** Clears the entire repository. */
	public void clearAll() throws IOException {
		Map<String, Object> params = new HashMap<>();
		params.put(P_CLEAR_ALL, "yes");
		params.put(P_PERFORM_CLEAR, "yes");

		try (InputStream respStrm = sendBulkRequest(params, "clear", false)) {
			// Do nothing -- there is no response
		}
	}

	/**
	 * Exports all data in the repository to a zip file.
	 * <p>
	 * Each graph in the repository is serialized in the specified format
	 * and stored in a file named after the graph. These graph-files are
	 * then bundled into the specified zip file.
	 *
	 * @param zipFile File to write to.
	 * @param format Format of the data.
	 */
	public void exportRepository(File zipFile, RDFFormat format) throws IOException {
		try (OutputStream os = new FileOutputStream(zipFile)) {
			exportRepository(os, format);
		}
	}

	/**
	 * Exports all data in the repository as a zip file to an output stream.
	 * <p>
	 * Each graph in the repository is serialized in the specified format
	 * and stored in a file named after the graph. These graph-files are
	 * then bundled into a zip file that is written to the specified output
	 * stream.
	 *
	 * @param zipFileOutputStream Stream to write to.
	 * @param format Format of the data.
	 */
	public void exportRepository(OutputStream zipFileOutputStream, RDFFormat format) throws IOException {
		Map<String, Object> params = new HashMap<>();
		params.put("exportAll", "yes");
		params.put("dataFormat", format.toString());

		byte[] buffer = new byte[BUFFER_SIZE];
		try (InputStream respStrm = sendBulkRequest(params, "export", false)) {
			for (;;) {
				int bytesRead = respStrm.read(buffer);
				if (bytesRead < 0) {
					break;
				} else if (bytesRead > 0) {
					zipFileOutputStream.write(buffer, 0, bytesRead);
				}
			}
		}
	}

	/**
	 * Replaces the contents of the repository with the data contained in the
	 * specified zip file.
	 * <p>
	 * The specified zip file should contain a set of files, each representing
	 * a graph in the repository. Each graph-file should be named after the
	 * graph it represents and contain a serialized representation of the graph.
	 * The RDF format of each graph-file is inferred from the file extension.
	 *
	 * @param zipFile File to read from.
	 */
	public void importRepository(File zipFile) throws IOException {
		try (InputStream is = new FileInputStream(zipFile)) {
			importRepository(is);
		}
	}

	/**
	 * Replaces the contents of the repository with the data contained in the
	 * specified input stream.
	 * <p>
	 * The specified input stream should contain a zip file that contains the
	 * repository. The repository-zip file should contain a set of files, each
	 * representing a graph in the repository. Each graph-file should be named
	 * after the graph it represents and contain a serialized representation
	 * of the graph. The RDF format of each graph-file is inferred from the
	 * file extension.
	 *
	 * @param zipFileInputStream Stream to read from.
	 */
	public long importRepository(InputStream zipFileInputStream) throws IOException {
		Map<String, Object> params = new HashMap<>();
		params.put("import", "yes");
		params.put("statements", zipFileInputStream);

		byte[] buffer = new byte[BUFFER_SIZE];
		try (
			InputStream respStrm = sendBulkRequest(params, "insert", true);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
		) {
			for (;;) {
				int bytesRead = respStrm.read(buffer);
				if (bytesRead < 0) {
					break;
				} else if (bytesRead > 0) {
					baos.write(buffer, 0, bytesRead);
				}
			}
			String responseBody = baos.toString("UTF-8");
			Matcher m = INSERT_RESULT_PATTERN.matcher(responseBody);
			long result = -1;
			if (m.find()) {
				String numStmtsStr = m.group(1);
				result = Long.parseLong(numStmtsStr);
			}
			return result;
		}
	}

	private InputStream sendBulkRequest(Map<String, Object> params, String service,
		boolean multipart) throws IOException {

		String separator = _bulkEndpointUrl.endsWith("/") ? "" : "/";
		return sendRequest(params, _bulkEndpointUrl + separator + service, multipart);
	}

	public InputStream sendRequest(Map<String, Object> params) throws IOException {
		return sendRequest(params, _sparqlEndpointUrl, false);
	}

	/**
	 * Sends an HTTP-POST request with the supplied params to the given URL.  The
	 * server's response is checked, throwing an exception when it indicates an
	 * error.  Otherwise, the data returned by the server is returned as an InputStream.
	 *
	 * @param params The parameters for the POST request
	 * @param endpointUrl The URL to post the data to
	 * @param multipart Whether to send this request as "multipart/form-data" or "x-www-url-encoded"
	 * @return An InputStream containing the data that was returned by the server.
	 * @throws IOException In case of an error in the request or in the processing of
	 * it by the server.
	 */
	private InputStream sendRequest(Map<String, Object> params, String endpointUrl,
		boolean multipart) throws IOException {

		// Create the connection
		HttpURLConnection conn = (HttpURLConnection) new URL(endpointUrl).openConnection();
		HttpClientUtil.setAcceptGZIPEncoding(conn);

		if (multipart) {
			HttpClientUtil.prepareMultipartPostRequestInputStreamAware(conn, params, "UTF-8");
		} else {
			HttpClientUtil.preparePostRequest(conn, params);
		}

		// Send the request if it's not already been sent
		conn.connect();

		// Check whether the server reported any errors
		checkResponse(conn);

		// Get buffered input stream (HttpClientUtil takes care of any gzip compression)
		return new BufferedInputStream(HttpClientUtil.getInputStream(conn), _bufferSize);
	}

	private static void checkResponse(HttpURLConnection conn) throws IOException {
		int responseCode = conn.getResponseCode();
		if (responseCode != HttpURLConnection.HTTP_OK) {
			String responseMsg = conn.getResponseMessage();
			throw new IOException(responseMsg);
		}
	}

	public int getBufferSize() {
		return _bufferSize;
	}

	public void setBufferSize(int bufferSize) {
		this._bufferSize = bufferSize;
	}
}
