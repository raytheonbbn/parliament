// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2010, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.jetty;

/**
 * Provides entry points to use with the Apache Commons Daemon package (procrun)
 * to render Parliament running inside Jetty as a Windows Service.
 *
 * @author iemmons
 */
public class JettyService {
	private static final Logger LOG = LoggerFactory.getLogger(JettyService.class);

	private static boolean timeToExit = false;
	private static final Object lock = new Object();

	/** Entry point for running as a Windows service. */
	public static void start(String[] args) {
		try {
			JettyServerCore.initialize();
			JettyServerCore.getInstance().start();
			LOG.info("Starting Parliament server");
			synchronized (lock) {
				while (!timeToExit) {
					try {
						lock.wait(5000);
					} catch (InterruptedException ex) {
						// Do nothing
					}
				}
			}
			LOG.info("Shutting down Parliament server");
		} catch (Exception ex) {
			LOG.error("Parliament server encountered an exception", ex);
		} finally {
			JettyServerCore.getInstance().stop();
			LOG.info("Parliament server stopped");
		}
	}

	/** Called by the Windows service runner EXE to stop the service. */
	public static void stop(String[] args) {
		synchronized (lock) {
			timeToExit = true;
			lock.notifyAll();
		}
		LOG.info("Parliament server shutdown requested");
	}
}
