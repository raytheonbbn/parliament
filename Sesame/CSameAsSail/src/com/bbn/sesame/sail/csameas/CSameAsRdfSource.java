// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
// $Id: CSameAsRdfSource.java 747 2009-06-08 19:35:56Z tself $

package com.bbn.sesame.sail.csameas;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.sesame.sail.NamespaceIterator;
import org.openrdf.sesame.sail.RdfSource;
import org.openrdf.sesame.sail.Sail;
import org.openrdf.sesame.sail.SailInitializationException;
import org.openrdf.sesame.sail.SailInternalException;
import org.openrdf.sesame.sail.StackedSail;
import org.openrdf.sesame.sail.StatementIterator;
import org.openrdf.sesame.sail.query.GraphPattern;
import org.openrdf.sesame.sail.query.GraphPatternQuery;
import org.openrdf.sesame.sail.query.Query;
import org.openrdf.sesame.sail.query.SetOperator;
import org.openrdf.sesame.sail.query.TriplePattern;
import org.openrdf.sesame.sail.query.Var;

public class CSameAsRdfSource implements RdfSource, StackedSail
{
	private static final boolean ENABLE_QUERY_OPTIMIZATION = false;
	private static final String SAME_AS    = "http://www.w3.org/2002/07/owl#sameAs";
	private static final String DIFF_FROM  = "http://www.w3.org/2002/07/owl#differentFrom";
	private static final String CANON_INST = "http://parliament.semwebcentral.org/2006/02/sameas-ont#canonicalInstance";

	private RdfSource           _baseSail;
	protected URI               _sameAs;
	protected URI               _differentFrom;
	protected URI               _canonicalInstance;

	public CSameAsRdfSource()
	{
	}

	@Override
	public Sail getBaseSail()
	{
		return _baseSail;
	}

	@Override
	public void setBaseSail(Sail baseSail)
	{
		if (baseSail instanceof RdfSource)
		{
			_baseSail = (RdfSource) baseSail;

			ValueFactory f = getValueFactory();
			_sameAs = f.createURI(SAME_AS);
			_differentFrom = f.createURI(DIFF_FROM);
			_canonicalInstance = f.createURI(CANON_INST);
		}
		else
		{
			throw new SailInternalException(
				"SameAsRdfSource:  base Sail should be an RdfSource");
		}
	}

	@Override
	public void initialize(Map configParams) throws SailInitializationException
	{
		// base sail will be initialized separately
	}

	@Override
	public void shutDown()
	{
		if (_baseSail != null)
		{
			_baseSail.shutDown();
		}
	}

	@Override
	public NamespaceIterator getNamespaces()
	{
		return _baseSail.getNamespaces();
	}

	Resource getCanonicalInstance(Resource resource)
	{
		// XXX - check flags?

		// XXX - optimize by accessing KB directly?
		StatementIterator iterator = _baseSail.getStatements(resource,
			_canonicalInstance, null);
		try
		{
			while (iterator.hasNext())
			{
				return (Resource) iterator.next().getObject();
			}
		}
		finally
		{
			iterator.close();
		}

		return null;
	}

	void getEquivalenceClassInternal(Set<Resource> set, Resource resource)
	{
		if (!set.contains(resource))
		{
			set.add(resource);

			StatementIterator iterator = _baseSail.getStatements(resource,
				_sameAs, null);
			try
			{
				while (iterator.hasNext())
				{
					getEquivalenceClassInternal(set, (Resource) iterator.next()
						.getObject());
				}
			}
			finally
			{
				iterator.close();
			}

			iterator = _baseSail.getStatements(null, _sameAs, resource);
			try
			{
				while (iterator.hasNext())
				{
					getEquivalenceClassInternal(set, iterator.next().getSubject());
				}
			}
			finally
			{
				iterator.close();
			}
		}
	}

	Set<Resource> getEquivalenceClass(Resource resource)
	{
		Set<Resource> retval = new TreeSet<>();
		try
		{
			getEquivalenceClassInternal(retval, resource);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return retval;
	}

	@Override
	public StatementIterator getStatements(Resource subj, URI pred, Value obj)
	{
		// replace with canonical instances
		if (subj != null)
		{
			Resource canonical = getCanonicalInstance(subj);
			if (canonical != null)
			{
				subj = canonical;
			}
		}
		if (obj != null && obj instanceof Resource)
		{
			Resource canonical = getCanonicalInstance((Resource) obj);
			if (canonical != null)
			{
				obj = canonical;
			}
		}

		StatementIterator iterator = _baseSail.getStatements(subj, pred, obj);
		boolean canonicalizeSubject = (subj == null);
		boolean canonicalizeObject = (obj == null);
		return (canonicalizeSubject || canonicalizeObject)
			? new DistinctStatementIterator(
				new CanonicalStatementIterator(
					iterator, this, canonicalizeSubject, canonicalizeObject))
				: iterator;
	}

	@Override
	public boolean hasStatement(Resource subj, URI pred, Value obj)
	{
		boolean retval;
		StatementIterator iterator = getStatements(subj, pred, obj);
		try
		{
			retval = iterator.hasNext();
		}
		finally
		{
			iterator.close();
		}
		return retval;
	}

	/** inner loop processing of Vars */
	void optimizeQuery(Var var)
	{
		if (var.hasValue())
		{
			Value value = var.getValue();
			if (value instanceof Resource)
			{
				Resource canonical = getCanonicalInstance((Resource) value);
				if (canonical != null)
				{
					var.setValue(canonical);
				}
			}
		}
	}

	/** common processing of GraphPatterns */
	void optimizeQuery(GraphPattern graphPattern)
	{
		// replace any URIs with canonical instances
		for (Object pe : graphPattern.getPathExpressions())
		{
			if (pe instanceof TriplePattern)
			{
				TriplePattern tp = (TriplePattern) pe;
				optimizeQuery(tp.getSubjectVar());
				// predicate not used
				optimizeQuery(tp.getObjectVar());
			}
		}

		// recurse for sublists
		for (Object o : graphPattern.getOptionals())
		{
			optimizeQuery((GraphPattern) o);
		}
	}

	/** replace any URIs with canonical instances. */
	@Override
	public Query optimizeQuery(Query query)
	{
		// disabled because it changes the query constants returned
		if (ENABLE_QUERY_OPTIMIZATION)
		{
			if (query instanceof GraphPatternQuery)
			{
				GraphPatternQuery gpquery = (GraphPatternQuery) query;
				GraphPattern graphPattern = gpquery.getGraphPattern();
				optimizeQuery(graphPattern);
			}
			else if (query instanceof SetOperator)
			{
				SetOperator operator = (SetOperator) query;
				optimizeQuery(operator.getLeftArg());
				optimizeQuery(operator.getRightArg());
			}
			else
			{
				System.err.println("unexpected query type " + query.getClass()
				+ " in optimizeQuery");
			}
		}

		return _baseSail.optimizeQuery(query);
	}

	@Override
	public ValueFactory getValueFactory()
	{
		return _baseSail.getValueFactory();
	}
}
