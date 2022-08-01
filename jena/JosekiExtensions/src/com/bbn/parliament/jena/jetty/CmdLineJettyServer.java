// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2010, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.jetty;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CmdLineJettyServer {
	private static final CountDownLatch serverShouldShutDown = new CountDownLatch(1);
	private static final Logger LOG = LoggerFactory.getLogger(CmdLineJettyServer.class);

	private static class StdinMonitorThread extends Thread {
		public StdinMonitorThread() {
			super("Stdin Monitor Thread");
			setDaemon(true);
		}

		@Override
		public void run() {
			try {
				for (;;) {
					printPrompt();
					byte[] bytes = new byte[256];
					int count = System.in.read(bytes);
					if (count < 0) {
						// This means the read reached the end of the stream. This only
						// happens when the process has no proper stdin, i.e., when the
						// process is being run as a daemon.  We exit the loop here to stop
						// pointlessly checking for an "exit" command that will never come,
						// but we do not send the shutdown signal.  In this case, that will
						// come via the shutdown hook below.
						break;
					}
					String consoleInput = new String(bytes, 0, count, StandardCharsets.UTF_8);
					if ("exit".equalsIgnoreCase(consoleInput.trim())) {
						sendShutdownSignal();
						break;
					}
				}
			} catch (Throwable ex) {
				LOG.error("Exception:", ex);
			}
		}

		private static void printPrompt() {
			System.out.format("""



				######################################################################
				######################################################################
				#####                                                            #####
				#####   Warning:  Killing the server process by any means other  #####
				#####   than graceful shutdown may result in corrupt knowledge   #####
				#####   base files.  Please shut down the server by typing       #####
				#####   'exit' or 'Ctrl+C' at the prompt below.                  #####
				#####                                                            #####
				######################################################################
				######################################################################


				Type 'exit' and press <return> or <enter> to shut down the server.
				""");
		}
	}

	private static class ShutdownHook extends Thread {
		public ShutdownHook() {
			super("Shutdown Hook Thread");
			setDaemon(true);
		}

		@Override
		public void run() {
			sendShutdownSignal();
		}
	}

	private static void sendShutdownSignal() {
		LOG.info("Sending shutdown signal from {}", Thread.currentThread().getName());
		serverShouldShutDown.countDown();
	}

	/** Default entry point. */
	public static void main(String[] args) {
		try {
			JettyServerCore.getInstance().start();

			Thread.sleep(4000);

			Runtime.getRuntime().addShutdownHook(new ShutdownHook());

			Thread stdinMonitorThread = new StdinMonitorThread();
			stdinMonitorThread.start();

			serverShouldShutDown.await();

			System.out.println("Shutting down server");
		} catch (Throwable ex) {
			LOG.error("Parliament server encountered an exception", ex);
		} finally {
			JettyServerCore.getInstance().stop();
		}
	}
}
