// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jni;

import java.util.concurrent.TimeUnit;

public class KbConfig {
	/** Initializes a KbConfig instance with default configuration values */
	public KbConfig() {
		init();
	}

	/** Intended only to be called by the KbConfig ctor. */
	private native void init();

	/**
	 * Creates a KbConfig instance initialized according to the content of the
	 * configuration file
	 */
	public native void readFromFile();

	/** Directory containing the Parliament KB files */
	public String  m_kbDirectoryPath;

	/** Name of the memory-mapped statement file */
	public String  m_stmtFileName;

	/** Name of the memory-mapped resource file */
	public String  m_rsrcFileName;

	/** Name of the memory-mapped URI string file */
	public String  m_uriTableFileName;

	/** Name of the Berkeley DB URI-to-rsrcId file */
	public String  m_uriToIntFileName;

	/** Whether to open the KB in read-only mode */
	public boolean m_readOnly;

	/**
	 * The number of milliseconds between asynchronous sync's of the KB files
	 * to disk.  Set to zero to disable flushing the files to disk.
	 */
	public long    m_fileSyncTimerDelay;

	/** The initial # of resources for which space should be allocated */
	public long    m_initialRsrcCapacity;

	/** The expected average URI length */
	public long    m_avgRsrcLen;

	/**
	 * The increment by which the resource capacity should be grown whenever more
	 * space is required
	 */
	public long    m_rsrcGrowthIncrement;

	/**
	 * The factor by which the resource capacity should be grown whenever more
	 * space is required
	 */
	public double  m_rsrcGrowthFactor;

	/** The initial # of statements for which space should be allocated */
	public long    m_initialStmtCapacity;

	/**
	 * The increment by which the statement capacity should be grown whenever more
	 * space is required
	 */
	public long    m_stmtGrowthIncrement;

	/**
	 * The factor by which the statement capacity should be grown whenever more
	 * space is required
	 */
	public double  m_stmtGrowthFactor;

	/**
	 * The amount of memory to be devoted to the Berkeley DB cache.  The portion before
	 * the comma is the total cache size (with a k for kilobytes, m for megabytes, g for
	 * gigabytes).  The portion after the comma specifies how many segments the memory
	 * should be broken across, for compatibility with systems that limit the size of
	 * single memory allocations.
	 */
	public String  m_bdbCacheSize;

	/** Whether to translate typed string literals to plain literals */
	public boolean m_normalizeTypedStringLiterals;

	/** How long a query should be allowed to run before being aborted */
	public long m_timeoutDuration;

	/** The units for m_timeoutDuration */
	public TimeUnit m_timeoutUnit;
	public String getTimeoutUnit() {
		return m_timeoutUnit.toString();
	}
	public void setTimeoutUnit(String newValue) {
		m_timeoutUnit = TimeUnit.valueOf(newValue);
	}

	/** Whether to ensure all entailments at startup, or assume entailments
	 * are correct from previous runs */
	public boolean m_runAllRulesAtStartup;

	/** Whether the SWRL rule engine is enabled */
	public boolean m_enableSWRLRuleEngine;

	/**
	 * Determines whether the following rules are turned on:
	 * <ul>
	 * <li>"A is a subclass of B" ^ "B is a subclass of C" ==&gt; "A is a
	 * subclass of C"</li>
	 * <li>"X is of type A" ^ "A is a subclass of B" ==&gt; "X is of type B"</li>
	 * <li>If inferRdfsClass is true: "A is a subclass of B" ==&gt; "A and B are
	 * of type rdfs:Class"</li>
	 * <li>If inferOwlClass is true: "A is a subclass of B" ==&gt; "A and B are
	 * of type owl:Class"</li>
	 * <li>If inferRdfsResource is true: "A is a subclass of B" ==&gt; "A and B
	 * are subclasses of rdfs:Resource"</li>
	 * <li>If inferOwlThing is true: "A is a subclass of B" ==&gt; "A and B are
	 * subclasses of owl:Thing"</li>
	 * </ul>
	 */
	public boolean m_isSubclassRuleOn;

	/**
	 * Determines whether the following rules are turned on:
	 * <ul>
	 * <li>"P is a subproperty of Q" ^ "Q is a subproperty of R" ==&gt; "P is a
	 * subproperty of R"</li>
	 * <li>"P is a subproperty of Q" ^ "P(X, V)" ==&gt; "Q(X, V)"</li>
	 * </ul>
	 */
	public boolean m_isSubpropertyRuleOn;

	/** Whether the Domain inference rule is turned on */
	public boolean m_isDomainRuleOn;

	/** Whether the Range inference rule is turned on */
	public boolean m_isRangeRuleOn;

	/** Whether the EquivalentClass inference rule is turned on */
	public boolean m_isEquivalentClassRuleOn;

	/** Whether the EquivalentProperty inference rule is turned on */
	public boolean m_isEquivalentPropRuleOn;

	/** Whether the InverseOf inference rule is turned on */
	public boolean m_isInverseOfRuleOn;

	/** Whether the Symmetric Property inference rule is turned on */
	public boolean m_isSymmetricPropRuleOn;

	/** Whether the Functional Property inference rule is turned on */
	public boolean m_isFunctionalPropRuleOn;

	/**
	 * Determines whether the following rule is turned on:
	 * "P is an inverse functional property" ^ "P(X, V)" ^ "P(Y, V)" ==&gt; "owl:sameAs(X, Y)"
	 */
	public boolean m_isInvFunctionalPropRuleOn;

	/** Whether the Transitive Property inference rule is turned on */
	public boolean m_isTransitivePropRuleOn;

	/** Whether to infer rdfs:Class based on subclass statements */
	public boolean m_inferRdfsClass;

	/** Whether to infer owl:Class based on subclass statements */
	public boolean m_inferOwlClass;

	/** Whether to infer rdfs:Resource based on subclass statements */
	public boolean m_inferRdfsResource;

	/** Whether to infer owl:Thing based on subclass statements */
	public boolean m_inferOwlThing;

	public void disableAllRules() {
		m_enableSWRLRuleEngine = false;
		m_isSubclassRuleOn = false;
		m_isSubpropertyRuleOn = false;
		m_isDomainRuleOn = false;
		m_isRangeRuleOn = false;
		m_isEquivalentClassRuleOn = false;
		m_isEquivalentPropRuleOn = false;
		m_isInverseOfRuleOn = false;
		m_isSymmetricPropRuleOn = false;
		m_isFunctionalPropRuleOn = false;
		m_isInvFunctionalPropRuleOn = false;
		m_isTransitivePropRuleOn = false;
		m_inferRdfsClass = false;
		m_inferOwlClass = false;
		m_inferRdfsResource = false;
		m_inferOwlThing = false;
	}

	static {
		LibraryLoader.loadLibraries();
	}
}
