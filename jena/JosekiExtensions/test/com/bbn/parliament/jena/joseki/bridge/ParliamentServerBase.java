package com.bbn.parliament.jena.joseki.bridge;

import static org.junit.Assert.fail;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.bbn.parliament.jena.jetty.JettyServerCore;

public class ParliamentServerBase {
	private static final Object lock = new Object();

	private static boolean errorOcurredDuringServerStart = false;

	@BeforeClass
	public static void createServer() {
		try {
			JettyServerCore.initialize();
		} catch(Exception ex) {
			ex.printStackTrace();
			fail();
		}
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					JettyServerCore.getInstance().start();
				} catch(Exception ex) {
					synchronized (lock) {
						errorOcurredDuringServerStart = true;
					}
					ex.printStackTrace();
					fail();
				}
			}
		});
		t.setDaemon(true);
		t.start();
		while (!JettyServerCore.getInstance().isStarted()) {
			synchronized (lock) {
				if (errorOcurredDuringServerStart) {
					break;
				}
			}
			try {
				Thread.sleep(250);
			} catch(InterruptedException ex) {
				ex.printStackTrace();
				fail();
			}
		}
	}

	@AfterClass
	public static void stopServer() {
		JettyServerCore.getInstance().stop();
	}
}
