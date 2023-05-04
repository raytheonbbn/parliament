package com.bbn.parliament.kb_graph;

import org.apache.jena.query.ARQ;
import org.apache.jena.sparql.engine.main.QC;
import org.apache.jena.sparql.engine.main.StageBuilder;
import org.apache.jena.sparql.engine.main.StageGenerator;
import org.apache.jena.sparql.pfunction.PropertyFunctionRegistry;

import com.bbn.parliament.kb_graph.query.KbOpExecutor;
import com.bbn.parliament.kb_graph.query.KbQueryEngine;
import com.bbn.parliament.kb_graph.query.KbStageGenerator;
import com.bbn.parliament.kb_graph.query.index.pfunction.EnableIndexing;
import com.bbn.parliament.kb_graph.query.optimize.KbOptimize;

public class Kb {
	private static volatile boolean initialized = false;

	static {
		doInitialization();
	}

	private static synchronized void doInitialization() {
		// Called at start.
		if (initialized) {
			return;
		}
		initialized = true;
		ARQ.init();
		// insert our op executor into the SPARQL engine
		QC.setFactory(ARQ.getContext(), KbOpExecutor.KbOpExecutorFactory);
		StageGenerator origGenerator = StageBuilder.chooseStageGenerator(ARQ.getContext());
		StageGenerator newGenerator = new KbStageGenerator(origGenerator);
		StageBuilder.setGenerator(ARQ.getContext(), newGenerator);

		KbQueryEngine.register();
		KbOptimize.register();

		registerPropertyFunctions();
	}

	private static void registerPropertyFunctions() {
		PropertyFunctionRegistry.get().put(EnableIndexing.URI, EnableIndexing.class);
	}

	/**
	 * Kb System initialization - normally, this is not explicitly called because
	 * all routes to use Parliament will cause initialization to occur. However,
	 * calling it repeatedly is safe and low cost.
	 */
	public static void init() {
		// don't do anything here, just forces initialization to occur.
	}
}
