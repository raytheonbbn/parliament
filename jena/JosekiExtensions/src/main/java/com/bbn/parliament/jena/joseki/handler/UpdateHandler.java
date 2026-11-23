// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.joseki.handler;

import org.apache.jena.rdf.model.Resource;
import org.joseki.QueryExecutionException;
import org.joseki.Response;
import org.joseki.ReturnCodes;
import org.joseki.module.Loadable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jena.joseki.bridge.servlet.ParliamentRequest;
import com.bbn.parliament.jena.joseki.bridge.tracker.TrackableException;
import com.bbn.parliament.jena.joseki.bridge.tracker.TrackableUpdate;

/**
 * @author dreid@bbn.com
 * @author ebenson@bbn.com
 */
public class UpdateHandler implements Loadable {
	private static Logger log = LoggerFactory.getLogger(UpdateHandler.class);

	@Override
	public void init(Resource service, Resource implementation) {
	}

	@SuppressWarnings("static-method")
	public void execQuery(TrackableUpdate trackable, ParliamentRequest req, Response resp) throws QueryExecutionException {
		String updateQueryString = null;
		// handle request parsing...
		if (req.isSparqlUpdate()) {
			updateQueryString = req.getSparqlStmt();
			if (updateQueryString == null) {
				if (log.isDebugEnabled()) {
					log.debug("No update argument (but update parameter exists)");
				}
				throw new QueryExecutionException(ReturnCodes.rcArgumentError,
					"Update string is null");
			}
		}

		if(updateQueryString == null) {
			if (log.isDebugEnabled()) {
				log.debug("No query argument");
			}
			throw new QueryExecutionException(ReturnCodes.rcQueryExecutionFailure,
				"No query string");
		}

		if(updateQueryString.equals("")) {
			if (log.isDebugEnabled()) {
				log.debug("Empty query string");
			}
			throw new QueryExecutionException(ReturnCodes.rcQueryExecutionFailure,
				"Empty query string");
		}

		try {
			trackable.run();
		} catch(TrackableException e) {
			log.error("TrackableException", e);
			throw new QueryExecutionException(ReturnCodes.rcInternalError, e.getMessage());
		}

		resp.setOK();
	}
}
