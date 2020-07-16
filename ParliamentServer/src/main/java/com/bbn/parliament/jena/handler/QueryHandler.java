// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.handler;

import java.io.File;
import java.util.concurrent.CancellationException;

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

import com.bbn.parliament.jena.bridge.ParliamentBridge;
import com.bbn.parliament.jena.bridge.tracker.TrackableException;
import com.bbn.parliament.jena.bridge.tracker.TrackableQuery;
import com.bbn.parliament.jena.bridge.util.LogUtil;
import com.bbn.parliament.jena.util.JsonLdRdfWriter;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryException;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.shared.NotFoundException;
import com.hp.hpl.jena.shared.QueryStageException;

import java.io.OutputStream;



/** @author ebenson@bbn.com */
public class QueryHandler {

	private static Logger log = LoggerFactory.getLogger(QueryHandler.class);

	public QueryHandler() {
		super();
	}

	/*
	@Override
	public void init(Resource service, Resource implementation) {
		super.init(service, implementation);
	}
	*/
	
	public ResultSet execSelect(TrackableQuery trackable) {
		try {
			Query q = trackable.getQuery();
			trackable.run();

			if (trackable.getQueryResult() == null) {
				log.debug("No result");
				//throw new QueryExecutionException(ReturnCodes.rcServiceUnavailable, "No result"); //removed
				throw new Exception("No result");
			} else if (q.isSelectType()) {
				ResultSet rs = trackable.getResultSet();
				log.trace("Setting result set");

				File tmpDir = ParliamentBridge.getInstance().getConfiguration().getTmpDir();
				int threshold = ParliamentBridge.getInstance().getConfiguration().getDeferredFileOutputStreamThreshold();

				final FileBackedResultSet fileBackedRS = new FileBackedResultSet(rs, tmpDir, threshold);
				
				ResultSet result = fileBackedRS.getResultSet();
				fileBackedRS.delete();

				log.debug("OK/select");
				
				return result;
			}
		} catch(Exception e) {
			log.info(e.toString());
		}
		
		return null;
	}
	
	
	public void execSelect(TrackableQuery trackable, OutputStream out) {
		try {
			Query q = trackable.getQuery();
			trackable.run();

			if (trackable.getQueryResult() == null) {
				log.debug("No result");
				//throw new QueryExecutionException(ReturnCodes.rcServiceUnavailable, "No result"); //removed
				throw new Exception("No result");
			} else if (q.isSelectType()) {
				ResultSet rs = trackable.getResultSet();
				log.trace("Setting result set");

				File tmpDir = ParliamentBridge.getInstance().getConfiguration().getTmpDir();
				int threshold = ParliamentBridge.getInstance().getConfiguration().getDeferredFileOutputStreamThreshold();

				final FileBackedResultSet fileBackedRS = new FileBackedResultSet(rs, tmpDir, threshold);
				
				ResultSet result = fileBackedRS.getResultSet();
				
				
				ResultSetFormatter.outputAsXML(out, result);
				
				fileBackedRS.delete();

				log.debug("OK/select");
				
				//return result;
			}
		} catch(Exception e) {
			log.info(e.toString());
		}
		
		//return null;
	}
	/*
	@SuppressWarnings("static-method")
	public void execQuery(TrackableQuery trackable)
		throws Exception { //changed from QueryExecutionException
		try {
			Query q = trackable.getQuery();

			trackable.run();

			if (trackable.getQueryResult() == null) {
				log.debug("No result");
				//throw new QueryExecutionException(ReturnCodes.rcServiceUnavailable, "No result"); //removed
				throw new Exception("No result");
			} else if (q.isSelectType()) {
				ResultSet rs = trackable.getResultSet();
				log.trace("Setting result set");

				//ResultSetMem memoryRS = new ResultSetMem(rs);
				//resp.setResultSet(memoryRS);

				File tmpDir = ParliamentBridge.getInstance().getConfiguration().getTmpDir();
				int threshold = ParliamentBridge.getInstance().getConfiguration().getDeferredFileOutputStreamThreshold();

				final FileBackedResultSet fileBackedRS = new FileBackedResultSet(rs, tmpDir, threshold);
				resp.addCallback(new ResponseCallback()
				{
					@Override
					public void callback(boolean successfulOperation) {
						fileBackedRS.delete();
					}
				});
				
				ResultSet result = fileBackedRS.getResultSet();


				log.debug("OK/select");
			} else if (q.isConstructType() || q.isDescribeType()) {
				Model respModel = trackable.getModel();
				respModel.setWriterClassName(JsonLdRdfWriter.formatName, JsonLdRdfWriter.class.getName());
				resp.setModel(respModel);
				log.debug(q.isConstructType() ? "OK/construct" : "OK/describe");
			} else if (q.isAskType()) {
				boolean b = trackable.getBoolean();
				resp.setBoolean(b);
				log.debug("OK/ask");
			} else {
				log.error(LogUtil.formatForLog("Unknown query type - ", trackable.getQuery().toString()));
			}
		} catch(TrackableException e) {
			log.error("TrackableException", e);
			throw new QueryExecutionException(
				ReturnCodes.rcInternalError, LogUtil.getExceptionInfo(e));
		} catch (QueryException e) {
			log.error("Query execution error", e);
			throw new QueryExecutionException(
				ReturnCodes.rcQueryExecutionFailure, LogUtil.getExceptionInfo(e));
		} catch (NotFoundException e) {
			// Trouble loading data
			log.error("NotFoundException: ", e);
			throw new QueryExecutionException(
				ReturnCodes.rcResourceNotFound, LogUtil.getExceptionInfo(e));
		} catch (QueryStageException e) {
			// special handling for stage exception caused by canceling a query
			log.error("QueryStageException", e);
			//if (e.getCause() instanceof CancelledException) {
			//	log.error("Cause: CancelledException");
			//	throw new QueryExecutionException(
			//		ReturnCodes.rcInternalError, String.format("Query cancelled: %d\n%s", trackable.getId(), trackable.getQuery().toString()));
			//} else {
			throw new QueryExecutionException(ReturnCodes.rcInternalError, LogUtil.getExceptionInfo(e));
			//}
		} catch (JenaException e) { // Parse exceptions
			log.error("JenaException", e);
			throw new QueryExecutionException(
				ReturnCodes.rcArgumentUnreadable, LogUtil.getExceptionInfo(e));
		} catch (CancellationException e) {
			log.error("CancelledException", e);
			throw new QueryExecutionException(
				ReturnCodes.rcInternalError, String.format("Query cancelled: %d\n%s", trackable.getId(), trackable.getQuery().toString()));
		} catch (RuntimeException e) { // Parse exceptions
			log.error("RuntimeException",e);
			throw new QueryExecutionException(
				ReturnCodes.rcInternalError, LogUtil.getExceptionInfo(e));
		}
	}
	*/
}
