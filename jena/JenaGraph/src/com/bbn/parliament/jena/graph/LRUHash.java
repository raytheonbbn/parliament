// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
package com.bbn.parliament.jena.graph;

import java.util.LinkedHashMap;
import java.util.Map;

import com.hp.hpl.jena.graph.Node;

public class LRUHash extends LinkedHashMap<Node, Long>
{
   private static final long serialVersionUID = -7259280495201459276L;
   private int maxEntries;

   public LRUHash(int maxEntries)
   {
      super(maxEntries, (float) .75, true);
      this.maxEntries = maxEntries;
   }

   @Override
	protected boolean removeEldestEntry(Map.Entry<Node, Long> arg0)
   {
      return size() > maxEntries;
   }
}
