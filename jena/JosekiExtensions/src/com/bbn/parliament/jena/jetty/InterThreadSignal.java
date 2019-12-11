package com.bbn.parliament.jena.jetty;

// TODO: Replace this class with Java's CountdownLatch
public class InterThreadSignal {
	private boolean signalSent;
	private final Object lock;

	public InterThreadSignal() {
		signalSent = false;
		lock = new Object();
	}

	public void waitForSignal() throws InterruptedException {
		synchronized (lock) {
			while (!signalSent) {
				lock.wait();
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
