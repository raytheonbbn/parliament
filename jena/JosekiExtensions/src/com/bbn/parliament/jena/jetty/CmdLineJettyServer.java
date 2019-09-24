// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2010, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.jetty;

import java.nio.charset.StandardCharsets;

public class CmdLineJettyServer {
	private static final Logger LOG = LoggerFactory.getLogger(CmdLineJettyServer.class);

	/** Default entry point. */
	public static void main(String[] args) {
		try {
			JettyServerCore.initialize();
			JettyServerCore.getInstance().start();

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
		} catch (Exception ex) {
			LOG.error("Parliament server encountered an exception", ex);
		} finally {
			JettyServerCore.getInstance().stop();
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
