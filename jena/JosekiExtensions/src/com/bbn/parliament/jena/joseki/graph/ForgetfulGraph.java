// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
/**
 *
 */
package com.bbn.parliament.jena.joseki.graph;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.TripleMatch;
import com.hp.hpl.jena.graph.impl.GraphBase;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

/**
 * This is a graph that keeps track of the number of statements added,
 * but throws them away and doesn't actually store them.  It is useful
 * for syntax verification / count applications.
 *
 * @author sallen
 */
public class ForgetfulGraph extends GraphBase {
   protected int _numStatements = 0;

   /* (non-Javadoc)
    * @see com.hp.hpl.jena.graph.impl.GraphBase#graphBaseFind(com.hp.hpl.jena.graph.TripleMatch)
    */
   @Override
   protected ExtendedIterator<Triple> graphBaseFind(TripleMatch m) {
      throw new com.hp.hpl.jena.shared.NotFoundException("ForgetfulGraph::graphBaseFind");
   }

   /* (non-Javadoc)
    * @see com.hp.hpl.jena.graph.impl.GraphBase#performAdd(com.hp.hpl.jena.graph.Triple)
    */
   @Override
   public void performAdd(Triple t) {
      _numStatements++;
   }

   /* (non-Javadoc)
    * @see com.hp.hpl.jena.graph.impl.GraphBase#graphBaseSize()
    */
   @Override
   protected int graphBaseSize() {
      return _numStatements;
   }
}
