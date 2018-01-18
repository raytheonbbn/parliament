// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
// $Id: DistinctStatementIterator.java 747 2009-06-08 19:35:56Z tself $

package com.bbn.sesame.sail.csameas;

import java.util.HashSet;
import java.util.Set;

import org.openrdf.model.Statement;
import org.openrdf.sesame.sail.StatementIterator;

/** remove duplicate Statements returned by the encapsulated iterator. */
public class DistinctStatementIterator implements StatementIterator
{
	private StatementIterator  _iterator;
	private Set<Statement>     _statements;
	private Statement          _next;

	public DistinctStatementIterator(StatementIterator iterator)
	{
		_iterator   = iterator;
		_statements = new HashSet<>();
		_next       = null;
	}

	@Override
	public void close()
	{
		_iterator.close();
	}

	Statement findNext()
	{
		if (_next != null)
		{
			return _next;
		}

		while (_iterator.hasNext())
		{
			Statement statement = _iterator.next();
			if (!_statements.contains(statement))
			{
				_statements.add(statement);
				_next = statement;
				return statement;
			}
		}
		return null;
	}

	@Override
	public boolean hasNext()
	{
		return (findNext() != null);
	}

	@Override
	public Statement next()
	{
		Statement retval = findNext();
		_next = null;
		return retval;
	}
}
