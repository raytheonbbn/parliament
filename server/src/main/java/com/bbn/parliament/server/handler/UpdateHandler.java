// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.server.handler;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

import com.bbn.parliament.server.exception.DataFormatException;
import com.bbn.parliament.server.exception.MissingGraphException;
import com.bbn.parliament.server.exception.QueryExecutionException;
import com.bbn.parliament.server.exception.TrackableException;
import com.bbn.parliament.server.tracker.TrackableUpdate;
import com.bbn.parliament.server.tracker.Tracker;
import com.bbn.parliament.server.util.ConcurrentRequestController;
import com.bbn.parliament.server.util.ConcurrentRequestLock;
import com.bbn.parliament.server.util.SparqlStmtLogger;

public class UpdateHandler {
	@SuppressWarnings("static-method")
	public void handleRequest(String sparqlStmt, String requestor) throws QueryExecutionException {
		if(StringUtils.isBlank(sparqlStmt)) {
			throw new IllegalArgumentException("Null or blank query string");
		}
		SparqlStmtLogger.logSparqlStmt(sparqlStmt);
		TrackableUpdate trackable = Tracker.getInstance().createUpdate(sparqlStmt, requestor);

		try (ConcurrentRequestLock lock = ConcurrentRequestController.getWriteLock()) {
			@SuppressWarnings("unused") int intentionallyUnused = lock.hashCode();
			trackable.run();
		} catch (TrackableException | DataFormatException | MissingGraphException | IOException ex) {
			throw new QueryExecutionException(ex, "Error while executing query");
		}
	}
}
