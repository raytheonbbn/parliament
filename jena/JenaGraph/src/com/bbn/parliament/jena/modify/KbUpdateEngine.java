// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.modify;

import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.modify.UpdateEngine;
import org.apache.jena.sparql.modify.UpdateEngineBase;
import org.apache.jena.sparql.modify.UpdateEngineFactory;
import org.apache.jena.sparql.modify.UpdateEngineRegistry;
import org.apache.jena.sparql.modify.UpdateSink;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.update.Update;

import com.bbn.parliament.jena.graph.KbGraphStore;

/** @author sallen */
public class KbUpdateEngine extends UpdateEngineBase {
	public KbUpdateEngine(KbGraphStore graphStore, Binding inputBinding, Context context) {
		super(graphStore, inputBinding, context);
	}

	@Override
	public void startRequest() {
		graphStore.startRequest(request);
		KbUpdateEngineWorker worker = new KbUpdateEngineWorker((KbGraphStore) graphStore, startBinding, context);
		for (Update up : request.getOperations())
			up.visit(worker);
		graphStore.finishRequest(request);
	}

	private static UpdateEngineFactory factory = new UpdateEngineFactory() {
		@Override
		public boolean accept(DatasetGraph graphStore, Context context) {
			return (graphStore instanceof KbGraphStore);
		}

		@Override
		public UpdateEngine create(DatasetGraph graphStore, Binding inputBinding, Context context) {
			return new KbUpdateEngine((KbGraphStore) graphStore, inputBinding, context);
		}
	};

	public static UpdateEngineFactory getFactory() {
		return factory;
	}

	public static void register() {
		UpdateEngineRegistry.get().add(getFactory());
	}

	@Override
	public void finishRequest() {
		// TODO Auto-generated method stub

	}

	@Override
	public UpdateSink getUpdateSink() {
		// TODO Auto-generated method stub
		return null;
	}
}
