// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
// $Id: CanonicalStatementIterator.java 747 2009-06-08 19:35:56Z tself $

package com.bbn.sesame.sail.csameas;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.Value;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.sesame.sail.StatementIterator;

public class CanonicalStatementIterator implements StatementIterator
{
	private StatementIterator _iterator;
	private CSameAsRdfSource  _source;
	private boolean           _canonicalizeSubject;
	private boolean           _canonicalizeObject;

	public CanonicalStatementIterator(StatementIterator iterator,
		CSameAsRdfSource source, boolean canonicalizeSubject,
		boolean canonicalizeObject)
	{
		_iterator = iterator;
		_source = source;
		_canonicalizeSubject = canonicalizeSubject;
		_canonicalizeObject = canonicalizeObject;
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
		boolean replace = false;
		Statement statement = _iterator.next();
		Resource subject;
		Value object;

		subject = statement.getSubject();
		if (_canonicalizeSubject)
		{
			Resource canonical = _source.getCanonicalInstance(subject);
			if (canonical != null)
			{
				subject = canonical;
				replace = true;
			}
		}

		object = statement.getObject();
		if (_canonicalizeObject)
		{
			if (object instanceof Resource)
			{
				Resource canonical = _source.getCanonicalInstance((Resource) object);
				if (canonical != null)
				{
					object = canonical;
					replace = true;
				}
			}
		}

		return replace
			? new StatementImpl(subject, statement.getPredicate(), object)
			: statement;
	}
}
