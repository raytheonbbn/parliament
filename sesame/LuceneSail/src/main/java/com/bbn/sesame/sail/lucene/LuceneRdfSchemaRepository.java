// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
// $Id: LuceneRdfSchemaRepository.java 747 2009-06-08 19:35:56Z tself $

package com.bbn.sesame.sail.lucene;

import java.util.Map;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.sesame.sail.LiteralIterator;
import org.openrdf.sesame.sail.RdfSchemaRepository;
import org.openrdf.sesame.sail.Sail;
import org.openrdf.sesame.sail.SailInitializationException;
import org.openrdf.sesame.sail.SailInternalException;
import org.openrdf.sesame.sail.StatementIterator;

public class LuceneRdfSchemaRepository extends LuceneRdfRepository implements RdfSchemaRepository
{
	private RdfSchemaRepository _baseSail;

	public LuceneRdfSchemaRepository()
	{
		super();
	}

	@Override
	public void setBaseSail(Sail baseSail)
	{
		super.setBaseSail(baseSail);
		if (baseSail instanceof RdfSchemaRepository)
		{
			_baseSail = (RdfSchemaRepository) baseSail;
		}
		else
		{
			throw new SailInternalException(
				"RdfSchameRepository:  base Sail should be an RdfSchemaRepository");
		}
	}

	@Override
	public void initialize(Map configParams) throws SailInitializationException
	{
		super.initialize(configParams);
	}

	@Override
	public StatementIterator getClasses()
	{
		return _baseSail.getClasses();
	}

	@Override
	public StatementIterator getDirectSubClassOf(Resource subClass,
		Resource superClass)
	{
		return _baseSail.getDirectSubClassOf(subClass, superClass);
	}

	@Override
	public StatementIterator getDirectSubPropertyOf(Resource subProperty,
		Resource superProperty)
	{
		return _baseSail.getDirectSubPropertyOf(subProperty, superProperty);
	}

	@Override
	public StatementIterator getDirectType(Resource anInstance, Resource aClass)
	{
		return _baseSail.getDirectType(anInstance, aClass);
	}

	@Override
	public StatementIterator getDomain(Resource prop, Resource domain)
	{
		return _baseSail.getDomain(prop, domain);
	}

	@Override
	public StatementIterator getExplicitStatements(Resource subj, URI pred, Value obj)
	{
		return _baseSail.getExplicitStatements(subj, pred, obj);
	}

	@Override
	public LiteralIterator getLiterals(String label, String language, URI datatype)
	{
		return _baseSail.getLiterals(label, language, datatype);
	}

	@Override
	public StatementIterator getProperties()
	{
		return _baseSail.getProperties();
	}

	@Override
	public StatementIterator getRange(Resource prop, Resource range)
	{
		return _baseSail.getRange(prop, range);
	}

	@Override
	public StatementIterator getSubClassOf(Resource subClass, Resource superClass)
	{
		return _baseSail.getSubClassOf(subClass, superClass);
	}

	@Override
	public StatementIterator getSubPropertyOf(Resource subProperty,
		Resource superProperty)
	{
		return _baseSail.getSubPropertyOf(subProperty, superProperty);
	}

	@Override
	public StatementIterator getType(Resource anInstance, Resource aClass)
	{
		return _baseSail.getType(anInstance, aClass);
	}

	@Override
	public boolean hasExplicitStatement(Resource subj, URI pred, Value obj)
	{
		return _baseSail.hasExplicitStatement(subj, pred, obj);
	}

	@Override
	public boolean isClass(Resource resource)
	{
		return _baseSail.isClass(resource);
	}

	@Override
	public boolean isDirectSubClassOf(Resource subClass, Resource superClass)
	{
		return _baseSail.isDirectSubClassOf(subClass, superClass);
	}

	@Override
	public boolean isDirectSubPropertyOf(Resource subProperty, Resource superProperty)
	{
		return _baseSail.isDirectSubPropertyOf(subProperty, superProperty);
	}

	@Override
	public boolean isDirectType(Resource anInstance, Resource aClass)
	{
		return _baseSail.isDirectType(anInstance, aClass);
	}

	@Override
	public boolean isProperty(Resource resource)
	{
		return _baseSail.isProperty(resource);
	}

	@Override
	public boolean isSubClassOf(Resource subClass, Resource superClass)
	{
		return _baseSail.isSubClassOf(subClass, superClass);
	}

	@Override
	public boolean isSubPropertyOf(Resource subProperty, Resource superProperty)
	{
		return _baseSail.isSubPropertyOf(subProperty, superProperty);
	}

	@Override
	public boolean isType(Resource anInstance, Resource aClass)
	{
		return _baseSail.isType(anInstance, aClass);
	}
}
