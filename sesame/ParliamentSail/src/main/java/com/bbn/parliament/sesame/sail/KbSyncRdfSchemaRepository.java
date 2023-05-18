// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
// $Id: KbSyncRdfSchemaRepository.java 747 2009-06-08 19:35:56Z tself $

package com.bbn.parliament.sesame.sail;

import java.util.Map;

import org.openrdf.sesame.sail.Sail;
import org.openrdf.sesame.sail.StackedSail;
import org.openrdf.sesame.sailimpl.sync.SyncRdfSchemaRepository;

/**
 * Allow access to locks from other packages, and ensure
 * that each Thread has at most 1 real read lock.
 */
public class KbSyncRdfSchemaRepository extends SyncRdfSchemaRepository
implements SyncSail
{
	private ReadWriteLock _lock = new ReadWriteLock();

	@Override
	public void initialize(Map configParams)
	{
		Sail sail = getBaseSail();
		while (sail != null)
		{
			if(sail instanceof KbRdfSource)
			{
				((KbRdfSource) sail).setSyncSail(this);
			}
			if(sail instanceof StackedSail)
			{
				sail = ((StackedSail) sail).getBaseSail();
			}
			else
			{
				break;
			}
		}
	}

	@Override
	public void _getWriteLock()
	{
		try
		{
			_lock.getWriteLock();
		}
		catch (InterruptedException ignore)
		{
		}
	}

	@Override
	public void _releaseWriteLock()
	{
		_lock.releaseWriteLock();
	}

	@Override
	public void _getReadLock()
	{
		try
		{
			_lock.getReadLock();
		}
		catch (InterruptedException ignore)
		{
		}
	}

	@Override
	public void _releaseReadLock()
	{
		_lock.releaseReadLock();
	}

	@Override
	public boolean isWriting()
	{
		return transactionStarted();
	}
}
