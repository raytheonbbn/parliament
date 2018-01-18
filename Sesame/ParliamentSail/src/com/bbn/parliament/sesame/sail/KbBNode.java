// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
// $Id: KbBNode.java 747 2009-06-08 19:35:56Z tself $

package com.bbn.parliament.sesame.sail;

import org.openrdf.model.BNode;

import com.bbn.parliament.jni.KbInstance;

public class KbBNode extends KbResource implements BNode
{
	private static final long serialVersionUID = 1L;

	KbBNode(KbInstance kb, long index)
	{
		super(kb, index);
	}

	/** Creates and returns a new anonymous (blank) node in a Parliament instance. */
	public static KbBNode create(KbInstance kb)
	{
		long index = kb.createAnonymousRsrc();
		return new KbBNode(kb, index);
	}

	@Override
	public String getID()
	{
		return "node" + getIndex(getKb());
	}

	@Override
	public String toString()
	{
		return "BNode " + getID();
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
		{
			return true;
		}

		if (o instanceof BNode)
		{
			BNode otherNode = (BNode) o;
			return this.getID().equals(otherNode.getID());
		}

		return false;
	}

	// Implements Object.hashCode()
	@Override
	public int hashCode()
	{
		return getID().hashCode();
	}

	@Override
	public String getKbStringRepresentation()
	{
		throw new RuntimeException("I don't have a String Representation! "
			+ "Thus I will explode.  This presumably happened in the scenario "
			+ "where a bnode reference was used against a different Parliament.");
	}
}
