// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, 2014-2015, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.bridge;

import java.io.OutputStream;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/*
import org.joseki.DatasetDesc;
import org.joseki.JosekiServerException;
import org.joseki.Processor;
import org.joseki.QueryExecutionException;
import org.joseki.Request;
import org.joseki.Response;
import org.joseki.ReturnCodes;
import org.joseki.module.Loadable;
*/

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

//import com.bbn.parliament.jena.joseki.bridge.servlet.ParliamentRequest;
import com.bbn.parliament.jena.bridge.tracker.TrackableQuery;
import com.bbn.parliament.jena.bridge.tracker.TrackableUpdate;
import com.bbn.parliament.jena.bridge.tracker.Tracker;
import com.bbn.parliament.jena.bridge.util.LogUtil;
import com.bbn.parliament.jena.handler.QueryHandler;
import com.bbn.parliament.jena.handler.UpdateHandler;
import com.hp.hpl.jena.query.QueryParseException;
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



@Component("actionRouter")
public class ActionRouter {
	public static final boolean FAIR_READ_WRITE_LOCK = true;

	private static Logger log = LoggerFactory.getLogger(ActionRouter.class);
	private static ReadWriteLock lock = new ReentrantReadWriteLock(FAIR_READ_WRITE_LOCK);

	public ActionRouter() {}

	public void execQuery(String sparqlStmt, String requestor, OutputStream out) throws Exception {
		try {
			QueryHandler handler = new QueryHandler();

			TrackableQuery trackable = Tracker.getInstance().createQuery(sparqlStmt, requestor);
			SparqlStmtLogger.logSparqlStmt(sparqlStmt);

			getReadLock();
			try {
				handler.execSelect(trackable, out);
			} finally {
				releaseReadLock();
				log.debug("Released read lock");
			}

		} catch (QueryParseException ex) {
			String msg = String.format(
					"Encountered an error while parsing query:%n    %1$s%n%n%2$s",
					ex.getMessage(), sparqlStmt);
				log.info(LogUtil.fixEolsForLogging(msg));
				throw ex;
		}
	}

	public void execUpdate(String sparqlStmt, String requestor) throws Exception {

			UpdateHandler handler = new UpdateHandler();
			//handler.init(initService, initImplementation);

			TrackableUpdate trackable = Tracker.getInstance().createUpdate(sparqlStmt, requestor);
			SparqlStmtLogger.logSparqlStmt(sparqlStmt);

			getWriteLock();
			try {
				handler.execUpdate(trackable);
			} finally {
				releaseWriteLock();
				log.debug("Released write lock");
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
