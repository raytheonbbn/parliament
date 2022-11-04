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
import org.apache.jena.sparql.modify.UpdateEngineWorker;
import org.apache.jena.sparql.modify.UpdateSink;
import org.apache.jena.sparql.modify.UpdateVisitorSink;
import org.apache.jena.sparql.modify.request.UpdateVisitor;
import org.apache.jena.sparql.util.Context;

import com.bbn.parliament.jena.graph.KbGraphStore;

/** @author sallen */
public class KbUpdateEngine extends UpdateEngineBase {
	private static UpdateEngineFactory factory = new UpdateEngineFactory() {
		@Override
		public boolean accept(DatasetGraph datasetGraph, Context context) {
			return (datasetGraph instanceof KbGraphStore);
		}

		@Override
		public UpdateEngine create(DatasetGraph datasetGraph, Binding inputBinding, Context context) {
			return new KbUpdateEngine((KbGraphStore) datasetGraph, inputBinding, context);
		}
	};

	private UpdateSink updateSink;

	public static UpdateEngineFactory getFactory() {
		return factory;
	}

	public static void register() {
		UpdateEngineRegistry.get().add(getFactory());
	}

	public KbUpdateEngine(KbGraphStore datasetGraph, Binding inputBinding, Context context) {
		super(datasetGraph, inputBinding, context);
		updateSink = null;
	}

	@Override
	public void startRequest() {
	}

	@Override
	public void finishRequest() {
	}

	/**
	 * Returns the {@link UpdateSink}.  In this implementation, this is done by
	 * with an {@link UpdateVisitor} which will visit each update operation
	 * and send the operation to the associated {@link UpdateEngineWorker}.
	 */
	@Override
	public UpdateSink getUpdateSink() {
		if (updateSink == null) {
			var worker = new KbUpdateEngineWorker((KbGraphStore) datasetGraph, inputBinding, context);
			updateSink = new UpdateVisitorSink(worker);
		}
		return updateSink;
	}
}
