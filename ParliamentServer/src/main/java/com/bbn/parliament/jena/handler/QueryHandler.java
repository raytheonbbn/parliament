// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.handler;

import java.io.IOException;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jena.bridge.ConcurrentRequestController;
import com.bbn.parliament.jena.bridge.ConcurrentRequestLock;
import com.bbn.parliament.jena.bridge.SparqlStmtLogger;
import com.bbn.parliament.jena.bridge.tracker.TrackableException;
import com.bbn.parliament.jena.bridge.tracker.TrackableQuery;
import com.bbn.parliament.jena.bridge.tracker.Tracker;
import com.bbn.parliament.jena.bridge.util.LogUtil;
import com.bbn.parliament.jena.exception.DataFormatException;
import com.bbn.parliament.jena.exception.MissingGraphException;
import com.bbn.parliament.jena.exception.QueryExecutionException;
import com.bbn.parliament.jena.util.JsonLdRdfWriter;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryParseException;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Model;

public class QueryHandler {
	private static Logger LOG = LoggerFactory.getLogger(QueryHandler.class);

	@SuppressWarnings("static-method")
	public void handleRequest(String sparqlStmt, String requestor, OutputStream out) throws QueryExecutionException {
		if(sparqlStmt == null || sparqlStmt.isEmpty()) {
			throw new IllegalArgumentException("Null or blank query string");
		}
		try {
			TrackableQuery trackable = Tracker.getInstance().createQuery(sparqlStmt, requestor);
			SparqlStmtLogger.logSparqlStmt(sparqlStmt);

			try (ConcurrentRequestLock lock = ConcurrentRequestController.getReadLock()) {
				execQuery(trackable, out);
			}
		} catch (TrackableException | DataFormatException | MissingGraphException | IOException ex) {
			throw new QueryExecutionException("Error while executing query", ex);
		} catch (QueryParseException ex) {
			String msg = String.format("Query parsing error:%n    %1$s%n%n%2$s",
				ex.getMessage(), sparqlStmt);
			LOG.warn(LogUtil.fixEolsForLogging(msg));
			throw ex;
		}
	}

	private static void execQuery(TrackableQuery trackable, OutputStream out)
		throws TrackableException, DataFormatException, MissingGraphException, IOException,
		QueryExecutionException {

		Query q = trackable.getQuery();
		trackable.run();

		if (trackable.getQueryResult() == null) {
			throw new QueryExecutionException("Query produced no result");
		} else if (q.isSelectType()) {
			ResultSet rs = trackable.getResultSet();

			//File tmpDir = ParliamentBridge.getInstance().getConfiguration().getTmpDir();
			//int threshold = ParliamentBridge.getInstance().getConfiguration().getDeferredFileOutputStreamThreshold();

			//final FileBackedResultSet fileBackedRS = new FileBackedResultSet(rs, tmpDir, threshold);

			//ResultSet result = fileBackedRS.getResultSet();

			ResultSetFormatter.outputAsXML(out, rs);

			//fileBackedRS.delete();

			LOG.debug("OK/select");
		} else if (q.isConstructType() || q.isDescribeType()) {
			Model respModel = trackable.getModel();
			respModel.setWriterClassName(JsonLdRdfWriter.formatName, JsonLdRdfWriter.class.getName());
			//resp.setModel(respModel);
			LOG.debug(q.isConstructType() ? "OK/construct" : "OK/describe");
			throw new UnsupportedOperationException("TODO: Need to port the commented line of code above from Joseki to Spring");
		} else if (q.isAskType()) {
			@SuppressWarnings("unused")
			boolean b = trackable.getBoolean();
			//resp.setBoolean(b);
			LOG.debug("OK/ask");
			throw new UnsupportedOperationException("TODO: Need to port the commented line of code above from Joseki to Spring");
		} else {
			LOG.error(LogUtil.formatForLog("Unknown query type - ", trackable.getQuery().toString()));
		}
	}
}
