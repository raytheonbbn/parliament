// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
package com.bbn.parliament.jena.graph.index.temporal.extent;

/**
 * Common ancestor of all temporal entities used in the temporal
 * index.
 *
 *  @author dkolas */
public interface TemporalExtent {
	TemporalInstant getStart();
	TemporalInstant getEnd();

	boolean sameAs(TemporalExtent other);
}
