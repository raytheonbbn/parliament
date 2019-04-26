// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jni;

import java.io.Closeable;
import java.io.OutputStream;
import java.io.PrintStream;

/** Parliament's JNI Interface */
public class KbInstance implements Closeable {
	/**
	 * The return value of the countStmts() method, used to work around Java's
	 * lack of "out" parameters.
	 */
	public static class CountStmtsResult {
		public CountStmtsResult(long total, long numDel, long numInferred,
			long numDelAndInferred, long numHidden, long numVirtual) {
			_total = total;
			_numDel = numDel;
			_numInferred = numInferred;
			_numDelAndInferred = numDelAndInferred;
			_numVirtual = numVirtual;
			_numHidden = numHidden;
		}

		public long getTotal() {
			return _total;
		}

		public long getNumDel() {
			return _numDel;
		}

		public long getNumInferred() {
			return _numInferred;
		}

		public long getNumDelAndInferred() {
			return _numDelAndInferred;
		}

		public long getNumVirtual() {
			return _numVirtual;
		}

		public long getNumHidden(){
			return _numHidden;
		}

		private long _total;
		private long _numDel;
		private long _numInferred;
		private long _numDelAndInferred;
		private long _numVirtual;
		private long _numHidden;
	}

	/**
	 * The return value of the getExcessCapacity() method, used to work around
	 * Java's lack of "out" parameters.
	 */
	public static class GetExcessCapacityResult {
		public GetExcessCapacityResult(double pctUnusedUriCapacity,
			double pctUnusedRsrcCapacity, double pctUnusedStmtCapacity) {
			_pctUnusedUriCapacity = pctUnusedUriCapacity;
			_pctUnusedRsrcCapacity = pctUnusedRsrcCapacity;
			_pctUnusedStmtCapacity = pctUnusedStmtCapacity;
		}

		public double getPctUnusedUriCapacity() {
			return _pctUnusedUriCapacity;
		}

		public double getPctUnusedRsrcCapacity() {
			return _pctUnusedRsrcCapacity;
		}

		public double getPctUnusedStmtCapacity() {
			return _pctUnusedStmtCapacity;
		}

		private double _pctUnusedUriCapacity;
		private double _pctUnusedRsrcCapacity;
		private double _pctUnusedStmtCapacity;
	}

	/** The null statement id (k_nullStmtId in C++). */
	public static final long NULL_STMT_ID = tempInit();

	/** The null resource id (k_nullRsrcId in C++). */
	public static final long NULL_RSRC_ID = tempInit();

	/** Indicates a KB disposition in which some files exist, while others are missing. */
	public static final short INDETERMINATE_KB_STATE = tempInit();

	/** Indicates that the kb does not exist. */
	public static final short KB_DOES_NOT_EXIST = tempInit();

	/** Indicates that the kb exists, but that it is missing the URI-to-int dictionary. */
	public static final short KB_EXISTS_WITHOUT_URI_TO_INT = tempInit();

	/** Indicates that the kb exists. */
	public static final short KB_EXISTS = tempInit();

	/** Instructs a statement iterator to skip deleted statements. */
	public static final int SKIP_DELETED_STMT_ITER_FLAG = tempInit();

	/** Instructs a statement iterator to skip inferred statements. */
	public static final int SKIP_INFERRED_STMT_ITER_FLAG = tempInit();

	/** Instructs a statement iterator to skip literal statements. */
	public static final int SKIP_LITERAL_STMT_ITER_FLAG = tempInit();

	/** Instructs a statement iterator to skip non-literal statements. */
	public static final int SKIP_NON_LITERAL_STMT_ITER_FLAG = tempInit();

	private long m_pKb = 0;

	static {
		System.loadLibrary("Parliament");
		initStatic();
	}

	/**
	 * This method is a hack. It is intended to be used as an initializer for
	 * those constants that are initialized by the native method initStatic().
	 * The reason we don't simply initialize these with zero is so that the
	 * compiler doesn't do constant folding, which undermines the effect of the
	 * initializations performed by initStatic().
	 */
	private static short tempInit() {
		return 0;
	}

	/** Intended only to be called by the KbInstance class ctor. */
	private static native void initStatic();

	/** Creates or opens a KB. */
	public KbInstance(KbConfig config) throws Throwable {
		init(config);
	}

	/** Intended only to be called by the KbInstance instance ctor. */
	private native void init(KbConfig config);

	/**
	 * Frees system resources associated with the KB instance, and closes all KB
	 * files. This is idempotent, so it may be called by application code
	 * whenever the application is finished with the instance. (This is highly
	 * recommended, as an un-finalized instance holds significant system
	 * resources and prevents others from opening the KB.)
	 */
	@Override
	public void close() {
		finalize();
	}

	/**
	 * Frees system resources associated with the KB instance, and closes all KB
	 * files. This is idempotent, so it may be called by application code
	 * whenever the application is finished with the instance. (This is highly
	 * recommended, as an un-finalized instance holds significant system
	 * resources and prevents others from opening the KB.)
	 */
	@Override
	public void finalize() {
		if (m_pKb != 0) {
			try {
				dispose();
			} catch (Throwable ex) {
				// Do nothing
			}

			m_pKb = 0;
		}
	}

	/** Intended only to be called by finalize(). */
	private native void dispose();

	public static native String getVersion();

	/**
	 * Synchronizes with storage, flushing any changes to disk, and does not
	 * return until all changes have been written
	 */
	public native void sync();

	/** Return statistics about the excess capacity */
	public native GetExcessCapacityResult getExcessCapacity();

	/** Release excess capacity */
	public native void releaseExcessCapacity();

	/**
	 * Returns one of the four constants INDETERMINATE_KB_STATE,
	 * KB_DOES_NOT_EXIST, KB_EXISTS_WITHOUT_URI_TO_INT, or KB_EXISTS.
	 */
	public static native short determineDisposition(KbConfig config,
		boolean throwIfIndeterminate);

	/**
	 * Deletes a KB's files. The KB must be closed.
	 *
	 * @param cfg A KbConfig instance from which the method takes the location and
	 * names of the KB files.  If null, the method calls KbConfig.readFile instead.
	 *
	 * @param directory The directory in which the KB files are located.  If this
	 * is null or empty, the directory in the KbConfig parameter is used instead.
	 * If both parameters are null, the directory in the KbConfig instance returned
	 * by KbConfig.readFile is used.
	 */
	public static native void deleteKb(KbConfig cfg, String directory, boolean deleteContainingDir);
	public static void deleteKb(KbConfig cfg, String directory) {
		deleteKb(cfg, directory, false);
	}

	/**
	 * Return the number of statements in the KB (including deleted and
	 * inferred, not including virtual).
	 */
	public native long stmtCount();

	/** Return the number of statements in the KB. */
	public native CountStmtsResult countStmts();

	/** Return the number of resources in the KB. */
	public native long rsrcCount();

	/** Return the average length (in characters) of the resources in the KB. */
	public native double averageRsrcLength();

	/**
	 * Returns an iterator over all of the statements that match the supplied
	 * resource URIs. Pass KbInstance.NULL_RSRC_ID for any resource ID that you
	 * do not wish to constrain.
	 *
	 * To control the kind of statements returned by the iterator, pass a
	 * combination of the flags SKIP_DELETED_STMT_ITER_FLAG,
	 * SKIP_INFERRED_STMT_ITER_FLAG, SKIP_LITERAL_STMT_ITER_FLAG, and
	 * SKIP_NON_LITERAL_STMT_ITER_FLAG combined with the bit-wise or operator.
	 * (It is an error to pass both the literal and non-literal flags, but any
	 * other combination is allowed.)
	 */
	public native StmtIterator find(long subjectId, long predicateId,
		long objectId, int flags);

	/**
	 * Returns the number of statements in which the specified resource occurs
	 * as the subject.
	 */
	public native long subjectCount(long rsrcId);

	/**
	 * Returns the number of statements in which the specified resource occurs
	 * as the predicate.
	 */
	public native long predicateCount(long rsrcId);

	/**
	 * Returns the number of statements in which the specified resource occurs
	 * as the object.
	 */
	public native long objectCount(long rsrcId);

	/** Returns whether a resource is is a literal. */
	public native boolean isRsrcLiteral(long rsrcId);

	/** Returns whether a resource is anonymous. */
	public native boolean isRsrcAnonymous(long rsrcId);

	/** Returns the resource id for a specified URI. */
	public native long uriToRsrcId(String uri, boolean isLiteral, boolean createIfMissing);

	/** Returns the URI associated with a given resource id. */
	public native String rsrcIdToUri(long rsrcId);

	/** Creates and returns a new id for an anonymous resource. */
	public native long createAnonymousRsrc();

	/** Creates a new statement and returns its index. */
	public native long addStmt(long subjectId, long predicateId, long objectId,
		boolean isInferred);

	/** Marks the specified statement as deleted. */
	public native void deleteStmt(long subjectId, long predicateId, long objectId);

	/** Exports the KB in N-Triples format. */
	public native void dumpKbAsNTriples(OutputStream s, boolean includeInferredStmts,
		boolean includeDeletedStmts, boolean useAsciiOnlyEncoding);

	/** Checks the internal structure of the KB. */
	public native boolean validate(PrintStream s);

	/** Prints a textual representation of all the statements. */
	public native void printStatements(PrintStream s, boolean includeNextStmts,
		boolean verboseNextStmts);

	/** Prints a textual representation of all the resources. */
	public native void printResources(PrintStream s, boolean includeFirstStmts,
		boolean verboseFirstStmts);

	/** Returns the number of rules currently in use */
	public native long ruleCount();

	/** Prints a textual representation of all the rules. */
	public native void printRules(PrintStream s);

	/** Prints a textual representation of all the rule triggers. */
	public native void printRuleTriggers(PrintStream s);

	/** Adds a reification of the given statement to the kb.  FOR TESTING PURPOSES ONLY.*/
	public native void addReification(long statementName, long subjectId,
		long predicateId, long objectId);

	/** Removes a reification from the KB.  FOR TESTING PURPOSES ONLY. */
	public native void deleteReification(long statementName, long subjectId,
		long predicateId, long objectId);

	/**
	 * Finds reification quads in the kb, given some combination of
	 * statementName, subject, predicate, and object
	 */
	public native ReificationIterator findReifications(long statementName,
		long subjectId, long predicateId, long objectId);
}
