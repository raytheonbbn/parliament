package com.bbn.parliament.jena.joseki.bridge;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.atomic.AtomicBoolean;

import com.bbn.parliament.jena.jetty.JettyServerCore;

public class ParliamentTestServer {
	private static AtomicBoolean errorOcurredDuringServerStart = new AtomicBoolean(false);

	// Call from @BeforeClass
	public static void createServer() {
		try {
			JettyServerCore.initialize();
		} catch(Exception ex) {
			fail(ex.getMessage());
		}
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					JettyServerCore.getInstance().start();
				} catch(Exception ex) {
					errorOcurredDuringServerStart.set(true);
					fail(ex.getMessage());
				}
			}
		});
		t.setDaemon(true);
		t.start();
		while (!JettyServerCore.getInstance().isStarted()) {
			if (errorOcurredDuringServerStart.get()) {
				break;
			}
			try {
				Thread.sleep(250);
			} catch(InterruptedException ex) {
				fail(ex.getMessage());
			}
		}
	}

	// Call from @AfterClass
	public static void stopServer() {
		JettyServerCore.getInstance().stop();
	}
}
