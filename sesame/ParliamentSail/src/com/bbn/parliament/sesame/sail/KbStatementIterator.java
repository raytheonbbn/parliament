// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.sesame.sail;

import org.openrdf.model.Statement;
import org.openrdf.sesame.sail.StatementIterator;

import com.bbn.parliament.jni.KbInstance;
import com.bbn.parliament.jni.StmtIterator;

public class KbStatementIterator implements StatementIterator
{
	private SyncSail     _syncSail;
	private KbInstance   _kb;
	private StmtIterator _iter;

	KbStatementIterator(KbInstance kb, SyncSail syncSail, StmtIterator iter)
	{
		if (kb == null)
		{
			throw new IllegalArgumentException("kb argument may not be null");
		}
		if (iter == null)
		{
			throw new IllegalArgumentException("iter argument may not be null");
		}

		_kb = kb;
		_syncSail = syncSail;
		_iter = iter;
		if (_syncSail != null)
		{
			_syncSail._getReadLock();
		}
	}

	@Override
	public void close()
	{
		if (_iter != null)
		{
			_iter.finalize();
			_iter = null;
		}
		if (_syncSail != null)
		{
			_syncSail._releaseReadLock();
			_syncSail = null; // just in case we're closed again
		}
	}

	@Override
	public boolean hasNext()
	{
		return _iter.hasNext();
	}

	@Override
	public Statement next()
	{
		return new KbStatement(_kb, _iter.next());
	}
}
