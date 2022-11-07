// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.handler;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;

import org.apache.jena.query.QueryParseException;
import org.apache.jena.rdf.model.Model;
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
import com.bbn.parliament.jena.exception.NoAcceptableException;
import com.bbn.parliament.jena.exception.QueryExecutionException;
import com.bbn.parliament.jena.util.JsonLdRdfWriter;
import com.bbn.parliament.spring_boot.service.AcceptableMediaType;
import com.bbn.parliament.spring_boot.service.QueryResultCategory;

public class QueryHandler {
	private static Logger LOG = LoggerFactory.getLogger(QueryHandler.class);

	private final String query;
	private final TrackableQuery trackable;
	private final QueryResultCategory queryCategory;
	private final AcceptableMediaType contentType;

	public QueryHandler(String query, List<AcceptableMediaType> acceptList, String requestor) {
		if(query == null || query.isEmpty()) {
			throw new IllegalArgumentException("Null or blank query string");
		}
		Objects.requireNonNull(acceptList, "acceptList");
		Objects.requireNonNull(requestor, "requestor");

		this.query = query;
		trackable = Tracker.getInstance().createQuery(query, requestor);
		queryCategory = (trackable.getQuery().isConstructType() || trackable.getQuery().isDescribeType())
			? QueryResultCategory.RDF
			: QueryResultCategory.RESULT_SET;
		contentType = chooseMediaType(acceptList, queryCategory);
	}

	public AcceptableMediaType getContentType() {
		return contentType;
	}

	public void handleRequest(OutputStream out) throws IOException {
		try {
			SparqlStmtLogger.logSparqlStmt(query);
			try (ConcurrentRequestLock lock = ConcurrentRequestController.getReadLock()) {
				@SuppressWarnings("unused") int intentionallyUnused = lock.hashCode();
				execQuery(out);
			}
		} catch (TrackableException | DataFormatException | MissingGraphException | QueryExecutionException ex) {
			throw new IOException("Error while executing query", ex);
		} catch (QueryParseException ex) {
			LOG.warn(LogUtil.fixEolsForLogging("Query parsing error:%n    %1$s%n%n%2$s"
				.formatted(ex.getMessage(), query)));
			throw ex;
		}
	}

	private void execQuery(OutputStream out) throws TrackableException, DataFormatException,
		MissingGraphException, IOException, QueryExecutionException {

		trackable.run();

		if (trackable.getQueryResult() == null) {
			throw new QueryExecutionException("Query produced no result");
		} else if (queryCategory == QueryResultCategory.RDF) {
			Model respModel = trackable.getModel();
			respModel.setWriterClassName(JsonLdRdfWriter.formatName, JsonLdRdfWriter.class.getName());
			respModel.write(out, contentType.getRdfFormat().toString(), null);
			LOG.debug(trackable.getQuery().isConstructType() ? "OK/construct" : "OK/describe");
		} else if (trackable.getQuery().isSelectType()) {
			//File tmpDir = ParliamentBridge.getInstance().getConfiguration().getTmpDir();
			//int threshold = ParliamentBridge.getInstance().getConfiguration().getDeferredFileOutputStreamThreshold();
			//FileBackedResultSet fileBackedRS = new FileBackedResultSet(trackable.getResultSet(), tmpDir, threshold);
			//ResultSet result = fileBackedRS.getResultSet();

			contentType.serializeResultSet(out, trackable.getResultSet());

			//fileBackedRS.delete();

			LOG.debug("OK/select");
		} else if (trackable.getQuery().isAskType()) {
			contentType.serializeResultSet(out, trackable.getBoolean());
			LOG.debug("OK/ask");
		} else {
			LOG.error(LogUtil.formatForLog("Unknown query type - ", trackable.getQuery().toString()));
		}
	}

	private static AcceptableMediaType chooseMediaType(
		List<AcceptableMediaType> acceptList, QueryResultCategory category) {

		LOG.info(
			"chooseMediaType: accept list = {}, category = {}, default media type = {}",
			acceptList, category, category.getDefaultMediaType());
		if (acceptList.isEmpty()) {
			return category.getDefaultMediaType();
		} else {
			return acceptList.stream()
				.filter(mt -> mt.getCategory() == category)
				.findFirst()
				.orElseThrow(() -> new NoAcceptableException(QueryResultCategory.RESULT_SET));
		}
	}
}
