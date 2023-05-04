package com.bbn.parliament.server.util;

/** Used as a return type from ConcurrentRequestController methods. */
public interface ConcurrentRequestLock extends AutoCloseable {
	@Override
	void close();
}
