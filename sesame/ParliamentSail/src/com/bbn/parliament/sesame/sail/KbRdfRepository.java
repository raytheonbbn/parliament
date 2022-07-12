// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
// $Id: KbRdfRepository.java 747 2009-06-08 19:35:56Z tself $

package com.bbn.parliament.sesame.sail;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.openrdf.model.BNode;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.sesame.sail.RdfRepository;
import org.openrdf.sesame.sail.SailChangedListener;
import org.openrdf.sesame.sail.SailInitializationException;
import org.openrdf.sesame.sail.SailUpdateException;
import org.openrdf.sesame.sail.StatementIterator;

import com.bbn.parliament.jni.KbInstance;

public class KbRdfRepository extends KbRdfSource implements RdfRepository
{
	protected boolean _transactionStarted;
	private Map<Object, KbBNode> _bnodes = null;
	private int _adds = 0;

	public KbRdfRepository()
	{
		super();
	}

	@Override
	public void initialize(Map configParams) throws SailInitializationException
	{
		super.initialize(configParams);
	}

	@Override
	public void startTransaction()
	{
		_bnodes = new HashMap<>();
		_adds = 0;
		_transactionStarted = true;
	}

	@Override
	public void commitTransaction()
	{
		if(_adds > 0)
		{
			@SuppressWarnings("resource")
			KbInstance kb = getKb();
			kb.sync();
		}

		_transactionStarted = false;
		_bnodes = null;
	}

	@Override
	public boolean transactionStarted()
	{
		return _transactionStarted;
	}

	/**
	 * read/write version of toLong, accommodating mappings from BNodes
	 */
	long toLongRW(Object object)
	{
		if((object instanceof BNode) && (!(object instanceof Value)))
		{
			KbBNode bnode = _bnodes.get(object);
			if(bnode == null)
			{
				@SuppressWarnings("resource")
				KbInstance kb = getKb();
				bnode = new KbBNode(kb, kb.createAnonymousRsrc());
				_bnodes.put(object, bnode);
			}
			return toLong(bnode, true);
		}
		else
		{
			return toLong(object, true);
		}
	}

	@Override
	public void addStatement(Resource subj, URI pred, Value obj)
		throws SailUpdateException
	{
		if(!transactionStarted())
		{
			throw new SailUpdateException("no transaction started.");
		}

		long s = toLongRW(subj);
		long p = toLongRW(pred);
		long o = toLongRW(obj);

		@SuppressWarnings("resource")
		KbInstance kb = getKb();
		kb.addStmt(s, p, o, false);

		++_adds;
	}

	@Override
	public int removeStatements(Resource subj, URI pred, Value obj)
		throws SailUpdateException
	{
		if(!transactionStarted())
		{
			throw new SailUpdateException("no transaction started.");
		}

		// jlerner, 10/27/06 - Fixing the temporary removal of the syncSail here
		// to be consistent with the other place where this occurred, in
		// changeNamespacePrefix, which was the suspected cause of bug #98.

		//// avoid blocking on read lock within write lock
		//SyncSail oldSyncSail = syncSail;
		//syncSail = null;

		int count = 0;
		KbStatementIterator iterator = (KbStatementIterator) getStatements(
			subj, pred, obj);
		try
		{
			while (iterator.hasNext())
			{
				KbStatement statement = (KbStatement) iterator.next();
				@SuppressWarnings("resource")
				KbInstance kb = getKb();
				kb.deleteStmt(
					statement.getUnderlyingStatement().getSubject(),
					statement.getUnderlyingStatement().getPredicate(),
					statement.getUnderlyingStatement().getObject());
				++count;
			}
		}
		finally
		{
			//syncSail = oldSyncSail;
			iterator.close();
		}
		return count;
	}

	void deleteFile(String path)
	{
		File file = new File(getDirectory(), path);
		boolean status = file.delete();
		System.out.println((status ? "deleted " : "didn't delete ") + file);
	}

	@Override
	public void clearRepository() throws SailUpdateException
	{
		if(!transactionStarted())
		{
			throw new SailUpdateException("no transaction started.");
		}

		getKb().finalize();
		KbInstance.deleteKb(getConfig(), getDirectory());

		try
		{
			openKb(getDirectory(), true);
		}
		catch (Throwable t)
		{
			throw new SailUpdateException(t);
		}
	}

	@Override
	public void changeNamespacePrefix(String namespace, String prefix)
		throws SailUpdateException
	{
		if(!transactionStarted())
		{
			throw new SailUpdateException("no transaction started.");
		}

		KbLiteral prefixLiteral = KbLiteral.create(getKb(), prefix);
		KbUri namespacePrefix = KbUri.create(getKb(), NAMESPACE_ONT + "#prefix");
		KbUri namespaceURI = KbUri.create(getKb(), namespace);
		KbUri namespaceNamespace = KbUri.create(getKb(), NAMESPACE_ONT + "#Namespace");

		// jlerner, 10/27/06 - I'm undoing this temporary syncSail unset because
		// the new SWMR lock directly supports reading during a write lock,
		// and I believe the temporary removal of the syncSail reference is
		// causing a bug (#98 in bugzilla) where a namespace prefix will
		// occasionally get assigned to two different URIs.

		//// avoid blocking on read lock within write lock
		//SyncSail oldSyncSail = syncSail;
		//syncSail = null;

		StatementIterator iterator = getStatements(null, namespacePrefix, prefixLiteral);
		try
		{
			while (iterator.hasNext())
			{
				Statement statement = iterator.next();
				if(statement.getSubject().equals(namespaceURI))
				{
					return;  // prefix/namespace pair already stored
				}
				else
				{
					throw new SailUpdateException("namespace prefix " + prefix
						+ " already used for " + statement.getSubject());
				}
			}
		}
		finally
		{
			//syncSail = oldSyncSail;
			iterator.close();
		}

		KbUri rdfType = KbUri.create(getKb(), "http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		addStatement(namespaceURI, rdfType, namespaceNamespace);
		addStatement(namespaceURI, namespacePrefix, prefixLiteral);
	}

	// Sesame 1.2
	@Override
	public void addListener(SailChangedListener listener)
	{
		// unimplemented
	}

	@Override
	public void removeListener(SailChangedListener listener)
	{
		// unimplemented
	}
}
