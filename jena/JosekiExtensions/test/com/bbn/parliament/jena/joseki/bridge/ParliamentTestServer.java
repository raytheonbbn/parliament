package com.bbn.parliament.jena.joseki.bridge;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.atomic.AtomicBoolean;

import com.bbn.parliament.jena.jetty.JettyServerCore;

public class ParliamentTestServer {
	private static boolean timeToExit = false;
	private static final Object lock = new Object();
	private static final AtomicBoolean serverHasStarted = new AtomicBoolean(false);

	// Call from @BeforeClass
	public static void createServer() {
		try {
			JettyServerCore.initialize();
		} catch (Exception ex) {
			fail(ex.getMessage());
		}
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					JettyServerCore.getInstance().start();
					serverHasStarted.set(true);
					synchronized (lock) {
						while (!timeToExit) {
							try {
								lock.wait(5000);
							} catch (InterruptedException ex) {
								// Do nothing
							}
						}
					}
				} catch (Exception ex) {
					serverHasStarted.set(true);
					fail(ex.getMessage());
				} finally {
					JettyServerCore.getInstance().stop();
				}
			}
		});
		t.setDaemon(true);
		t.start();
		do {
			try {
				Thread.sleep(250);
			} catch(InterruptedException ex) {
				fail(ex.getMessage());
			}
		} while (!serverHasStarted.get());
	}

	// Call from @AfterClass
	public static void stopServer() {
		synchronized (lock) {
			timeToExit = true;
			lock.notifyAll();
		}
	}
}
