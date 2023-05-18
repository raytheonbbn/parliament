// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
package com.bbn.parliament.sesame.sail;

import java.util.Map;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.sesame.sail.LiteralIterator;
import org.openrdf.sesame.sail.NamespaceIterator;
import org.openrdf.sesame.sail.RdfSchemaRepository;
import org.openrdf.sesame.sail.Sail;
import org.openrdf.sesame.sail.SailChangedListener;
import org.openrdf.sesame.sail.SailInitializationException;
import org.openrdf.sesame.sail.SailUpdateException;
import org.openrdf.sesame.sail.StackedSail;
import org.openrdf.sesame.sail.StatementIterator;
import org.openrdf.sesame.sail.query.Query;

/**
 * A SAIL that can be forced to wait on an instance of the inner class Wait.
 *
 * @author jlerner
 */
public class WaitingSchemaSail implements StackedSail, RdfSchemaRepository
{
	public static class Wait
	{
		private boolean _waiting = true;

		public boolean isWaiting()
		{
			return _waiting;
		}

		public void stopWaiting()
		{
			_waiting = false;
		}
	}

	private RdfSchemaRepository _baseSail;
	private Wait                _wait;

	public WaitingSchemaSail()
	{
		super();
	}

	@Override
	public void setBaseSail(Sail baseSail)
	{
		_baseSail = (RdfSchemaRepository) baseSail;
	}

	@Override
	public Sail getBaseSail()
	{
		return _baseSail;
	}

	private void takeANumber()
	{
		if (_wait != null)
		{
			synchronized (_wait)
			{
				// Make sure our wait var doesn't get nulled out on us before we're
				// notified
				Wait threadWait = _wait;
				try
				{
					while (threadWait.isWaiting())
					{
						threadWait.wait();
					}
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void initialize(Map configParams) throws SailInitializationException
	{
	}

	@Override
	public void shutDown()
	{
		_baseSail.shutDown();
	}

	@Override
	public void addListener(SailChangedListener listener)
	{
		_baseSail.addListener(listener);
	}

	@Override
	public void addStatement(Resource subj, URI pred, Value obj)
		throws SailUpdateException
	{
		takeANumber();
		_baseSail.addStatement(subj, pred, obj);
	}

	@Override
	public void changeNamespacePrefix(String namespace, String prefix)
		throws SailUpdateException
	{
		_baseSail.changeNamespacePrefix(namespace, prefix);
	}

	@Override
	public void clearRepository() throws SailUpdateException
	{
		takeANumber();
		_baseSail.clearRepository();
	}

	@Override
	public void commitTransaction()
	{
		_baseSail.commitTransaction();
	}

	@Override
	public NamespaceIterator getNamespaces()
	{
		return _baseSail.getNamespaces();
	}

	@Override
	public StatementIterator getStatements(Resource subj, URI pred, Value obj)
	{
		takeANumber();
		return _baseSail.getStatements(subj, pred, obj);
	}

	@Override
	public ValueFactory getValueFactory()
	{
		return _baseSail.getValueFactory();
	}

	@Override
	public boolean hasStatement(Resource subj, URI pred, Value obj)
	{
		return _baseSail.hasStatement(subj, pred, obj);
	}

	@Override
	public Query optimizeQuery(Query qc)
	{
		return _baseSail.optimizeQuery(qc);
	}

	@Override
	public void removeListener(SailChangedListener listener)
	{
		_baseSail.removeListener(listener);
	}

	@Override
	public int removeStatements(Resource subj, URI pred, Value obj)
		throws SailUpdateException
	{
		takeANumber();
		return _baseSail.removeStatements(subj, pred, obj);
	}

	@Override
	public void startTransaction()
	{
		_baseSail.startTransaction();
	}

	@Override
	public boolean transactionStarted()
	{
		return _baseSail.transactionStarted();
	}

	public Wait getWaitObject()
	{
		return _wait;
	}

	public void setWaitObject(Wait wait)
	{
		_wait = wait;
	}

	@Override
	public StatementIterator getClasses()
	{
		return _baseSail.getClasses();
	}

	@Override
	public StatementIterator getDirectSubClassOf(Resource subClass,
		Resource superClass)
	{
		return _baseSail.getDirectSubClassOf(subClass, superClass);
	}

	@Override
	public StatementIterator getDirectSubPropertyOf(Resource subProperty,
		Resource superProperty)
	{
		return _baseSail.getDirectSubPropertyOf(subProperty, superProperty);
	}

	@Override
	public StatementIterator getDirectType(Resource anInstance, Resource aClass)
	{
		return _baseSail.getDirectType(anInstance, aClass);
	}

	@Override
	public StatementIterator getDomain(Resource prop, Resource domain)
	{
		return _baseSail.getDomain(prop, domain);
	}

	@Override
	public StatementIterator getExplicitStatements(Resource subj, URI pred,
		Value obj)
	{
		return _baseSail.getExplicitStatements(subj, pred, obj);
	}

	@Override
	public LiteralIterator getLiterals(String label, String language,
		URI datatype)
	{
		return _baseSail.getLiterals(label, language, datatype);
	}

	@Override
	public StatementIterator getProperties()
	{
		return _baseSail.getProperties();
	}

	@Override
	public StatementIterator getRange(Resource prop, Resource range)
	{
		return _baseSail.getRange(prop, range);
	}

	@Override
	public StatementIterator getSubClassOf(Resource subClass, Resource superClass)
	{
		return _baseSail.getSubClassOf(subClass, superClass);
	}

	@Override
	public StatementIterator getSubPropertyOf(Resource subProperty,
		Resource superProperty)
	{
		return _baseSail.getSubPropertyOf(subProperty, superProperty);
	}

	@Override
	public StatementIterator getType(Resource anInstance, Resource aClass)
	{
		return _baseSail.getType(anInstance, aClass);
	}

	@Override
	public boolean hasExplicitStatement(Resource subj, URI pred, Value obj)
	{
		return _baseSail.hasExplicitStatement(subj, pred, obj);
	}

	@Override
	public boolean isClass(Resource resource)
	{
		return _baseSail.isClass(resource);
	}

	@Override
	public boolean isDirectSubClassOf(Resource subClass, Resource superClass)
	{
		return _baseSail.isDirectSubClassOf(subClass, superClass);
	}

	@Override
	public boolean isDirectSubPropertyOf(Resource subProperty,
		Resource superProperty)
	{
		return _baseSail.isDirectSubPropertyOf(subProperty, superProperty);
	}

	@Override
	public boolean isDirectType(Resource anInstance, Resource aClass)
	{
		return _baseSail.isDirectType(anInstance, aClass);
	}

	@Override
	public boolean isProperty(Resource resource)
	{
		return _baseSail.isProperty(resource);
	}

	@Override
	public boolean isSubClassOf(Resource subClass, Resource superClass)
	{
		return _baseSail.isSubClassOf(subClass, superClass);
	}

	@Override
	public boolean isSubPropertyOf(Resource subProperty, Resource superProperty)
	{
		return _baseSail.isSubPropertyOf(subProperty, superProperty);
	}

	@Override
	public boolean isType(Resource anInstance, Resource aClass)
	{
		return _baseSail.isType(anInstance, aClass);
	}
}
