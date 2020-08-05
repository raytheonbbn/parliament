// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.handler;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jena.bridge.tracker.TrackableException;
import com.bbn.parliament.jena.bridge.tracker.TrackableUpdate;


/**
 * @author dreid@bbn.com
 * @author ebenson@bbn.com
 */
public class UpdateHandler {
	private static Logger log = LoggerFactory.getLogger(UpdateHandler.class);


	public void execUpdate(TrackableUpdate trackable) throws Exception {
		String updateQueryString = trackable.getQuery();
		// handle request parsing...

		if(updateQueryString == null) {
			if (log.isDebugEnabled()) {
				log.debug("No query argument");
			}
			throw new Exception("No query string");
		}

		if(updateQueryString.equals("")) {
			if (log.isDebugEnabled()) {
				log.debug("Empty query string");
			}
			throw new Exception("Empty query string");
		}

		try {
			trackable.run();
		} catch(TrackableException e) {
			log.error("TrackableException", e);
			throw e;
		}
	}
}
