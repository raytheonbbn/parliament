// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.graph.index.temporal;

import java.util.Properties;

import com.bbn.parliament.jena.graph.index.temporal.memory.MemoryTemporalIndex;
import com.hp.hpl.jena.graph.Graph;

/** @author rbattle */
public class MockTemporalIndex extends MemoryTemporalIndex {
	public MockTemporalIndex(Graph graph, Properties configuration) {
		super(graph, configuration);
	}
}
