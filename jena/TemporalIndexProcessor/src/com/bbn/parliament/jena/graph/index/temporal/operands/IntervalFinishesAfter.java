// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
package com.bbn.parliament.jena.graph.index.temporal.operands;

import java.util.Iterator;

import com.bbn.parliament.jena.graph.index.Record;
import com.bbn.parliament.jena.graph.index.temporal.ExtentTester;
import com.bbn.parliament.jena.graph.index.temporal.TemporalExtentIterator;
import com.bbn.parliament.jena.graph.index.temporal.extent.TemporalExtent;

/**
 * @author dkolas
 */
public class IntervalFinishesAfter extends MemoryOperatorImplementation {
	public IntervalFinishesAfter(ExtentTester extentTester) {
		super(extentTester);
	}

	/**
    * @see com.bbn.parliament.jena.graph.index.temporal.TemporalPropertyFunction#bindFirstVar(
    *      com.bbn.parliament.jena.graph.index.temporal.extent.TemporalExtent)
    */
   @Override
   public Iterator<Record<TemporalExtent>> bindFirstVar(
      TemporalExtent boundExtent) {
      return new TemporalExtentIterator(wrapIterator( getIndex()
         .afterFinish(boundExtent)), new AlwaysIncludeEndsInclusionDecider(boundExtent));
   }

   /**
    * @see com.bbn.parliament.jena.graph.index.temporal.TemporalPropertyFunction#bindSecondVar(
    *      com.bbn.parliament.jena.graph.index.temporal.extent.TemporalExtent)
    */
   @Override
   public Iterator<Record<TemporalExtent>> bindSecondVar(
      TemporalExtent boundExtent) {
	   return new TemporalExtentIterator(wrapIterator( getIndex()
		         .beforeFinish(boundExtent)), new AlwaysIncludeEndsInclusionDecider(boundExtent));
   }
}
