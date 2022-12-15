// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jni;

import java.io.Closeable;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class is a standard iterator over statements in a KbInstance. It is
 * instantiated by calling the begin() and find() methods on that class. In the
 * context of Java Generics, this class can be safely cast to Iterator&lt;Long&gt;.
 *
 * This class maintains a thread-local Boolean flag to facilitate the canceling
 * of queries at a high level.  This facility assumes that each query is handled
 * entirely on a single thread.  This assumption holds true for the current
 * combination of Joseki 3.4.1, Jena 2.6.2, and ARQ 2.8.1.  See the methods
 * resetQueryCanceledFlag and getQueryCanceledFlag for details.
 */
public class StmtIterator implements Iterator<StmtIterator.Statement>, Closeable {
	public static class Statement {
		private long subject;
		private long predicate;
		private long object;
		private boolean isLiteral;
		private boolean isDeleted;
		private boolean isInferred;

		public Statement(long subject, long predicate, long object, boolean isLiteral,
				boolean isDeleted, boolean isInferred) {
			this.subject = subject;
			this.predicate = predicate;
			this.object = object;
			this.isLiteral = isLiteral;
			this.isDeleted = isDeleted;
			this.isInferred = isInferred;
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

		public boolean isLiteral() {
			return isLiteral;
		}

		public boolean isDeleted() {
			return isDeleted;
		}

		public boolean isInferred() {
			return isInferred;
		}

		@Override
		public String toString() {
			return "[%1$d,%2$d,%3$d]".formatted(subject, predicate, object);
		}

		public String toString(KbInstance kb) {
			return "[(%1$d) %2$s,(%3$d) %4$s,(%5$d) %6$s]".formatted(
				subject, kb.rsrcIdToUri(subject),
				predicate, kb.rsrcIdToUri(predicate),
				object, kb.rsrcIdToUri(object));
		}
	}

	private static final ThreadLocal<AtomicBoolean> m_isQueryCanceled =
		new ThreadLocal<>() {
			@Override protected AtomicBoolean initialValue() {
				return new AtomicBoolean(false);
			}
		};

	private long m_pIter;

	static {
		LibraryLoader.loadLibraries();
	}

	/**
	 * This is private because StmtIterator instances are created only by native
	 * methods on KbInstance.
	 */
	private StmtIterator(long pIter) {
		m_pIter = pIter;
	}

	/**
	 * Frees the underlying native iterator associated with the instance. This
	 * is idempotent, so it may be called by application code whenever the
	 * application is finished with the instance.
	 */
	@Override
	public void close() {
		finalize();
	}

	/**
	 * Frees the underlying native iterator associated with the instance. This
	 * is idempotent, so it may be called by application code whenever the
	 * application is finished with the instance.
	 */
	@Override
	public void finalize() {
		if (m_pIter != 0) {
			long pIter = m_pIter;
			m_pIter = 0;
			try {
				dispose(pIter);
			} catch (Throwable ex) {
				// Do nothing
			}
		}
	}

	/** Intended only to be called by finalize() -- param MUST be m_pIter. */
	private native void dispose(long pIter);

	/** The standard hasNext() method of the Iterator interface. */
	@Override
	public boolean hasNext() {
		throwIfCanceled();
		return hasNext(m_pIter);
	}

	/** Intended only to be called by hasNext() -- param MUST be m_pIter. */
	private native boolean hasNext(long pIter);

	/** Similar to next(), but avoids conversions between Long and long. */
	private Statement nextStatement() {
		throwIfCanceled();
		return nextStatement(m_pIter);
	}

	/** Intended only to be called by nextStmtId() -- param MUST be m_pIter. */
	private native Statement nextStatement(long pIter);

	/** The standard next() method of the Iterator interface. */
	@Override
	public Statement next() {
		return nextStatement();
	}

	/**
	 * The standard remove() method of the Iterator interface. On the
	 * StmtIterator class, this optional method is unimplemented.
	 */
	@Override
	public void remove() {
		throw new UnsupportedOperationException(
			"remove() is not supported by com.bbn.parliament.jni.StmtIterator");
	}

	private static void throwIfCanceled() {
		if (m_isQueryCanceled.get().get()) {
			throw new InterruptedStmtIterationException("Statement iteration interrupted");
		}
	}

	/**
	 * Resets the query-canceled flag to "false" so that future calls to StmtIterator
	 * instances on this thread will complete normally.
	 */
	public static void resetQueryCanceledFlag() {
		m_isQueryCanceled.get().set(false);
	}

	/**
	 * Returns the query-canceled flag for the current thread.  If the flag returned by
	 * this method is set to "true", then any subsequent call on this thread to the
	 * hasNext(), next(), or nextStmtId() methods of any StmtIterator instance will
	 * throw an exception of type InterruptedStmtIterationException.
	 *
	 * By caching a reference to this flag in a globally accessible data structure, the
	 * caller can enable other threads to cancel any query running on this one.
	 */
	public static AtomicBoolean getQueryCanceledFlag() {
		return m_isQueryCanceled.get();
	}
}
