// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, 2014-2015, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.bridge;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.bbn.parliament.jena.bridge.tracker.TrackableException;
import com.bbn.parliament.jena.bridge.tracker.TrackableQuery;
import com.bbn.parliament.jena.bridge.tracker.TrackableUpdate;
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

	@SuppressWarnings("static-method")
	public void execQuery(String sparqlStmt, String requestor, OutputStream out) throws QueryExecutionException {
		if(sparqlStmt == null || sparqlStmt.isEmpty()) {
			throw new IllegalArgumentException("Null or blank query string");
		}
		try {
			TrackableQuery trackable = Tracker.getInstance().createQuery(sparqlStmt, requestor);
			SparqlStmtLogger.logSparqlStmt(sparqlStmt);

			getReadLock();
			try {
				execSelect(trackable, out);
			} finally {
				releaseReadLock();
				log.debug("Released read lock");
			}
		} catch (QueryParseException ex) {
			String msg = String.format("Query parsing error:%n    %1$s%n%n%2$s",
				ex.getMessage(), sparqlStmt);
			log.warn(LogUtil.fixEolsForLogging(msg));
			throw ex;
		}
	}

	private static void execSelect(TrackableQuery trackable, OutputStream out) throws QueryExecutionException {
		try {
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

				log.debug("OK/select");
			} else if (q.isConstructType() || q.isDescribeType()) {
				Model respModel = trackable.getModel();
				respModel.setWriterClassName(JsonLdRdfWriter.formatName, JsonLdRdfWriter.class.getName());
				//resp.setModel(respModel);
				log.debug(q.isConstructType() ? "OK/construct" : "OK/describe");
				throw new NotImplementedException("TODO: Need to port the commented line of code above from Joseki to Spring");
			} else if (q.isAskType()) {
				@SuppressWarnings("unused")
				boolean b = trackable.getBoolean();
				//resp.setBoolean(b);
				log.debug("OK/ask");
				throw new NotImplementedException("TODO: Need to port the commented line of code above from Joseki to Spring");
			} else {
				log.error(LogUtil.formatForLog("Unknown query type - ", trackable.getQuery().toString()));
			}
		} catch (TrackableException | DataFormatException | MissingGraphException | IOException ex) {
			throw new QueryExecutionException("Error while executing query", ex);
		}
	}

	@SuppressWarnings("static-method")
	public void execUpdate(String sparqlStmt, String requestor) throws QueryExecutionException {
		if(sparqlStmt == null || sparqlStmt.isEmpty()) {
			throw new IllegalArgumentException("Null or blank query string");
		}
		SparqlStmtLogger.logSparqlStmt(sparqlStmt);
		TrackableUpdate trackable = Tracker.getInstance().createUpdate(sparqlStmt, requestor);

		getWriteLock();
		try {
			trackable.run();
		} catch (TrackableException | DataFormatException | MissingGraphException | IOException ex) {
			throw new QueryExecutionException("Error while executing query", ex);
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
