// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
// $Id: SameAsRdfSource.java 747 2009-06-08 19:35:56Z tself $

package com.bbn.sesame.sail.sameas;

import java.util.ArrayList;
import java.util.List;
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
import org.openrdf.sesame.sail.query.Query;

public class SameAsRdfSource implements RdfSource, StackedSail
{
	private static final String SAME_AS = "http://www.w3.org/2002/07/owl#sameAs";
	private static final String RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
	private static final String RDFS_LABEL = "http://www.w3.org/2000/01/rdf-schema#label";

	private RdfSource	_baseSail;
	private URI			_sameAs;
	private URI			_rdfType;
	private URI			_rdfsLabel;

	public SameAsRdfSource()
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
			_rdfType = f.createURI(RDF_TYPE);
			_rdfsLabel = f.createURI(RDFS_LABEL);
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
					getEquivalenceClassInternal(set,
						(Resource) iterator.next().getObject());
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
		List<StatementIterator> statementIterators = new ArrayList<>();
		Resource substitutionSubj = subj;
		Value substitutionObj = obj;

		if (pred != null && pred.equals(_sameAs))
		{
			return _baseSail.getStatements(subj, pred, obj);
		}

		List<Resource> subjects;
		if (subj == null || (pred != null && pred.equals(_rdfsLabel))
			|| (pred != null && pred.equals(_rdfType)))
		{
			subjects = new ArrayList<>();
			subjects.add(subj);
			substitutionSubj = null;
		}
		else
		{
			subjects = new ArrayList<>(getEquivalenceClass(subj));
		}

		List<Value> objects;
		if (obj == null || (!(obj instanceof Resource))
			|| (pred != null && pred.equals(_rdfType)))
		{
			objects = new ArrayList<>();
			objects.add(obj);
			substitutionObj = null;
		}
		else
		{
			objects = new ArrayList<>(getEquivalenceClass((Resource) obj));
		}

		if (subjects.size() == 1 && objects.size() == 1)
		{
			return _baseSail.getStatements(subj, pred, obj);
		}

		for (Resource subject : subjects)
		{
			for (Value object : objects)
			{
				StatementIterator iterator = _baseSail.getStatements(
					subject, pred, object);
				statementIterators.add(new SubstitutingStatementIterator(iterator,
					substitutionSubj, substitutionObj));
			}
		}

		return new StatementIteratorIterator(statementIterators);
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

	@Override
	public Query optimizeQuery(Query query)
	{
		return _baseSail.optimizeQuery(query);
	}

	@Override
	public ValueFactory getValueFactory()
	{
		return _baseSail.getValueFactory();
	}
}
