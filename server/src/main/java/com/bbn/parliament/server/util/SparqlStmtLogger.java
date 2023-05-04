// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2015, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.server.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class exists to allow control of log level for SPARQL statements (queries and
 * updates) independent of the log level of any other class.
 *
 * @author iemmons
 */
public class SparqlStmtLogger {
	private static Logger log = LoggerFactory.getLogger(SparqlStmtLogger.class);

	public static void logSparqlStmt(String sparqlStmt) {
		if (log.isTraceEnabled()) {
			log.trace("SPARQL statement:\n{}", LogUtil.fixEolsForLogging(sparqlStmt));
		}
	}
}
