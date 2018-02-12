// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2010, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.jetty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides entry points to use with the Apache Commons Daemon package (procrun)
 * to render Parliament running inside Jetty as a Windows Service.
 *
 * @author iemmons
 */
public class JettyService {
	private static Logger _log = LoggerFactory.getLogger(JettyService.class);

	/** Entry point for running as a Windows service. */
	public static void start(String[] args) {
		try {
			JettyServerCore.initialize();
			JettyServerCore.getInstance().start();
		} catch (Exception ex) {
			_log.error("Parliament server encountered an exception", ex);
		}
	}

	/** Called by the Windows service runner EXE to stop the service. */
	public static void stop(String[] args) {
		JettyServerCore.getInstance().stop();
	}
}
