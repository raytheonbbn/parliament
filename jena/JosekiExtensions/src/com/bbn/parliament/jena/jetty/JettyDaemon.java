// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2011, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.jetty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides entry points to use with the Apache Commons Daemon package to
 * render Parliament running inside Jetty as a UNIX or Linux Daemon.
 *
 * @author iemmons
 */
public class JettyDaemon {
	private static Logger _log = LoggerFactory.getLogger(JettyDaemon.class);

	/**
	 * Initialize the daemon, possibly running as super-user.  E.g., open
	 * configuration files, create log files, create server sockets, create
	 * threads, etc.
	 */
	@SuppressWarnings("static-method")
	public void init(String[] args) {
		try {
			_log.info("Initializing Jetty Daemon");
			JettyServerCore.initialize();
		} catch (JettyServerCore.InitException ex) {
			_log.error("Parliament server encountered an exception", ex);
		}
	}

	/** Start the server running */
	@SuppressWarnings("static-method")
	public void start() {
		try {
			_log.info("Starting Jetty Daemon");
			JettyServerCore.getInstance().start();
		} catch (Exception ex) {
			_log.error("Parliament server encountered an exception", ex);
		}
	}

	/** Tell the server to terminate */
	@SuppressWarnings("static-method")
	public void stop() {
		_log.info("Stopping Jetty Daemon");
		JettyServerCore.getInstance().stop();
	}

	/** Cleanup resources allocated in init() */
	@SuppressWarnings("static-method")
	public void destroy() {
		_log.info("Destroying Jetty Daemon");
		// Do nothing
	}
}
