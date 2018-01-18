// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
// $Id: SWRLRule.java 747 2009-06-08 19:35:56Z tself $

package com.bbn.sesame.rules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.sesame.query.TableQuery;
import org.openrdf.sesame.sail.RdfRepository;
import org.openrdf.sesame.sail.query.GraphPattern;
import org.openrdf.sesame.sail.query.PathExpression;
import org.openrdf.sesame.sail.query.ProjectionElem;
import org.openrdf.sesame.sail.query.SelectQuery;
import org.openrdf.sesame.sail.query.TriplePattern;
import org.openrdf.sesame.sail.query.Var;
import org.openrdf.vocabulary.OWL;
import org.w3._2003._05.owl_xml.DataValue;
import org.w3._2003._05.owl_xml.DescriptionClazz;
import org.w3._2003._05.owl_xml.Individual;
import org.w3._2003._11.ruleml.Imp;
import org.w3._2003._11.swrlx.BuiltinAtom;
import org.w3._2003._11.swrlx.ClassAtom;
import org.w3._2003._11.swrlx.DatavaluedPropertyAtom;
import org.w3._2003._11.swrlx.DifferentIndividualsAtom;
import org.w3._2003._11.swrlx.IndividualPropertyAtom;
import org.w3._2003._11.swrlx.SameIndividualAtom;

public class SWRLRule
{
	Imp               _rule;
	TableQuery        _query;
	Map<String, Var>  _variables = new HashMap<>();
	List<SWRLBuiltin> _builtins  = new ArrayList<>();

	Var getVariable(String name)
	{
		Var retval = _variables.get(name);
		if (retval == null)
		{
			retval = new Var(name);
			_variables.put(name, retval);
		}
		return retval;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	SWRLRule(Imp rule)
	{
		_rule = rule;

		Iterator bodyAtoms = rule.getBody()
			.getClassAtomOrDatarangeAtomOrIndividualPropertyAtom().iterator();
		List<PathExpression> pathExpressionList = new ArrayList<>();
		while (bodyAtoms.hasNext())
		{
			Object atom = bodyAtoms.next();
			PathExpression pathExpression = null;
			if (atom instanceof ClassAtom)
			{
				ClassAtom ca = (ClassAtom) atom;

				pathExpression = new TriplePattern(getVariable(ca.getVar()),
					getVar(URIImpl.RDF_TYPE), getVar(ca.getClazz()));
			}
			else if (atom instanceof DatavaluedPropertyAtom)
			{
				DatavaluedPropertyAtom dpa = (DatavaluedPropertyAtom) atom;

				pathExpression = new TriplePattern(getVar(dpa.getContent().get(0)),
					getVar(dpa.getProperty()), getVar(dpa.getContent().get(1)));
			}
			else if (atom instanceof IndividualPropertyAtom)
			{
				IndividualPropertyAtom ipa = (IndividualPropertyAtom) atom;

				pathExpression = new TriplePattern(getVar(ipa.getContent().get(0)),
					getVar(ipa.getProperty()), getVar(ipa.getContent().get(1)));
			}
			else if (atom instanceof SameIndividualAtom)
			{
				System.err.println("body sameIndividualAtom not yet implemented");
			}
			else if (atom instanceof DifferentIndividualsAtom)
			{
				System.err.println("body differentIndividualsAtom not yet implemented");
			}
			else if (atom instanceof BuiltinAtom)
			{
				BuiltinAtom ba = (BuiltinAtom) atom;
				String builtin = ba.getBuiltin();
				List args = ba.getDObject();
				if (builtin.equals("http://www.w3.org/2003/11/swrlb#stringConcat"))
				{
					if (args.size() < 2)
					{
						System.err.println("too few arguments for stringConcat");
					}
					else
					{
						_builtins.add(new StringConcat(args));
					}
				}
				else
				{
					System.err.println(builtin + " not yet implemented");
				}
			}
			else
			{
				System.err.println("unexpected body atom class " + atom.getClass());
			}

			if (pathExpression != null)
			{
				pathExpressionList.add(pathExpression);
			}
		}

		List<ProjectionElem> projection = new ArrayList<>();
		for (Var var : _variables.values())
		{
			projection.add(new ProjectionElem(var));
		}

		GraphPattern graphPattern = new GraphPattern();
		graphPattern.addAll(pathExpressionList);
		_query = new TableQuery(new SelectQuery(true, projection, graphPattern));
	}

	public TableQuery getQuery()
	{
		return _query;
	}

	Var getVar(Object object)
	{
		if (object instanceof org.w3._2003._11.ruleml.Var)
		{
			return getVariable(((org.w3._2003._11.ruleml.Var) object).getValue());
		}
		else
		{
			return new Var(null, getValue(object));
		}
	}

	static Value getValue(String string)
	{
		return new URIImpl(string);
	}

	Value getValue(Object object)
	{
		if (object instanceof DescriptionClazz)
		{
			return getValue(((DescriptionClazz) object).getName());
		}
		else if (object instanceof Individual)
		{
			return getValue(((Individual) object).getName());
		}
		else if (object instanceof DataValue)
		{
			DataValue dv = (DataValue) object;
			return new LiteralImpl(dv.getContent().get(0).toString(), dv.getDatatype());
		}
		else if (object instanceof URI)
		{
			return getValue(((URI) object).getURI());
		}
		else if (object instanceof String)
		{
			return getValue((String) object);
		}
		else
		{
			System.err.println("unexpected object class in getValue:  "
				+ object.getClass());
			return null;
		}
	}

	Value getValue(Object object, Map<String, Value> boundVars)
	{
		if (object instanceof org.w3._2003._11.ruleml.Var)
		{
			String name = ((org.w3._2003._11.ruleml.Var) object).getValue();
			return boundVars.get(name);
		}
		else
		{
			return getValue(object);
		}
	}

	@SuppressWarnings("rawtypes")
	public void runHead(RdfRepository repository, String[] variables,
		List<Value> values) throws Exception
	{
		Map<String, Value> boundVars = new HashMap<>();

		// bind variables
		for (int i = 0; i < values.size(); ++i)
		{
			boundVars.put(variables[i], values.get(i));
		}

		// run body builtins
		for (SWRLBuiltin builtin : _builtins)
		{
			builtin.run(this, boundVars);
		}

		// process head atoms
		Iterator headAtoms = _rule.getHead()
			.getClassAtomOrDatarangeAtomOrIndividualPropertyAtom().iterator();
		while (headAtoms.hasNext())
		{
			Object atom = headAtoms.next();
			Resource subj = null;
			URI pred = null;
			Value obj = null;
			if (atom instanceof ClassAtom)
			{
				ClassAtom ca = (ClassAtom) atom;

				subj = (Resource) boundVars.get(ca.getVar());
				pred = (URI) getValue(URIImpl.RDF_TYPE);
				obj = getValue(ca.getClazz());
			}
			else if (atom instanceof DatavaluedPropertyAtom)
			{
				DatavaluedPropertyAtom dpa = (DatavaluedPropertyAtom) atom;

				subj = (Resource) getValue(dpa.getContent().get(0), boundVars);
				pred = (URI) getValue(dpa.getProperty());
				obj = getValue(dpa.getContent().get(1), boundVars);

				// convert value if necessary
				if (obj instanceof Resource)
				{
					obj = new LiteralImpl(obj.toString());
				}
			}
			else if (atom instanceof IndividualPropertyAtom)
			{
				IndividualPropertyAtom ipa = (IndividualPropertyAtom) atom;

				subj = (Resource) getValue(ipa.getContent().get(0), boundVars);
				pred = (URI) getValue(ipa.getProperty());
				obj = getValue(ipa.getContent().get(1), boundVars);

				// convert value if necessary
				if (obj instanceof Literal)
				{
					obj = getValue(obj.toString());
				}
			}
			else if (atom instanceof SameIndividualAtom)
			{
				SameIndividualAtom sia = (SameIndividualAtom) atom;
				subj = (Resource) getValue(sia.getIndividualOrVar().get(0), boundVars);
				pred = repository.getValueFactory().createURI(OWL.SAMEAS);
				obj = getValue(sia.getIndividualOrVar().get(1), boundVars);
			}
			else if (atom instanceof DifferentIndividualsAtom)
			{
				System.err.println("head differentIndividualsAtom not yet implemented");
			}
			else if (atom instanceof BuiltinAtom)
			{
				System.err.println("head builtinAtom not allowed");
			}
			else
			{
				System.err.println("unexpected head atom class " + atom.getClass());
			}

			repository.addStatement(subj, pred, obj);
			// XXX - mark statement as inferred
		}
	}
}
