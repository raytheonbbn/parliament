// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
// $Id: StringConcat.java 747 2009-06-08 19:35:56Z tself $

package com.bbn.sesame.rules;

import java.util.List;
import java.util.Map;

import org.openrdf.model.Value;
import org.openrdf.model.impl.LiteralImpl;
import org.w3._2003._11.ruleml.Var;

public class StringConcat extends SWRLBuiltin
{
	StringConcat(List<Object> args)
	{
		super(args);
	}

	@Override
	boolean run(SWRLRule rule, Map<String, Value> boundVars)
	{
		StringBuffer result = new StringBuffer();
		for (int i = 1; i < _args.size(); ++i)
		{
			result.append(rule.getValue(_args.get(i), boundVars).toString());
		}
		boundVars.put(((Var) _args.get(0)).getValue(),
			new LiteralImpl(result.toString()));

		return true;
	}
}
