// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
package com.bbn.parliament.jena.graph.union;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;

/**
 * @author dkolas
 */
public interface KbUnionableGraph extends Graph
{
   long getNodeCountInPosition(Node node, int position);
}
