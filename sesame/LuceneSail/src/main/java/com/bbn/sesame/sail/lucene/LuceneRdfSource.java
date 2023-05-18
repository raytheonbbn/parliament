// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
// $Id: LuceneRdfSource.java 747 2009-06-08 19:35:56Z tself $

package com.bbn.sesame.sail.lucene;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
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
import org.openrdf.sesame.sail.query.BooleanExpr;
import org.openrdf.sesame.sail.query.GraphPattern;
import org.openrdf.sesame.sail.query.GraphPatternQuery;
import org.openrdf.sesame.sail.query.Like;
import org.openrdf.sesame.sail.query.Query;

import com.bbn.parliament.core.jni.KbInstance;
import com.bbn.parliament.sesame.sail.KbLiteral;
import com.bbn.parliament.sesame.sail.KbRdfSource;
import com.bbn.sesame.sail.sameas.StatementIteratorIterator;

public class LuceneRdfSource implements RdfSource, StackedSail
{
	private static final String LUCENE_QUERY = "http://parliament.semwebcentral.org/2005/11/lucene-ont#query";

	private RdfSource          _baseSail;
	protected URI              _luceneQuery;
	private QueryParser        _queryParser   = new QueryParser("literal", new StandardAnalyzer());
	private KbRdfSource        _underlyingKbSource;
	protected String           _indexPath     = "lucenesail.idx";
	protected StandardAnalyzer _analyzer      = new StandardAnalyzer();

	/** Thread => luceneQuery String */
	private Map<Thread, String> _luceneQueries = new HashMap<>();

	public KbInstance getKb()
	{
		return (_underlyingKbSource == null)
			? null
			: _underlyingKbSource.getKb();
	}

	public LuceneRdfSource()
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
			_luceneQuery = getValueFactory().createURI(LUCENE_QUERY);
			Sail base = baseSail;
			while (base != null && !(base instanceof KbRdfSource))
			{
				if (!(base instanceof StackedSail))
				{
					throw new SailInternalException(
						"Unable to find a Parliament SAIL in the SAIL stack.");
				}
				base = ((StackedSail) base).getBaseSail();
			}
			if (base == null)
			{
				throw new SailInternalException(
					"SAIL stack must include a Parliament SAIL");
			}
			_underlyingKbSource = (KbRdfSource) base;
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
		try
		{
			// create index if necessary
			try
			{
				(new IndexWriter(_indexPath, _analyzer, false)).close();
			}
			catch (IOException ex)
			{
				(new IndexWriter(_indexPath, _analyzer, true)).close();
			}
		}
		catch (Exception ex)
		{
			throw new SailInitializationException(ex);
		}
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

	@Override
	public StatementIterator getStatements(Resource subj, URI pred, Value obj)
	{
		String luceneQuery = _luceneQueries.get(Thread.currentThread());
		if (luceneQuery != null)
		{
			try
			{
				IndexSearcher indexSearcher = new IndexSearcher(_indexPath);
				org.apache.lucene.search.Query query = _queryParser.parse(luceneQuery);
				Hits hits = indexSearcher.search(query);
				List<StatementIterator> iterators = new ArrayList<>();
				for (int i = 0; i < hits.length(); ++i)
				{
					Document document = hits.doc(i);
					long index = Long.parseLong(document.getField("index").stringValue());
					@SuppressWarnings("resource")
					KbInstance kb = getKb();
					iterators.add(_baseSail.getStatements(null, null,
						new KbLiteral(kb, index)));
				}
				indexSearcher.close();
				return new StatementIteratorIterator(iterators);
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
				return null; // XXX
			}
		}
		else
		{
			return _baseSail.getStatements(subj, pred, obj);
		}
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
	@SuppressWarnings("rawtypes")
	public Query optimizeQuery(Query query)
	{
		_luceneQueries.remove(Thread.currentThread());

		String luceneQuery = null;
		if (query instanceof GraphPatternQuery)
		{
			GraphPatternQuery gpq = (GraphPatternQuery) query;
			GraphPattern gp = gpq.getGraphPattern();
			Iterator constraints = gp.getConjunctiveConstraints().iterator();
			List<BooleanExpr> newConstraints = new ArrayList<>();
			while (constraints.hasNext())
			{
				BooleanExpr be = (BooleanExpr) constraints.next();
				if (be instanceof Like)
				{
					Like like = (Like) be;
					Collection variables = new ArrayList();
					like.getVariables(variables);
					String string = like.toString(); // only access to pattern
					int quote2 = string.lastIndexOf('"');
					int quote1 = string.lastIndexOf('"', quote2 - 1);
					luceneQuery = string.substring(quote1 + 1, quote2);
				}
				else
				{
					newConstraints.add(be);
				}
			}
			if (luceneQuery != null)
			{
				gp.setConstraints(newConstraints);
				_luceneQueries.put(Thread.currentThread(), luceneQuery);
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
