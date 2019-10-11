package com.bbn.parliament.jena.joseki.bridge;

import static org.junit.jupiter.api.Assertions.fail;

import com.bbn.parliament.jena.jetty.InterThreadSignal;
import com.bbn.parliament.jena.jetty.JettyServerCore;

public class ParliamentTestServer {
	private static final InterThreadSignal serverHasStarted = new InterThreadSignal();
	private static final InterThreadSignal serverShouldShutDown = new InterThreadSignal();

	private static class TestServerThread extends Thread {
		public TestServerThread() {
			super("Test Server Thread");
			setDaemon(true);
		}

		@Override
		public void run() {
			try {
				JettyServerCore.initialize();
				JettyServerCore.getInstance().start();
				serverHasStarted.sendSignal();
				serverShouldShutDown.waitForSignal();
			} catch (Exception ex) {
				serverHasStarted.sendSignal();
				fail(ex.getMessage());
			} finally {
				JettyServerCore.getInstance().stop();
			}
		}
	}

	// Call from @BeforeClass
	public static void createServer() {
		Thread testServerThread = new TestServerThread();
		testServerThread.start();

		try {
			serverHasStarted.waitForSignal();
		} catch (InterruptedException ex) {
			fail(ex.getMessage());
		}
	}

	// Call from @AfterClass
	public static void stopServer() {
		serverShouldShutDown.sendSignal();
	}
}
