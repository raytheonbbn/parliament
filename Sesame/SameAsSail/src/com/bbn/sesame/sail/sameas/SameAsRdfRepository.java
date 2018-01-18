// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
// $Id: SameAsRdfRepository.java 747 2009-06-08 19:35:56Z tself $

package com.bbn.sesame.sail.sameas;

import java.util.Map;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.sesame.sail.RdfRepository;
import org.openrdf.sesame.sail.Sail;
import org.openrdf.sesame.sail.SailChangedListener;
import org.openrdf.sesame.sail.SailInitializationException;
import org.openrdf.sesame.sail.SailInternalException;
import org.openrdf.sesame.sail.SailUpdateException;

public class SameAsRdfRepository extends SameAsRdfSource implements
RdfRepository
{
	private RdfRepository _baseSail;

	public SameAsRdfRepository()
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

	@Override
	public void addStatement(Resource subj, URI pred, Value obj)
		throws SailUpdateException
	{
		_baseSail.addStatement(subj, pred, obj);
	}

	@Override
	public int removeStatements(Resource subj, URI pred, Value obj)
		throws SailUpdateException
	{
		return _baseSail.removeStatements(subj, pred, obj);
	}

	@Override
	public void clearRepository() throws SailUpdateException
	{
		_baseSail.clearRepository();
		setBaseSail(_baseSail); // reset cached indices
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
		_baseSail.removeListener(listener);
	}
}
