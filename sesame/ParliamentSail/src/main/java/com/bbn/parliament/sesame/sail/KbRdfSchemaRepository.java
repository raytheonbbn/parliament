// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
// $Id: KbRdfSchemaRepository.java 747 2009-06-08 19:35:56Z tself $

package com.bbn.parliament.sesame.sail;

import java.util.Map;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.sesame.sail.LiteralIterator;
import org.openrdf.sesame.sail.RdfSchemaRepository;
import org.openrdf.sesame.sail.SailInitializationException;
import org.openrdf.sesame.sail.StatementIterator;

public class KbRdfSchemaRepository extends KbRdfRepository implements RdfSchemaRepository
{
	private static final String RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	private static final String RDFS = "http://www.w3.org/2000/01/rdf-schema#";

	public KbRdfSchemaRepository()
	{
		super();
	}

	@Override
	public void initialize(Map configParams) throws SailInitializationException
	{
		super.initialize(configParams);
	}

	@Override
	public StatementIterator getClasses()
	{
		throw new UnimplementedException();
	}

	@Override
	public StatementIterator getDirectSubClassOf(Resource subClass, Resource superClass)
	{
		URI rdfsSubClassOf = createURI(RDFS + "subClassOf");
		return getStatements(subClass, rdfsSubClassOf, superClass, true);
	}

	@Override
	public StatementIterator getDirectSubPropertyOf(Resource subProp, Resource superProp)
	{
		URI rdfsSubPropOf = createURI(RDFS + "subPropertyOf");
		return getStatements(subProp, rdfsSubPropOf, superProp, true);
	}

	@Override
	public StatementIterator getDirectType(Resource anInstance, Resource aClass)
	{
		URI rdfType = createURI(RDF + "type");
		return getStatements(anInstance, rdfType, aClass, true);
	}

	@Override
	public StatementIterator getDomain(Resource prop, Resource domain)
	{
		return getStatements(prop, createURI(RDFS + "domain"), domain);
	}

	@Override
	public StatementIterator getExplicitStatements(Resource subj, URI pred, Value obj)
	{
		return getStatements(subj, pred, obj, true);
	}

	@Override
	public LiteralIterator getLiterals(String label, String language, URI datatype)
	{
		throw new UnimplementedException();
	}

	@Override
	public StatementIterator getProperties()
	{
		throw new UnimplementedException();
	}

	@Override
	public StatementIterator getRange(Resource prop, Resource range)
	{
		return getStatements(prop, createURI(RDFS + "range"), range);
	}

	@Override
	public StatementIterator getSubClassOf(Resource subClass, Resource superClass)
	{
		return getStatements(subClass, createURI(RDFS + "subClassOf"), superClass);
	}

	@Override
	public StatementIterator getSubPropertyOf(Resource subProperty,
		Resource superProperty)
	{
		return getStatements(subProperty,
			createURI(RDFS + "subPropertyOf"), superProperty);
	}

	@Override
	public StatementIterator getType(Resource anInstance, Resource aClass)
	{
		return getStatements(anInstance, createURI(RDF + "type"), aClass);
	}

	/** common code to ensure iterator is closed */
	private static boolean hasNext(StatementIterator iterator)
	{
		boolean retval;
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
	public boolean hasExplicitStatement(Resource subj, URI pred, Value obj)
	{
		return hasNext(getStatements(subj, pred, obj, true));
	}

	@Override
	public boolean isClass(Resource resource)
	{
		return hasNext(getStatements(resource, createURI(RDF + "type"),
			createURI("http://www.w3.org/2002/07/owl#Class"))); // XXX
	}

	@Override
	public boolean isDirectSubClassOf(Resource subClass, Resource superClass)
	{
		throw new UnimplementedException();
	}

	@Override
	public boolean isDirectSubPropertyOf(Resource subProperty, Resource superProperty)
	{
		throw new UnimplementedException();
	}

	@Override
	public boolean isDirectType(Resource anInstance, Resource aClass)
	{
		throw new UnimplementedException();
	}

	@Override
	public boolean isProperty(Resource resource)
	{
		throw new UnimplementedException();
	}

	@Override
	public boolean isSubClassOf(Resource subClass, Resource superClass)
	{
		throw new UnimplementedException();
	}

	@Override
	public boolean isSubPropertyOf(Resource subProperty, Resource superProperty)
	{
		throw new UnimplementedException();
	}

	@Override
	public boolean isType(Resource anInstance, Resource aClass)
	{
		return hasNext(getStatements(anInstance, createURI(RDF + "type"), aClass));
	}
}
