// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
// $Id: StatementIteratorIterator.java 747 2009-06-08 19:35:56Z tself $

package com.bbn.sesame.sail.sameas;

import java.util.Iterator;
import java.util.List;

import org.openrdf.model.Statement;
import org.openrdf.sesame.sail.StatementIterator;

/** provide a single StatementIterator over several component StatementIterators */
public class StatementIteratorIterator implements StatementIterator
{
	private List<StatementIterator>     _vector;
	private Iterator<StatementIterator> _vectorIterator;
	private StatementIterator           _currentIterator = null;
	private Statement                   _next            = null;

	public StatementIteratorIterator(List<StatementIterator> iterators)
	{
		_vector = iterators;
		_vectorIterator = _vector.iterator();
	}

	@Override
	public void close()
	{
		for (StatementIterator iter : _vector)
		{
			iter.close();
		}
	}

	Statement findNext()
	{
		if (_next != null)
		{
			return _next;
		}

		while (true)
		{
			if (_currentIterator != null && _currentIterator.hasNext())
			{
				_next = _currentIterator.next();
				return _next;
			}
			else if (_vectorIterator.hasNext())
			{
				_currentIterator = _vectorIterator.next();
			}
			else
			{
				return null;
			}
		}
	}

	@Override
	public boolean hasNext()
	{
		return findNext() != null;
	}

	@Override
	public Statement next()
	{
		Statement retval = findNext();
		_next = null;
		return retval;
	}
}
