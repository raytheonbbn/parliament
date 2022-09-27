// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.sesame.sail;

public class ReadWriteLock
{
	/** Maps Thread instances to an Integer representing the number of read locks */
	private ThreadLockHashMap _readLocks      = new ThreadLockHashMap();
	private Thread            _writingThread  = null;
	private int               _writeLockCount = 0;

	@SuppressWarnings("unused")
	private void dump(String action)
	{
		System.out.print(Thread.currentThread().getId() + " " + action);
		if (_writingThread == null)
		{
			System.out.print(" Wxx:x");
		}
		else
		{
			System.out.print(
				" W" + _writingThread.getId() + ":" + _writeLockCount);
		}
		_readLocks.dump();
		System.out.println();
	}

	public synchronized void getWriteLock() throws InterruptedException
	{
		//dump("gettingWriteLock---");

		// Flag the thread as wants-to-write before attempting to get the lock.
		// This allows multiple writers who try to upgrade to play nicely with
		// each other even though only one of them will ever get past the first
		// loop at a time.
		_readLocks.setWriting(Thread.currentThread(), true);
		while (_writingThread != null && _writingThread != Thread.currentThread())
		{
			wait();
		}

		if (_writingThread == null)
		{
			_writingThread = Thread.currentThread();
			_writeLockCount = 1;
		}
		else if (_writingThread == Thread.currentThread())
		{
			_writeLockCount++;
		}

		while (!_readLocks.allWriters())
		{
			wait();
		}

		//dump("gotWriteLock-------");
	}

	public synchronized void releaseWriteLock()
	{
		//dump("releasingWriteLock-");
		if (_writingThread != Thread.currentThread())
		{
			throw new IllegalStateException(
				"The calling thread does not own the write lock.");
		}
		_writeLockCount--;
		if (_writeLockCount <= 0)
		{
			_writingThread = null;
			_readLocks.setWriting(Thread.currentThread(), false);
			notifyAll();
		}
		//dump("releasedWriteLock--");
	}

	public synchronized void getReadLock() throws InterruptedException
	{
		//dump("gettingReadLock---");
		// If the thread already has a read lock, allow it to proceed.
		// This should work even if the thread got a read lock after
		// already owning the write lock.
		int count = _readLocks.getLockCount(Thread.currentThread());
		boolean bWriting = false;
		if (count == 0)
		{
			// If another thread is writing or waiting to write, give it priority
			if (_writingThread != null)
			{
				if (_writingThread != Thread.currentThread())
				{
					while (_writingThread != null)
					{
						wait();
					}
				}
				else
				{
					bWriting = true;
				}
			}
		}
		_readLocks.addLock(Thread.currentThread(), bWriting);
		//dump("gotReadLock-------");
	}

	public synchronized void releaseReadLock()
	{
		//dump("releasingReadLock-");
		// No checks should be necessary here - the lock hash will throw
		// the exception itself if there isn't a lock to release.
		int newCount = _readLocks.removeLock(Thread.currentThread());
		if (newCount <= 0)
		{
			notifyAll();
		}
		//dump("releasedReadLock--");
	}

	private static class ThreadLockHashMap
	{
		private static final int DEFAULT_NUM_BUCKETS  = 101;
		private static final int DEFAULT_BUCKET_DEPTH = 4;

		private int              _numBuckets;
		private int              _bucketDepth;
		private Thread[][]       _threads;
		private int[][]          _lockCounts;
		private boolean[][]      _writerFlags;
		private int              _threadCount;
		private int              _writerCount;

		public ThreadLockHashMap()
		{
			this(DEFAULT_NUM_BUCKETS, DEFAULT_BUCKET_DEPTH);
		}

		/**
		 * Constructor that initializes size of hashmap.
		 *
		 * @param numBuckets Number of separate buckets (must be prime).
		 * @param bucketDepth Number of slots within each bucket.
		 */
		public ThreadLockHashMap(int numBuckets, int bucketDepth)
		{
			_numBuckets = numBuckets;
			_bucketDepth = bucketDepth;
			_threads = new Thread[_numBuckets][_bucketDepth];
			_lockCounts = new int[_numBuckets][_bucketDepth];
			_writerFlags = new boolean[_numBuckets][_bucketDepth];
		}

		@SuppressWarnings("unused")
		public void dump()
		{
			System.out.print(" Rx" + _threadCount);
			System.out.print(" Wx" + _writerCount);
			for (int i = 0; i < _numBuckets; i++)
			{
				for (int j = 0; j < _bucketDepth; j++)
				{
					if (_threads[i][j] != null)
					{
						System.out.print(" R" + _threads[i][j].getId() + ":"
							+ _lockCounts[i][j]);
						if (_writerFlags[i][j])
						{
							System.out.print("W");
						}
						else
						{
							System.out.print(" ");
						}
					}
				}
			}
		}

		/**
		 * Indicates whether the hash map contains only threads that are flagged
		 * as writers.
		 *
		 * @return <code>true</code> if all threads in the hash are flagged as
		 *         writers, or if the hash is empty. Otherwise, <code>false</code>.
		 */
		public boolean allWriters()
		{
			return _writerCount == _threadCount;
		}

		/**
		 * Returns whether the hashmap is empty of Threads
		 *
		 * @return <code>true</code> if empty.
		 */
		@SuppressWarnings("unused")
		public boolean isEmpty()
		{
			return _threadCount < 1;
		}

		/**
		 * Returns the number of Threads currently in the hashmap.
		 *
		 * @return Number of threads.
		 */
		@SuppressWarnings("unused")
		public int getSize()
		{
			return _threadCount;
		}

		private int getBucket(Thread t)
		{
			return t.hashCode() % _numBuckets;
		}

		private int getDepth(Thread t, int bucket)
		{
			for (int i = 0; i < _bucketDepth; ++i)
			{
				if (_threads[bucket][i] == t)
				{
					return i;
				}
			}
			return -1;
		}

		/**
		 * Returns the number of locks held by the given thread.
		 *
		 * @param thread Thread being checked for locks.
		 * @return Number of locks.
		 */
		public int getLockCount(Thread thread)
		{
			int bucket = getBucket(thread);
			int depth = getDepth(thread, bucket);
			return (depth < 0)
				? 0
					: _lockCounts[bucket][depth];
		}

		/**
		 * Increments the number of locks held by the given Thread.
		 *
		 * @param thread Thread that is getting the lock.
		 * @throws InterruptedException
		 */
		public void addLock(Thread thread, boolean bWriting)
		{
			int bucket = getBucket(thread);
			int depth = -1;
			int firstNull = _bucketDepth;
			while (depth < 0)
			{
				for (int i = 0; i < _bucketDepth; ++i)
				{
					if (_threads[bucket][i] == null)
					{
						firstNull = Math.min(firstNull, i);
					}
					else if (_threads[bucket][i] == thread)
					{
						depth = i;
						break;
					}
				}
				if (depth < 0)
				{
					if (firstNull < _bucketDepth)
					{
						depth = firstNull;
						_threads[bucket][depth] = thread;
						_lockCounts[bucket][depth] = 0;
						_threadCount++;

						// Using an internal call instead of duplicating logic
						// to ensure consistency about how and when the writer
						// count gets incremented
						setWriting(thread, bWriting);
					}
					else
					{
						// The bucket's full - freak out!
						throw new IllegalStateException(
							"The thread bucket runneth over.");
					}
				}
			}

			++(_lockCounts[bucket][depth]);
		}

		/**
		 * Sets the flag indicating whether or not a thread is currently
		 * attempting to write. Threads should set this flag to indicate that they
		 * are going to attempt to get a write lock, not to indicate that they
		 * have one. This way, deadlock can be avoided in a situation where
		 * multiple threads need to upgrade from readers to writers, but only one
		 * of them may get the write lock at a time.
		 *
		 * @param thread The thread to flag.
		 * @param bIsWriting The value to set.
		 */
		public void setWriting(Thread thread, boolean bIsWriting)
		{
			int bucket = getBucket(thread);
			int depth = getDepth(thread, bucket);
			if (depth < 0)
			{
				// Theoretically, this is an error, but it'll keep things simpler if
				// we can just blindly set every releasing writer thread as a non-writer,
				// even if it doesn't actually exist in the read lock hash.
				return;
			}
			if (bIsWriting != _writerFlags[bucket][depth])
			{
				if (bIsWriting)
				{
					++_writerCount;
				}
				else
				{
					--_writerCount;
				}
			}
			_writerFlags[bucket][depth] = bIsWriting;
		}

		@SuppressWarnings("unused")
		public boolean isWriting(Thread thread)
		{
			int bucket = getBucket(thread);
			int depth = getDepth(thread, bucket);
			return (depth < 0)
				? false
					: _writerFlags[bucket][depth];
		}

		/**
		 * Decrements the number of locks held by the given Thread. If the number
		 * of locks is 0, then the Thread is removed from the hash map.
		 *
		 * @param thread Thread that is giving up the lock.
		 * @return The new lock count for <code>thread</code>.
		 * @throws IllegalStateException if the specified thread does not hold any
		 *            read locks.
		 */
		public int removeLock(Thread thread)
		{
			int bucket = getBucket(thread);
			int depth = getDepth(thread, bucket);
			if (depth < 0)
			{
				throw new IllegalStateException(
					"The specified thread has no lock to release.");
			}

			int newCount = --_lockCounts[bucket][depth];
			if (_lockCounts[bucket][depth] <= 0)
			{
				_threads[bucket][depth] = null;
				_threadCount--;
				if (_writerFlags[bucket][depth])
				{
					_writerCount--;
				}
				_writerFlags[bucket][depth] = false;
			}
			return newCount;
		}
	}
}
