// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
// $Id: KbStatement.java 747 2009-06-08 19:35:56Z tself $

package com.bbn.parliament.sesame.sail;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

import com.bbn.parliament.jni.KbInstance;
import com.bbn.parliament.jni.StmtIterator;

public class KbStatement implements Statement
{
	private static final long serialVersionUID = 1L;

	private KbInstance					_kb;
	private StmtIterator.Statement	_statement;

	KbStatement(KbInstance kb, StmtIterator.Statement statement)
	{
		_kb = kb;
		_statement = statement;
	}

	@Override
	public Value getObject()
	{
		long object = _statement.getObject();
		if (_statement.isLiteral())
		{
			return new KbLiteral(_kb, object);
		}
		else if (_kb.isRsrcAnonymous(object))
		{
			return new KbBNode(_kb, object);
		}
		else
		{
			return new KbUri(_kb, object);
		}
	}

	@Override
	public URI getPredicate()
	{
		return new KbUri(_kb, _statement.getPredicate());
	}

	@Override
	public Resource getSubject()
	{
		long subject = _statement.getSubject();
		return _kb.isRsrcAnonymous(subject)
			? new KbBNode(_kb, subject)
			: new KbUri(_kb, subject);
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public int compareTo(Object statement)
	{
		// assume another implementation is more complete than ours
		if (!(statement instanceof KbStatement))
		{
			return -((Comparable) statement).compareTo(this);
		}

		StmtIterator.Statement other = ((KbStatement)statement).getUnderlyingStatement();
		long x = 0;
		if ((x = other.getSubject() - _statement.getSubject()) != 0){
			return Math.round(x / Math.abs(x));
		}
		if ((x = other.getPredicate() - _statement.getPredicate()) != 0){
			return Math.round(x / Math.abs(x));
		}
		if ((x = other.getObject() - _statement.getObject()) != 0){
			return Math.round(x / Math.abs(x));
		}
		return 0;

		/*
		long diff = ((KbStatement) statement)._index - _index;
		if (diff < 0)
		{
			return -1;
		}
		else if (diff > 0)
		{
			return 1;
		}
		else
		{
			return 0;
		}
		 */
	}

	@Override
	public boolean equals(Object object)
	{
		return (object == null) ? false : compareTo(object) == 0;
	}

	@Override
	public int hashCode()
	{
		return ("seed"+_statement.getSubject()+_statement.getPredicate()+_statement.getObject()).hashCode();
	}

	public StmtIterator.Statement getUnderlyingStatement()
	{
		return _statement;
	}

	@Override
	public String toString()
	{
		return "(" + getSubject() + ", " + getPredicate() + ", " + getObject() + ")";
	}
}
