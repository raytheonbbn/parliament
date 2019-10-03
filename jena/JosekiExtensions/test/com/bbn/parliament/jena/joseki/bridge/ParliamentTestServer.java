package com.bbn.parliament.jena.joseki.bridge;

import static org.junit.jupiter.api.Assertions.fail;

import com.bbn.parliament.jena.jetty.JettyServerCore;

public class ParliamentTestServer {
	private static class InterThreadSignal {
		private boolean signalSent;
		private final Object lock;

		public InterThreadSignal() {
			signalSent = false;
			lock = new Object();
		}

		public void waitForSignal() {
			synchronized (lock) {
				while (!signalSent) {
					try {
						lock.wait(5000);
					} catch (InterruptedException ex) {
						fail(ex.getMessage());
					}
				}
			}
		}

		public void sendSignal() {
			synchronized (lock) {
				signalSent = true;
				lock.notifyAll();
			}
		}
	}

	private static final InterThreadSignal serverHasStarted = new InterThreadSignal();
	private static final InterThreadSignal serverShouldShutDown = new InterThreadSignal();

	// Call from @BeforeClass
	public static void createServer() {
		Thread t = new Thread(new Runnable() {
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
		});
		t.setDaemon(true);
		t.start();
		serverHasStarted.waitForSignal();
	}

	// Call from @AfterClass
	public static void stopServer() {
		serverShouldShutDown.sendSignal();
	}
}
