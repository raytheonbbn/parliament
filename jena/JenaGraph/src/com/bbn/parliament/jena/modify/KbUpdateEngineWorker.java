// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.modify;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.AlreadyExists;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.modify.UpdateEngineWorker;
import org.apache.jena.sparql.modify.request.UpdateCreate;
import org.apache.jena.sparql.util.Context;

import com.bbn.parliament.jena.graph.KbGraph;
import com.bbn.parliament.jena.graph.KbGraphFactory;
import com.bbn.parliament.jena.graph.KbGraphStore;

/** @author sallen */
public class KbUpdateEngineWorker extends UpdateEngineWorker {
	public KbUpdateEngineWorker(KbGraphStore graphStore, Binding initialBinding, Context context) {
		super(graphStore, initialBinding, context);
	}

	@Override
	public void visit(UpdateCreate create) {
		Node graphName = create.getGraph();

		if (graphStore.containsGraph(graphName))
		{
			if (create.isSilent())
			{
				return;
			}
			throw new AlreadyExists("Named graph: " + graphName);
		}

		@SuppressWarnings("resource")
		KbGraph newGraph = KbGraphFactory.createNamedGraph();
		graphStore.addGraph(graphName, newGraph);
	}
}
