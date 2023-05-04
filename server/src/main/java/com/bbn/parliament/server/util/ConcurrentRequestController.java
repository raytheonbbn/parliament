// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, 2014-2015, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.server.util;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class ConcurrentRequestController {
	private static class ReadLock implements ConcurrentRequestLock {
		public ReadLock() {
			lock.readLock().lock();
		}

		@Override
		public void close() {
			lock.readLock().unlock();
		}
	}

	private static class WriteLock implements ConcurrentRequestLock {
		public WriteLock() {
			lock.writeLock().lock();
		}

		@Override
		public void close() {
			lock.writeLock().unlock();
		}
	}

	private static final boolean FAIR_READ_WRITE_LOCK = true;
	private static ReadWriteLock lock = new ReentrantReadWriteLock(FAIR_READ_WRITE_LOCK);

	private ConcurrentRequestController() {}	// prevents instantiation

	public static ConcurrentRequestLock getReadLock() {
		return new ReadLock();
	}

	public static ConcurrentRequestLock getWriteLock() {
		return new WriteLock();
	}
}
