// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
// $Id: SWRLBuiltin.java 747 2009-06-08 19:35:56Z tself $

package com.bbn.sesame.rules;

import java.util.List;
import java.util.Map;

import org.openrdf.model.Value;

abstract public class SWRLBuiltin
{
   protected List<Object> _args;

   SWRLBuiltin(List<Object> args)
   {
      _args = args;
   }

   abstract boolean run(SWRLRule rule, Map<String, Value> boundVars);
}
