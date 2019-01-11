// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2010, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.jetty;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CmdLineJettyServer {
	private static Logger _log = LoggerFactory.getLogger(CmdLineJettyServer.class);

	/** Default entry point. */
	public static void main(String[] args) {
		try {
			JettyServerCore.initialize();
			StdinMonitorThread monitor = new StdinMonitorThread();
			monitor.start();
			JettyServerCore.getInstance().start();
		} catch (Exception ex) {
			_log.error("Parliament server encountered an exception", ex);
		}
	}

	private static class StdinMonitorThread extends Thread {
		public StdinMonitorThread() {
			super("Standard-In Monitor Thread");
			setDaemon(true);
		}

		@Override
		public void run() {
			try {
				try {
					Thread.sleep(4000);
				} catch (InterruptedException ex) {
					// Do nothing
				}
				for (boolean timeToExit = false; !timeToExit;) {
					printPrompt();
					byte[] bytes = new byte[256];
					int count = System.in.read(bytes);
					String consoleInput = new String(bytes, 0, count, StandardCharsets.UTF_8).trim();
					timeToExit = consoleInput.equalsIgnoreCase("exit");
				}
				System.out.format("Shutting down server%n");
				JettyServerCore.getInstance().stop();
			} catch (IOException ex) {
				// Do nothing
			}
		}

		private static void printPrompt() {
			System.out.format("%n"
				+ "%n"
				+ "%n"
				+ "######################################################################%n"
				+ "######################################################################%n"
				+ "#####                                                            #####%n"
				+ "#####   Warning:  Killing the server process by any means other  #####%n"
				+ "#####   than graceful shutdown may result in corrupt knowledge   #####%n"
				+ "#####   base files.  Please shut down the server by typing       #####%n"
				+ "#####   'exit' at the prompt below.                              #####%n"
				+ "#####                                                            #####%n"
				+ "######################################################################%n"
				+ "######################################################################%n"
				+ "%n"
				+ "%n"
				+ "Type 'exit' and press <return> or <enter> to shut down the server.%n");
		}
	}
}
