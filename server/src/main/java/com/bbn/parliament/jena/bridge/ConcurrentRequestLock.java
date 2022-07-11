package com.bbn.parliament.jena.bridge;

/** Used as a return type from ConcurrentRequestController methods. */
public interface ConcurrentRequestLock extends AutoCloseable {
	@Override
	void close();
}
