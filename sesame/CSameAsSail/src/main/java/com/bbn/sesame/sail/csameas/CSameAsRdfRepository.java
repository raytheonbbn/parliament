// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
// $Id: CSameAsRdfRepository.java 747 2009-06-08 19:35:56Z tself $

package com.bbn.sesame.sail.csameas;

import java.util.Map;

import org.openrdf.model.BNode;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.sesame.sail.RdfRepository;
import org.openrdf.sesame.sail.Sail;
import org.openrdf.sesame.sail.SailChangedListener;
import org.openrdf.sesame.sail.SailInitializationException;
import org.openrdf.sesame.sail.SailInternalException;
import org.openrdf.sesame.sail.SailUpdateException;
import org.openrdf.sesame.sail.StatementIterator;

public class CSameAsRdfRepository extends CSameAsRdfSource implements RdfRepository
{
	private RdfRepository _baseSail;

	public CSameAsRdfRepository()
	{
		super();
	}

	@Override
	public void setBaseSail(Sail baseSail)
	{
		super.setBaseSail(baseSail);
		if (baseSail instanceof RdfRepository)
		{
			_baseSail = (RdfRepository) baseSail;
		}
		else
		{
			throw new SailInternalException(
				"SameAsRdfSource:  base Sail should be an RdfRepository");
		}
	}

	@Override
	public void initialize(Map configParams) throws SailInitializationException
	{
		super.initialize(configParams);
	}

	@Override
	public void startTransaction()
	{
		_baseSail.startTransaction();
	}

	@Override
	public void commitTransaction()
	{
		_baseSail.commitTransaction();
	}

	@Override
	public boolean transactionStarted()
	{
		return _baseSail.transactionStarted();
	}

	// not currently used
	Resource getCanonicalOrInstance(Resource resource)
	{
		Resource canonical = getCanonicalInstance(resource);
		return (canonical == null)
			? resource
				: canonical;
	}

	// not currently used
	Value getCanonicalOrValue(Value value)
	{
		return (value instanceof Resource)
			? getCanonicalOrInstance((Resource) value)
				: value;
	}

	@Override
	public void addStatement(Resource subj, URI pred, Value obj)
		throws SailUpdateException
	{
		if (pred.equals(_sameAs) && !subj.equals(obj))
		{
			Resource csubj = getCanonicalInstance(subj);
			Resource cobj = getCanonicalInstance((Resource) obj);
			if (csubj == null)
			{
				if (cobj == null)
				{
					// pick one
					Resource canonical = (Resource) obj;
					if (canonical instanceof BNode)
					{
						canonical = subj;
					}
					setCanonicalInstance(subj, canonical);
					setCanonicalInstance((Resource) obj, canonical);
				}
				else
				{
					setCanonicalInstance(subj, cobj);
				}
			}
			else
			{
				if (cobj == null)
				{
					setCanonicalInstance((Resource) obj, csubj);
				}
				else if (!csubj.equals(cobj))
				{
					// XXX - pick the smaller for from
					Resource from = csubj;
					Resource to = cobj;

					// move all the objects
					StatementIterator iterator = _baseSail.getStatements(null,
						_canonicalInstance, from);
					try
					{
						while (iterator.hasNext())
						{
							Statement statement = iterator.next();
							setCanonicalInstance(statement.getSubject(), to);
						}
					}
					finally
					{
						iterator.close();
					}
					_baseSail.removeStatements(null, _canonicalInstance, from);

					// XXX - delete copied statements from from
				}
			}
		}
		else if (pred.equals(_differentFrom))
		{
			// XXX - implement
		}

		_baseSail.addStatement(subj, pred, obj);

		if (true) // XXX - if new
		{
			// copy statement to canonical
			Resource csubj = getCanonicalInstance(subj);
			Resource cobj = (obj instanceof Resource)
				? getCanonicalInstance((Resource) obj)
					: null;
				if (csubj != null || cobj != null)
				{
					_baseSail.addStatement((csubj == null) ? subj : csubj, pred,
						(cobj == null) ? obj : cobj);
					// XXX - if new, set flag
				}
		}
	}

	@Override
	public int removeStatements(Resource subj, URI pred, Value obj)
		throws SailUpdateException
	{
		// XXX - check for sameAs?
		return _baseSail.removeStatements(subj, pred, obj);
	}

	@Override
	public void clearRepository() throws SailUpdateException
	{
		_baseSail.clearRepository();
		setBaseSail(_baseSail);    // reset cached indices
	}

	@Override
	public void changeNamespacePrefix(String namespace, String prefix)
		throws SailUpdateException
	{
		_baseSail.changeNamespacePrefix(namespace, prefix);
	}

	@Override
	public void addListener(SailChangedListener listener)
	{
		_baseSail.addListener(listener);
	}

	@Override
	public void removeListener(SailChangedListener listener)
	{
		_baseSail.addListener(listener);
	}

	void setCanonicalInstance(Resource resource, Resource canonical)
		throws SailUpdateException
	{
		_baseSail.addStatement(resource, _canonicalInstance, canonical);

		// copy statements from resource to canonical
		if (!resource.equals(canonical))
		{
			StatementIterator iterator = _baseSail.getStatements(resource, null, null);
			try
			{
				while (iterator.hasNext())
				{
					Statement statement = iterator.next();
					Value obj = statement.getObject();
					Resource cobj = (obj instanceof Resource)
						? getCanonicalInstance((Resource) obj)
							: null;
						_baseSail.addStatement(canonical, statement.getPredicate(),
							(cobj == null) ? obj : cobj);
						// XXX - if new, set flag
				}
			}
			finally
			{
				iterator.close();
			}

			iterator = _baseSail.getStatements(null, null, resource);
			try
			{
				while (iterator.hasNext())
				{
					Statement statement = iterator.next();
					Resource subj = statement.getSubject();
					Resource csubj = getCanonicalInstance(subj);
					_baseSail.addStatement((csubj == null) ? subj : csubj,
						statement.getPredicate(), canonical);
					// XXX - if new, set flag
				}
			}
			finally
			{
				iterator.close();
			}
		}
	}
}
