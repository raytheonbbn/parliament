// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
package com.bbn.parliament.kb_graph.index.temporal.extent;

/** @author dkolas */
public abstract class TemporalExtent {
	public abstract TemporalInstant getStart();
	public abstract TemporalInstant getEnd();
}
