// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
// $Id: KbValue.java 747 2009-06-08 19:35:56Z tself $

package com.bbn.parliament.sesame.sail;

import org.openrdf.model.Literal;
import org.openrdf.model.Value;
import org.openrdf.sesame.sail.StatementIterator;

import com.bbn.parliament.jni.KbInstance;

public abstract class KbValue implements Value
{
	private static final long serialVersionUID = 1L;
	private long              _index;
	private KbInstance        _kb;

	protected KbInstance getKb()
	{
		return _kb;
	}

	public KbValue(KbInstance kb, long index)
	{
		_kb = kb;
		_index = index;
	}

	@Override
	@SuppressWarnings("unchecked")
	public int compareTo(Object value)
	{
		if (!(value instanceof KbValue))
		{
			if (value instanceof Value)
			{
				return -(((Value) value).compareTo(this));
			}
			else
			{
				System.err.println("Value.compareTo of "
					+ value.getClass().getName() + " " + value + " to "
					+ getClass().getName() + " " + this);
				return -1;
			}
		}

		long diff = ((KbValue) value)._index - _index;
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
	}

	@Override
	public boolean equals(Object object)
	{
		// The test for null accommodates
		// org.openrdf.rio.rdfxml.RdfXmlWRiter.writeStatement
		return (object == null) ? false : compareTo(object) == 0;
	}

	@Override
	public int hashCode()
	{
		return (int) _index;
	}

	@Override
	public StatementIterator getObjectStatements()
	{
		return new KbRdfSource().getStatements(null, null, this);
	}

	public long getIndex(KbInstance kb)
	{
		// TODO: Note that this method can modify the KB even when it is being
		// called from an ostensibly read-only method, thereby causing a write
		// operation while the caller holds only a read lock.  This is bug
		// #1246 in Bugzilla.
		if (kb != _kb)
		{
			_kb = kb;
			_index = _kb.uriToRsrcId(getKbStringRepresentation(), this instanceof Literal, true);
		}
		return _index;
	}

	public abstract String getKbStringRepresentation();
}
