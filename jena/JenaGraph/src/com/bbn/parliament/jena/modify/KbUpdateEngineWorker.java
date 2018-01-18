// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.modify;

import com.bbn.parliament.jena.graph.KbGraphFactory;
import com.bbn.parliament.jena.graph.KbGraphStore;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.sparql.AlreadyExists;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.modify.UpdateEngineWorker;
import com.hp.hpl.jena.sparql.modify.request.UpdateCreate;
import com.hp.hpl.jena.sparql.util.Context;

/** @author sallen */
public class KbUpdateEngineWorker extends UpdateEngineWorker {

	public KbUpdateEngineWorker(KbGraphStore graphStore, Binding initialBinding, Context context) {
		super(graphStore, initialBinding, context);
	}

	@Override
	public void visit(UpdateCreate create) {

		Node graphName = create.getGraph() ;

		if (graphStore.containsGraph(graphName))
		{
			if (create.isSilent())
			{
				return;
			}
			throw new AlreadyExists("Named graph: " + graphName);
		}

		graphStore.addGraph(graphName, KbGraphFactory.createNamedGraph());
	}
}
