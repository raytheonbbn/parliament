// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
// $Id: KbResource.java 747 2009-06-08 19:35:56Z tself $

package com.bbn.parliament.sesame.sail;

import org.openrdf.model.GraphException;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.sesame.sail.SailUpdateException;
import org.openrdf.sesame.sail.StatementIterator;

import com.bbn.parliament.jni.KbInstance;

public abstract class KbResource extends KbValue implements Resource
{
	private static final long serialVersionUID = 1L;

	public KbResource(KbInstance kb, long index)
	{
		super(kb, index);
	}

	@Override
	public void addProperty(URI property, Value value)
		throws GraphException
	{
		try
		{
			new KbRdfRepository().addStatement(this, property, value);
		}
		catch (SailUpdateException ex)
		{
			throw new GraphException(ex);
		}
	}

	@Override
	public StatementIterator getSubjectStatements()
	{
		return new KbRdfSource().getStatements(this, null, null);
	}
}
