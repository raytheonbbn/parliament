// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
// $Id: KbLiteral.java 747 2009-06-08 19:35:56Z tself $

package com.bbn.parliament.sesame.sail;

import org.openrdf.model.Literal;
import org.openrdf.model.URI;

import com.bbn.parliament.core.jni.KbInstance;

public class KbLiteral extends KbValue implements Literal
{
	private static final long serialVersionUID = 1L;

	public KbLiteral(KbInstance kb, long index)
	{
		super(kb, index);
	}

	/**
	 * Creates and returns an instance of Literal for the specified value. The
	 * literal will be added to the supplied Parliament if it does not already exist.
	 *
	 * @param kb The Parliament instance to query
	 * @param value The literal value
	 * @return A Literal instance for <code>value</code> from <code>kb</code>.
	 */
	public static KbLiteral create(KbInstance kb, String value)
	{
		long index = kb.uriToRsrcId(value, true, true);
		return new KbLiteral(kb, index);
	}

	/**
	 * Creates and returns an instance of Literal for the specified value if and
	 * only if it already exists in the Parliament.
	 *
	 * @param kb The Parliament instance to query
	 * @param value The literal value
	 * @return A Literal instance for <code>value</code> from <code>kb</code>,
	 *         or <code>null</code> if no such literal exists.
	 */
	public static KbLiteral get(KbInstance kb, String value)
	{
		long index = kb.uriToRsrcId(value, true, false);
		return (index == KbInstance.NULL_RSRC_ID) ? null : new KbLiteral(kb, index);
	}

	@Override
	public URI getDatatype()
	{
		return null;
	}

	@Override
	public String getLabel()
	{
		return getKb().rsrcIdToUri(getIndex(getKb()));
	}

	@Override
	public String getLanguage()
	{
		return null;
	}

	@Override
	public String toString()
	{
		return getLabel();
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
		{
			return true;
		}
		else if (o instanceof Literal)
		{
			Literal other = (Literal) o;

			// Compare labels
			if (!getLabel().equals(other.getLabel()))
			{
				return false;
			}

			// Compare datatypes
			if (getDatatype() == null)
			{
				if (other.getDatatype() != null)
				{
					return false;
				}
			}
			else
			{
				if (!getDatatype().equals(other.getDatatype()))
				{
					return false;
				}
			}

			// Compare language tags
			if (getLanguage() == null)
			{
				if (other.getLanguage() != null)
				{
					return false;
				}
			}
			else
			{
				if (!getLanguage().equals(other.getLanguage()))
				{
					return false;
				}
			}

			return true;
		}

		return false;
	}

	@Override
	public int hashCode()
	{
		return getLabel().hashCode();
	}

	@Override
	public String getKbStringRepresentation()
	{
		// TODO Auto-generated method stub
		return null;
	}
}
