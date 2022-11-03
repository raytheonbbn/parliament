// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.utilities;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.jena.rdfxml.xmlinput.ALiteral;
import org.apache.jena.rdfxml.xmlinput.AResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jni.KbInstance;
import com.bbn.parliament.queryoptimization.TreeWidthEstimator;

public class StatementHandler implements org.apache.jena.rdfxml.xmlinput.StatementHandler
{
	private static final File STATEMENTS_FILE = new File("statements.mem");
	private static final File RESOURCES_FILE  = new File("resources.mem");
	private static final File URIS_FILE       = new File("uris.mem");
	private static final File URI_2_ID_FILE   = new File("u2i.db");

	private static Logger     _logger = LoggerFactory.getLogger(TreeWidthEstimator.class);
	private static boolean    _useCache       = true;
	private static long       _stmtCount;
	private static long       _startTime      = System.currentTimeMillis();
	private KbInstance        _kb;
	private AResource         _lastSubject    = null;
	private long              _lastSubjectIndex;
	private Map<String, Long> _predCache      = new HashMap<>();


	public StatementHandler(KbInstance kb)
	{
		if (kb == null)
		{
			throw new NullPointerException("kb argument must be non-null");
		}

		_kb = kb;
	}

	/**
	 * return the Parliament index for a subject, maintaining a "single element
	 * cache" to match consecutive subjects
	 */
	long getSubjectIndex(AResource subj)
	{
		long retval;

		if ((_lastSubject != null) && subj.equals(_lastSubject))
		{
			retval = _lastSubjectIndex;
		}
		else
		{
			retval = getResourceIndex(subj);
		}

		_lastSubject = subj;
		_lastSubjectIndex = retval;

		return retval;
	}

	/**
	 * return the Parliament index for a predicate, caching predicates within a
	 * single document
	 */
	long getPredicateIndex(AResource pred)
	{
		long retval;
		Long value = _predCache.get(pred.toString()); // com.hp.hpl.jena.rdf.arp.URIReference
		// doesn't implement hashCode()
		if (value == null)
		{
			retval = _kb.uriToRsrcId(pred.toString(), false, true);
			_predCache.put(pred.toString(), retval);
		}
		else
		{
			retval = value.longValue();
		}
		return retval;
	}

	/**
	 * returns the Parliament index for a Resource, caching within UserData of
	 * anonymous AResources
	 */
	long getResourceIndex(AResource res)
	{
		long retval;
		if (res.isAnonymous()) // getUserData only supported on anonymous nodes
		{
			Long userData = (Long) res.getUserData();
			if (userData == null)
			{
				retval = _kb.createAnonymousRsrc();
				res.setUserData(retval);
			}
			else
			{
				retval = userData.longValue();
			}
		}
		else
		{
			retval = _kb.uriToRsrcId(res.toString(), false, true);
		}

		return retval;
	}

	long getLiteralIndex(ALiteral lit)
	{
		return _kb.uriToRsrcId(lit.toString(), true, true);
	}

	/**
	 * @see com.hp.hpl.jena.rdf.arp.StatementHandler#statement(AResource,
	 *      AResource, AResource)
	 */
	@Override
	public void statement(AResource subj, AResource pred, AResource obj)
	{
		// local statistics
		_stmtCount++;

		// sanity checks
		if (subj == null)
		{
			_logger.warn("RDF parser - null subject");
			return;
		}

		if (pred == null)
		{
			_logger.warn("RDF parser - null predicate");
			return;
		}

		if (obj == null)
		{
			_logger.warn("RDF parser - null object");
			return;
		}

		if (_useCache)
		{
			_kb.addStmt(getSubjectIndex(subj), getPredicateIndex(pred),
				getResourceIndex(obj), false);
		}
		else
		{
			_kb.addStmt(getResourceIndex(subj), getResourceIndex(pred),
				getResourceIndex(obj), false);
		}

		if ((_stmtCount % 5000 == 0) && _logger.isInfoEnabled())
		{
			logStatistics();
		}
	}

	/**
	 * @see com.hp.hpl.jena.rdf.arp.StatementHandler#statement(AResource,
	 *      AResource, ALiteral)
	 */
	@Override
	public void statement(AResource subj, AResource pred, ALiteral lit)
	{
		// local statistics
		_stmtCount++;

		// sanity checks
		if (subj == null)
		{
			_logger.warn("RDF parser - null subject");
			return;
		}

		if (pred == null)
		{
			_logger.warn("RDF parser - null predicate");
			return;
		}

		if (lit == null)
		{
			_logger.warn("RDF parser - null object");
			return;
		}

		if (_useCache)
		{
			_kb.addStmt(getSubjectIndex(subj), getPredicateIndex(pred),
				getLiteralIndex(lit), false);
		}
		else
		{
			_kb.addStmt(getResourceIndex(subj), getResourceIndex(pred),
				getLiteralIndex(lit), false);
		}

		if ((_stmtCount % 5000 == 0) && _logger.isInfoEnabled())
		{
			logStatistics();
		}
	}

	static public long size(File file)
	{
		long retval = 0;

		if (file.isFile())
		{
			retval = file.length();
		}
		else
		{
			File[] files = file.listFiles();
			if (files != null)
			{
				for (File f : files)
				{
					retval += size(f);
				}
			}
		}
		return retval;
	}

	static public void logStatistics()
	{
		long total = _stmtCount;
		long seconds = (System.currentTimeMillis() - _startTime) / 1000;
		float rate = seconds > 0 ? ((float) total / (float) seconds) : 0;

		long statementBytes = size(STATEMENTS_FILE);
		long resourceBytes = size(RESOURCES_FILE);
		long uriBytes = size(URIS_FILE);
		long u2iBytes = size(URI_2_ID_FILE);
		long totalBytes = statementBytes + resourceBytes + uriBytes + u2iBytes;

		_logger.info("""
			RDF Statements [count: {} time: {}]
			File Sizes [statements: {} resources: {} uris: {} u2i: {} total: {}]
			""",
			_stmtCount, rate, statementBytes, resourceBytes, uriBytes, u2iBytes, totalBytes);
	}
}
