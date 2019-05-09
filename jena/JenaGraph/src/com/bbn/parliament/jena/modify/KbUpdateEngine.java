// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.modify;

import com.bbn.parliament.jena.graph.KbGraphStore;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.modify.UpdateEngine;
import com.hp.hpl.jena.sparql.modify.UpdateEngineBase;
import com.hp.hpl.jena.sparql.modify.UpdateEngineFactory;
import com.hp.hpl.jena.sparql.modify.UpdateEngineRegistry;
import com.hp.hpl.jena.sparql.util.Context;
import com.hp.hpl.jena.update.GraphStore;
import com.hp.hpl.jena.update.Update;
import com.hp.hpl.jena.update.UpdateRequest;

/** @author sallen */
public class KbUpdateEngine extends UpdateEngineBase {
	public KbUpdateEngine(KbGraphStore graphStore, UpdateRequest request, Binding inputBinding, Context context) {
		super(graphStore, request, inputBinding, context);
	}

	@Override
	public void execute() {
		graphStore.startRequest(request);
		KbUpdateEngineWorker worker = new KbUpdateEngineWorker((KbGraphStore) graphStore, startBinding, context);
		for (Update up : request.getOperations())
			up.visit(worker);
		graphStore.finishRequest(request);
	}

	private static UpdateEngineFactory factory = new UpdateEngineFactory() {
		@Override
		public boolean accept(UpdateRequest request, GraphStore graphStore, Context context) {
			return (graphStore instanceof KbGraphStore);
		}

		@Override
		public UpdateEngine create(UpdateRequest request, GraphStore graphStore, Binding inputBinding, Context context) {
			return new KbUpdateEngine((KbGraphStore) graphStore, request, inputBinding, context);
		}
	};

	public static UpdateEngineFactory getFactory() {
		return factory;
	}

	public static void register() {
		UpdateEngineRegistry.get().add(getFactory());
	}
}
