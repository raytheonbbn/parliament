// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
package com.bbn.parliament.sesame.sail;

import junit.framework.TestCase;

/** @author jlerner */
public class ReadWriteLockTest extends TestCase
{
	ReadWriteLock _lock;

	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
		_lock = new ReadWriteLock();
	}

	@Override
	protected void tearDown() throws Exception
	{
		super.tearDown();
		_lock = null;
	}

	private abstract class WaitingThread extends Thread
	{
		boolean _waiting = false;

		public WaitingThread() {
		}

		public boolean isWaiting()
		{
			return _waiting;
		}

		public void stopWaiting()
		{
			_waiting = false;
		}

		boolean _takeAnother = false;

		public void takeAnother()
		{
			_takeAnother = true;
		}

		int _lockCount = 0;

		@Override
		public void run()
		{
			super.run();
			try
			{
				getLock();
				_lockCount++;
				_waiting = true;
				synchronized (this)
				{
					try
					{
						while (_waiting)
						{
							wait();
							if (_takeAnother)
							{
								_lockCount++;
								_takeAnother = false;
								getLock();
							}
						}
					}
					catch (InterruptedException ignore)
					{

					}
				}
				for (int i = 0; i < _lockCount; i++)
				{
					releaseLock();
				}
			}
			catch (InterruptedException ignore)
			{
			}
		}

		protected abstract void getLock() throws InterruptedException;

		protected abstract void releaseLock();
	}

	class WaitingReaderThread extends WaitingThread
	{

		@Override
		protected void getLock() throws InterruptedException
		{
			_lock.getReadLock();
		}

		@Override
		protected void releaseLock()
		{
			_lock.releaseReadLock();
		}
	}

	class WaitingWriterThread extends WaitingThread
	{
		@Override
		protected void getLock() throws InterruptedException
		{
			_lock.getWriteLock();
		}

		@Override
		protected void releaseLock()
		{
			_lock.releaseWriteLock();
		}
	}

	private class AlternatingThread extends WaitingThread
	{
		boolean _bWriteNext = true;

		/**
		 * Creates an waiting thread that alternates between requesting read locks
		 * and write locks.
		 *
		 * @param bWriteFirst
		 *           <code>true</code> if the first lock requested when the
		 *           thread is started should be a write lock, <code>false</code>
		 *           to start with a read lock.
		 */
		public AlternatingThread(boolean bWriteFirst)
		{
			_bWriteNext = bWriteFirst;
		}

		@Override
		protected void getLock() throws InterruptedException
		{
			boolean bWrite = _bWriteNext;
			_bWriteNext ^= true;
			if (bWrite)
			{
				_lock.getWriteLock();
			}
			else
			{
				_lock.getReadLock();
			}
		}

		@Override
		protected void releaseLock()
		{
			boolean bRead = _bWriteNext;
			_bWriteNext ^= true;
			if (bRead)
			{
				_lock.releaseReadLock();
			}
			else
			{
				_lock.releaseWriteLock();
			}
		}
	}

	private class Reader implements Runnable
	{
		public Reader() {
		}

		@Override
		public void run()
		{
			try
			{
				_lock.getReadLock();
				_lock.releaseReadLock();
			}
			catch (InterruptedException ignore)
			{
			}
		}
	}

	private class Writer implements Runnable
	{
		public Writer() {
		}

		@Override
		public void run()
		{
			try
			{
				_lock.getWriteLock();
				_lock.releaseWriteLock();
			}
			catch (InterruptedException ignore)
			{
			}
		}
	}

	private static void assertDead(Thread t) throws Exception
	{
		t.join(1000);
		assertFalse(t.isAlive());
	}

	private static void assertAlive(Thread t) throws Exception
	{
		t.join(1000);
		assertTrue(t.isAlive());
	}

	public void testMultipleReaderThreads() throws Exception
	{
		WaitingReaderThread wrt = new WaitingReaderThread();
		wrt.start();
		assertAlive(wrt);

		Thread reader = new Thread(new Reader());
		reader.start();
		reader.join(1000);
		assertDead(reader);
		assertAlive(wrt);

		synchronized (wrt)
		{
			wrt.stopWaiting();
			wrt.notifyAll();
		}
		assertDead(wrt);
	}

	public void testReadDuringWrite() throws Exception
	{
		WaitingWriterThread wwt = new WaitingWriterThread();
		wwt.start();
		wwt.join(1000);
		assertAlive(wwt);

		Thread reader = new Thread(new Reader());
		reader.start();
		assertAlive(reader);
		assertAlive(wwt);

		synchronized (wwt)
		{
			wwt.stopWaiting();
			wwt.notifyAll();
		}
		assertDead(wwt);
		assertDead(reader);
	}

	public void testWriteDuringRead() throws Exception
	{
		WaitingReaderThread wrt = new WaitingReaderThread();
		wrt.start();
		assertAlive(wrt);

		Thread writer = new Thread(new Writer());
		writer.start();
		assertAlive(writer);
		assertAlive(wrt);

		synchronized (wrt)
		{
			wrt.stopWaiting();
			wrt.notifyAll();
		}
		assertDead(wrt);
		assertDead(writer);
	}

	public void testMultipleWriterThreads() throws Exception
	{
		WaitingWriterThread wwt = new WaitingWriterThread();
		wwt.start();
		assertAlive(wwt);

		Thread writer = new Thread(new Writer());
		writer.start();
		assertAlive(writer);
		assertAlive(wwt);

		synchronized (wwt)
		{
			wwt.stopWaiting();
			wwt.notifyAll();
		}
		assertDead(wwt);
		assertDead(writer);
	}

	public void testMultipleReadsInOneThread() throws Exception
	{
		WaitingReaderThread wrt = new WaitingReaderThread();
		wrt.start();
		assertAlive(wrt);

		synchronized (wrt)
		{
			wrt.takeAnother();
			wrt.notifyAll();
		}

		// The thread should now be blocked on itself again. If we
		// tell it to stop waiting and re-notify it, it should be
		// able to finish its reads. If it's still blocked, something
		// went wrong with the reentrancy of the read locks.
		synchronized (wrt)
		{
			wrt.stopWaiting();
			wrt.notifyAll();
		}
		assertDead(wrt);
	}

	public void testMultipleWritesInOneThread() throws Exception
	{
		WaitingWriterThread wwt = new WaitingWriterThread();
		wwt.start();
		assertAlive(wwt);

		synchronized (wwt)
		{
			wwt.takeAnother();
			wwt.notifyAll();
		}

		// yadda yadda thread blocked on self, blah blah stop waiting
		// not unblocked = readlock problem etcetera
		synchronized (wwt)
		{
			wwt.stopWaiting();
			wwt.notifyAll();
		}
		assertDead(wwt);
	}

	public void testWriteThenReadInOneThread() throws Exception
	{
		AlternatingThread at = new AlternatingThread(true);
		at.start();
		assertAlive(at);

		synchronized (at)
		{
			at.takeAnother();
			at.notifyAll();
		}

		// The lock should have allowed the read lock since the thread
		// already had the write lock, so it should just be waiting on
		// itself now.
		synchronized (at)
		{
			at.stopWaiting();
			at.notifyAll();
		}
		assertDead(at);
	}

	public void testReadThenWriteInOneThread() throws Exception
	{
		AlternatingThread at = new AlternatingThread(false);
		at.start();
		assertAlive(at);

		synchronized (at)
		{
			at.takeAnother();
			at.notifyAll();
		}

		// The lock should have upgraded to a write lock since the thread
		// already owned the only read lock, so it should just be waiting on
		// itself now.
		synchronized (at)
		{
			at.stopWaiting();
			at.notifyAll();
		}
		assertDead(at);
	}

	public void testMultipleSimultaneousReadToWriteUpgrades() throws Exception
	{
		AlternatingThread readThenWrite1 = new AlternatingThread(false);
		readThenWrite1.start();
		assertAlive(readThenWrite1);

		// We're going to give primer a write lock upgrade and then make sure that
		// additional attempted upgrades behave properly, so let's get a couple
		// more
		// read locks going...
		AlternatingThread readThenWrite2 = new AlternatingThread(false);
		readThenWrite2.start();
		assertAlive(readThenWrite2);
		AlternatingThread bookworm = new AlternatingThread(false);
		bookworm.start();
		assertAlive(bookworm);

		// Now, readThenWrite1 asks for a write lock, which should put it first in
		// line. However, we
		// still have two ordinary readers, so it's not going to get its lock yet.
		synchronized (readThenWrite1)
		{
			readThenWrite1.takeAnother();
			readThenWrite1.notifyAll();
		}
		// Note that this means the thread is currently waiting on the
		// ReadWriteLock while synchronized
		// on itself, so we won't be synchronizing with it externally until it's
		// back in a state where
		// the lock is expected to have been released. Unfortunately, this also
		// means no isAlive checks,
		// because those synchronize on the thread, but making sure everything has
		// terminated properly
		// at the end of the tests shouls be sufficient.

		// Attempt to upgrade readThenWrite2, too. All threads should remain
		// alive, since there's
		// still an ordinary reader holding a lock.
		synchronized (readThenWrite2)
		{
			readThenWrite2.takeAnother();
			readThenWrite2.notifyAll();
		}

		// Now, we stop both read-then-writers from waiting, but they should both
		// remain alive
		// since they're still waiting on their write locks.
		// We don't synchronize and notify in this case because we expect that the
		// threads are currently hung
		// waiting for a lock, so they're already synchronized on themselves
		readThenWrite1.stopWaiting();
		readThenWrite2.stopWaiting();

		// ...but the moment we let the bookworm finish reading, the first writer
		// thread should
		// be able to get its lock and terminate, which in turn allows the second
		// writer to
		// do the same.
		synchronized (bookworm)
		{
			bookworm.stopWaiting();
			bookworm.notifyAll();
		}
		assertDead(readThenWrite1);
		assertDead(readThenWrite2);
		assertDead(bookworm);
	}
}
