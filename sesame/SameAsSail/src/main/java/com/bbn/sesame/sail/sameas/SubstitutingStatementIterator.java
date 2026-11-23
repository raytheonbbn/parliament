// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
// $Id: SubstitutingStatementIterator.java 747 2009-06-08 19:35:56Z tself $

package com.bbn.sesame.sail.sameas;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.sesame.sail.StatementIterator;

/** substitute the original subject and/or object in returned statements */
public class SubstitutingStatementIterator implements StatementIterator
{
	private StatementIterator _iterator;
	private Resource          _substitutionSubject;
	private Value             _substitutionObject;

	public SubstitutingStatementIterator(StatementIterator iterator,
		Resource substitutionSubject, Value substitutionObject)
	{
		_iterator = iterator;
		_substitutionSubject = substitutionSubject;
		_substitutionObject = substitutionObject;
	}

	@Override
	public void close()
	{
		_iterator.close();
	}

	@Override
	public boolean hasNext()
	{
		return _iterator.hasNext();
	}

	@Override
	public Statement next()
	{
		Statement statement = _iterator.next();
		if (_substitutionSubject == null && _substitutionObject == null)
		{
			return statement;
		}

		Resource subject = statement.getSubject();
		if (_substitutionSubject != null)
		{
			subject = _substitutionSubject;
		}
		URI predicate = statement.getPredicate();
		Value object = statement.getObject();
		if (_substitutionObject != null)
		{
			object = _substitutionObject;
		}
		return new StatementImpl(subject, predicate, object);
	}
}
