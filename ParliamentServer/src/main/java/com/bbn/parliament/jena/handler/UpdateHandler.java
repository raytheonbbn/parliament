// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.handler;

import java.io.IOException;

import com.bbn.parliament.jena.bridge.ConcurrentRequestController;
import com.bbn.parliament.jena.bridge.ConcurrentRequestLock;
import com.bbn.parliament.jena.bridge.SparqlStmtLogger;
import com.bbn.parliament.jena.bridge.tracker.TrackableException;
import com.bbn.parliament.jena.bridge.tracker.TrackableUpdate;
import com.bbn.parliament.jena.bridge.tracker.Tracker;
import com.bbn.parliament.jena.exception.DataFormatException;
import com.bbn.parliament.jena.exception.MissingGraphException;
import com.bbn.parliament.jena.exception.QueryExecutionException;

public class UpdateHandler {
	@SuppressWarnings("static-method")
	public void handleRequest(String sparqlStmt, String requestor) throws QueryExecutionException {
		if(sparqlStmt == null || sparqlStmt.isEmpty()) {
			throw new IllegalArgumentException("Null or blank query string");
		}
		SparqlStmtLogger.logSparqlStmt(sparqlStmt);
		TrackableUpdate trackable = Tracker.getInstance().createUpdate(sparqlStmt, requestor);

		try (ConcurrentRequestLock lock = ConcurrentRequestController.getWriteLock()) {
			trackable.run();
		} catch (TrackableException | DataFormatException | MissingGraphException | IOException ex) {
			throw new QueryExecutionException("Error while executing query", ex);
		}
	}
}
