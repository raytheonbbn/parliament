package com.bbn.parliament.jena.joseki.bridge;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.CountDownLatch;

import com.bbn.parliament.jena.jetty.JettyServerCore;

public class ParliamentTestServer {
	private static final CountDownLatch serverHasStarted = new CountDownLatch(1);
	private static final CountDownLatch serverShouldShutDown = new CountDownLatch(1);

	private static class TestServerThread extends Thread {
		public TestServerThread() {
			super("Test Server Thread");
			setDaemon(true);
		}

		@Override
		public void run() {
			try {
				JettyServerCore.getInstance().start();
				serverHasStarted.countDown();
				serverShouldShutDown.await();
			} catch (Exception ex) {
				serverHasStarted.countDown();
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
			serverHasStarted.await();
		} catch (InterruptedException ex) {
			fail(ex.getMessage());
		}
	}

	// Call from @AfterClass
	public static void stopServer() {
		serverShouldShutDown.countDown();
	}
}
