// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.handler;

import java.io.OutputStream;

/*
import org.joseki.QueryExecutionException;
import org.joseki.Request;
import org.joseki.Response;
import org.joseki.ResponseCallback;
import org.joseki.ReturnCodes;
import org.joseki.processors.SPARQL;
*/

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jena.bridge.tracker.TrackableQuery;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;



/** @author ebenson@bbn.com */
public class QueryHandler {

	private static Logger log = LoggerFactory.getLogger(QueryHandler.class);

	public QueryHandler() {
		super();
	}

	public void execSelect(TrackableQuery trackable, OutputStream out) {
		try {
			Query q = trackable.getQuery();
			trackable.run();

			if (trackable.getQueryResult() == null) {
				log.debug("No result");
				throw new Exception("No result");
			} else if (q.isSelectType()) {
				ResultSet rs = trackable.getResultSet();
				log.trace("Setting result set");

				//File tmpDir = ParliamentBridge.getInstance().getConfiguration().getTmpDir();
				//int threshold = ParliamentBridge.getInstance().getConfiguration().getDeferredFileOutputStreamThreshold();

				//final FileBackedResultSet fileBackedRS = new FileBackedResultSet(rs, tmpDir, threshold);

				//ResultSet result = fileBackedRS.getResultSet();

				ResultSetFormatter.outputAsXML(out, rs);

				//fileBackedRS.delete();

				log.debug("OK/select");
			}
		} catch(Exception e) {
			log.info(e.toString());
		}
	}
}
