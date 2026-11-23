// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, 2014-2015, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.joseki.bridge;

import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.servlet.http.HttpServletRequest;

import org.apache.jena.query.QueryParseException;
import org.apache.jena.rdf.model.Resource;
import org.joseki.DatasetDesc;
import org.joseki.JosekiServerException;
import org.joseki.Processor;
import org.joseki.QueryExecutionException;
import org.joseki.Request;
import org.joseki.Response;
import org.joseki.ReturnCodes;
import org.joseki.module.Loadable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jena.joseki.bridge.servlet.ParliamentRequest;
import com.bbn.parliament.jena.joseki.bridge.tracker.TrackableQuery;
import com.bbn.parliament.jena.joseki.bridge.tracker.TrackableUpdate;
import com.bbn.parliament.jena.joseki.bridge.tracker.Tracker;
import com.bbn.parliament.jena.joseki.bridge.util.LogUtil;
import com.bbn.parliament.jena.joseki.handler.QueryHandler;
import com.bbn.parliament.jena.joseki.handler.UpdateHandler;

/**
 * Outer Joseki processor to route the request to the right handler.
 *
 * Joseki comes packaged with its "SPARQL" processor to handle SPARQL queries
 * but this doesn't support insert, update, or delete. This class multiplexes
 * between "Loadable" implementors to accomplish these extra three functions, as
 * well as wrap around a custom QueryHandler that makes use of the indexer.  It
 * also maintains the read and write locks.
 *
 * Ideally, choosing between query and update would be handled in a
 * RESTful way and would use HTTP commands to convey the intended operation
 * (Sesame goes this route), but Joseki uses request parameters to convey the
 * desired action instead of the HTTP command. This class follows Joseki's
 * choice and uses request parameters to specify the desired action.  The
 * BulkServlet by contrast is RESTful.
 *
 * Bulk operations are handled by the BulkServlet, and include insert, export,
 * and flush.  Delete, and Clear should be handled via SPARQL/Update.
 *
 * @author ebenson@bbn.com
 * @author sallen@bbn.com
 *
 * sallen (4/23/2009) - Moved bulk operations to the BulkServlet.  This class
 * now only handles SPARQL and SPARQL/Update queries.
 */
public class ActionRouter implements Loadable, Processor {
	public static final boolean FAIR_READ_WRITE_LOCK = true;

	private static Logger log = LoggerFactory.getLogger(ActionRouter.class);
	private static ReadWriteLock lock = new ReentrantReadWriteLock(FAIR_READ_WRITE_LOCK);

	private Resource initService = null;
	private Resource initImplementation = null;

	public ActionRouter() {
	}

	@Override
	public void init(Resource service, Resource implementation) {
		initService = service;
		initImplementation = implementation;
	}

	private static String formatRequestParticulars(ParliamentRequest preq, String msg) {
		HttpServletRequest hsr = preq.getHttpReq();
		StringBuilder params = new StringBuilder();
		if (hsr.getParameterMap().size() <= 0) {
			params.append("<none>");
		} else {
			for (Map.Entry<String, String[]> e : hsr.getParameterMap().entrySet()) {
				for (String val : e.getValue()) {
					params.append("%n      '%1$s' = '%2$s'".formatted(e.getKey(), val));
				}
			}
		}
		return """
			%1$s Request details:
				Method:        %2$s
				URL:           %3$s
				Content Type:  %4$s
				Parameters:    %5$s
			""".formatted(msg, hsr.getMethod(), hsr.getRequestURL(), hsr.getContentType(), params);
	}

	//resp.setHeader("Content-Type", "application/json");
	/** Handles incoming connection and routes to the appropriate handler. */
	@Override
	public void exec(Request req, Response resp, DatasetDesc dsDesc)
		throws QueryExecutionException {

		if (!(req instanceof ParliamentRequest)) {
			log.warn("ActionRouter.exec() passed a request object not of type ParliamentRequest");
			return;
		}
		ParliamentRequest preq = (ParliamentRequest) req;

		// Some error checking:  Only one of QUERY and UPDATE is allowed.
		if (!preq.isSparqlQuery() && !preq.isSparqlUpdate()) {
			String msg = formatRequestParticulars(preq,"""
				Request is neither a SPARQL query nor a SPARQL update.  Ensure \
				that the request method, content type, and parameters are set \
				according to the SPARQL protocol.""");
			log.warn(msg);
			throw new JosekiServerException(msg);
		} else if (preq.isSparqlQuery() && preq.isSparqlUpdate()) {
			String msg = formatRequestParticulars(preq,
				"Too many commands given -- must choose between query and update.");
			log.warn(msg);
			throw new JosekiServerException(msg);
		}

		// Passed the error checks, so delegate to the proper handler:
		String sparqlStmt = preq.getSparqlStmt();
		String requestor = preq.getRequestor();
		try {
			if (preq.isSparqlQuery()) {
				QueryHandler handler = new QueryHandler();
				handler.init(initService, initImplementation);

				TrackableQuery trackable = Tracker.getInstance().createQuery(sparqlStmt, requestor);
				SparqlStmtLogger.logSparqlStmt(sparqlStmt);

				getReadLock();
				try {
					handler.execQuery(trackable, preq, resp);
				} finally {
					releaseReadLock();
					log.debug("Released read lock");
				}
			} else if (preq.isSparqlUpdate()) {
				UpdateHandler handler = new UpdateHandler();
				handler.init(initService, initImplementation);

				TrackableUpdate trackable = Tracker.getInstance().createUpdate(sparqlStmt, requestor);
				SparqlStmtLogger.logSparqlStmt(sparqlStmt);

				getWriteLock();
				try {
					handler.execQuery(trackable, preq, resp);
				} finally {
					releaseWriteLock();
					log.debug("Released write lock");
				}
			}
		} catch (QueryParseException ex) {
			String msg = "Encountered an error while parsing query:%n    %1$s%n%n%2$s"
				.formatted(ex.getMessage(), sparqlStmt);
			log.info(LogUtil.fixEolsForLogging(msg));
			throw new QueryExecutionException(ReturnCodes.rcQueryParseFailure, msg);
		}
	}

	public static void getWriteLock() {
		lock.writeLock().lock();
	}

	public static void releaseWriteLock() {
		lock.writeLock().unlock();
	}

	public static void getReadLock() {
		lock.readLock().lock();
	}

	public static void releaseReadLock() {
		lock.readLock().unlock();
	}
}
