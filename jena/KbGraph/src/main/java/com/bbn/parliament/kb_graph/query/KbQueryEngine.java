package com.bbn.parliament.kb_graph.query;

import org.apache.jena.query.Query;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.core.Substitute;
import org.apache.jena.sparql.engine.QueryEngineRegistry;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.main.QueryEngineMain;
import org.apache.jena.sparql.util.Context;

import com.bbn.parliament.kb_graph.KbGraphStore;
import com.bbn.parliament.kb_graph.query.optimize.KbOptimize;

/**
 * A query engine for Parliament. The <code>KbQueryEngine</code> is a simple
 * extension of <code>QueryEngineMain</code>. The real processing occurs via the
 * {@link KbOptimize} and {@link KbOpExecutor} classes.
 *
 * @author rbattle
 */
public class KbQueryEngine extends QueryEngineMain {
	public static void register() {
		QueryEngineRegistry.addFactory(factory);
	}

	public static void unregister() {
		QueryEngineRegistry.removeFactory(factory);
	}

	private static KbQueryEngineFactory factory = new KbQueryEngineFactory();

	private Binding initialInput;

	public KbQueryEngine(Op op, KbGraphStore dataset, Binding input,
		Context context) {
		super(op, dataset, input, context);
		initialInput = input;
	}

	public KbQueryEngine(Query query, KbGraphStore dataset, Binding input,
		Context context) {
		super(query, dataset, input, context);
		initialInput = input;
	}

	@Override
	protected Op modifyOp(Op op) {
		Op o = Substitute.substitute(op, initialInput);
		// Optimize (high-level)
		o = super.modifyOp(o);

		// Record it.
		setOp(o);

		return o;
	}
}
