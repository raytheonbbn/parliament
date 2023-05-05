// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.core.jni;

import java.io.Closeable;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * This class is a standard iterator over reifications in a KbInstance. It is
 * instantiated by calling the findReification method on that class. In the
 * context of Java Generics, this class can be safely cast to Iterator&lt;Reification&gt;.
 */
public class ReificationIterator implements Iterator<ReificationIterator.Reification>, Closeable
{
	public static class Reification{
		private long statementName;
		private long subject;
		private long predicate;
		private long object;
		private boolean isLiteral;

		public Reification(long statementName, long subject, long predicate, long object, boolean isLiteral){
			this.statementName = statementName;
			this.subject = subject;
			this.predicate = predicate;
			this.object = object;
			this.isLiteral = isLiteral;
		}

		public long getStatementName() {
			return statementName;
		}

		public long getSubject() {
			return subject;
		}

		public long getPredicate() {
			return predicate;
		}

		public long getObject() {
			return object;
		}

		public boolean isLiteral(){
			return isLiteral;
		}

		@Override
		public String toString(){
			return "["+statementName+","+subject+","+predicate+","+object+"]";
		}
	}

	public static final ReificationIterator EMPTY_ITERATOR = new EmptyIterator();

	private static class EmptyIterator extends ReificationIterator {
		EmptyIterator() {
			super(0l);
		}

		@Override
		public void finalize() {
		}

		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public Reification next() {
			throw new NoSuchElementException("No elements in an empty iterator!");
		}

		@Override
		public void remove() {
		}
	}

	private long m_pIter;

	static
	{
		LibraryLoader.loadLibraries();
	}

	/**
	 * This is private because StmtIterator instances are created only by native
	 * methods on KbInstance.
	 */
	ReificationIterator(long pIter)
	{
		m_pIter = pIter;
	}

	/**
	 * Frees the underlying native iterator associated with the instance. This is
	 * idempotent, so it may be called by application code whenever the
	 * application is finished with the instance.
	 */
	@Override
	public void close() {
		finalize();
	}

	/**
	 * Frees the underlying native iterator associated with the instance. This is
	 * idempotent, so it may be called by application code whenever the
	 * application is finished with the instance.
	 */
	@Override
	public void finalize()
	{
		if (m_pIter != 0)
		{
			long pIter = m_pIter;
			m_pIter = 0;
			try
			{
				dispose(pIter);
			}
			catch (Throwable ex)
			{
				// Do nothing
			}
		}
	}

	/** Intended only to be called by finalize() -- param MUST be m_pIter. */
	private native void dispose(long pIter);

	/** The standard hasNext() method of the Iterator interface. */
	@Override
	public boolean hasNext()
	{
		return hasNext(m_pIter);
	}

	/** Intended only to be called by hasNext() -- param MUST be m_pIter. */
	private native boolean hasNext(long pIter);

	/** Standard next() method. */
	@Override
	public Reification next()
	{
		return nextReification(m_pIter);
	}

	/** Intended only to be called by nextStmtId() -- param MUST be m_pIter. */
	private native Reification nextReification(long pIter);

	/**
	 * The standard remove() method of the Iterator interface. On the
	 * StmtIterator class, this optional method is unimplemented.
	 */
	@Override
	public void remove()
	{
		throw new UnsupportedOperationException(
			"The optional remove operation is unsupported on iterators of type StmtIterator");
	}
}
