// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
// $Id: KbUri.java 747 2009-06-08 19:35:56Z tself $

package com.bbn.parliament.sesame.sail;

import org.openrdf.model.URI;
import org.openrdf.sesame.sail.StatementIterator;

import com.bbn.parliament.jni.KbInstance;

public class KbUri extends KbResource implements URI
{
	private static final long serialVersionUID = 1L;

	private String            _uri             = null;
	private String            _localName       = null;
	private String            _namespace       = null;

	public KbUri(KbInstance kb, long index)
	{
		super(kb, index);
		_uri = kb.rsrcIdToUri(getIndex(kb));
	}

	/** Assumes that the caller has guaranteed that the index and uri arguments correspond. */
	private KbUri(KbInstance kb, long index, String uri)
	{
		super(kb, index);
		_uri = uri;
	}

	/**
	 * Creates and returns an instance of URI for the specified uri. The URI will
	 * be added to the supplied Parliament if it does not already exist.
	 *
	 * @param kb The Parliament instance to query
	 * @param uri The uri value
	 * @return A URI instance for <code>uri</code> from <code>kb</code>.
	 */
	public static KbUri create(KbInstance kb, String uri)
	{
		long index = kb.uriToRsrcId(uri, false, true);
		return new KbUri(kb, index, uri);
	}

	/**
	 * Creates and returns an instance of URI for the specified uri. The URI will
	 * be added to the supplied Parliament if it does not already exist.
	 *
	 * @param kb The Parliament instance to query
	 * @param uri The uri value
	 * @return A URI instance for <code>uri</code> from <code>kb</code>, or
	 *         <code>null</code> if no such URI exists.
	 */
	public static KbUri get(KbInstance kb, String uri)
	{
		long index = kb.uriToRsrcId(uri, false, false);
		return (index == KbInstance.NULL_RSRC_ID)
			? null
				: new KbUri(kb, index, uri);
	}

	private void split()
	{
		String uri = getURI();
		int sep = uri.lastIndexOf('#');
		if (sep == -1)
		{
			sep = uri.lastIndexOf('/');
		}
		if (sep == -1)
		{
			sep = uri.lastIndexOf(':');
		}
		if (sep == -1)
		{
			_namespace = null;
			_localName = uri;
		}
		else
		{
			_namespace = uri.substring(0, sep + 1);
			_localName = uri.substring(sep + 1);
		}
	}

	@Override
	public String getLocalName()
	{
		if (_localName == null)
		{
			split();
		}
		return _localName;
	}

	@Override
	public String getNamespace()
	{
		if (_localName == null)
		{
			split();
		}
		return _namespace;
	}

	@Override
	public String getURI()
	{
		if (_uri == null)
		{
			@SuppressWarnings("resource")
			KbInstance kb = getKb();
			_uri = kb.rsrcIdToUri(getIndex(kb));
		}
		return _uri;
	}

	@Override
	public String toString()
	{
		return getURI();
	}

	/** allow for URIImpl's parsed from queries */
	@Override
	public int compareTo(Object object)
	{
		return (object instanceof KbValue)
			? super.compareTo(object)
				: getURI().compareTo(((URI) object).getURI());
	}

	@Override
	public StatementIterator getPredicateStatements()
	{
		return new KbRdfSource().getStatements(null, this, null);
	}

	@Override
	public boolean equals(Object rhs)
	{
		return (this == rhs)
			|| ((rhs instanceof URI) && getURI().equals(((URI) rhs).getURI()));
	}

	@Override
	public int hashCode()
	{
		// Note that the best, most natural, and most performant implementation
		// of this method would simply return getURI().hashCode(), but the Sesame
		// documentation requires the following.
		String ns = getNamespace();
		String ln = getLocalName();
		return (ns == null) ? ln.hashCode() : ns.hashCode() ^ ln.hashCode();
	}

	@Override
	public String getKbStringRepresentation()
	{
		return _uri;
	}
}
