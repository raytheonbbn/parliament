package com.bbn.parliament.kb_graph.query;

import org.apache.jena.query.Query;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.engine.Plan;
import org.apache.jena.sparql.engine.QueryEngineFactory;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.util.Context;

import com.bbn.parliament.kb_graph.KbGraphStore;

public class KbQueryEngineFactory implements QueryEngineFactory {
	@Override
	public boolean accept(Query query, DatasetGraph dataset, Context context) {
		return (dataset instanceof KbGraphStore);
	}

	@Override
	public Plan create(Query query, DatasetGraph dataset, Binding inputBinding,
		Context context) {
		KbGraphStore store = (KbGraphStore)dataset;
		KbQueryEngine engine = new KbQueryEngine(query, store, inputBinding, context);
		Plan plan = engine.getPlan();
		return plan;
	}

	@Override
	public boolean accept(Op op, DatasetGraph dataset, Context context) {
		return (dataset instanceof KbGraphStore);
	}

	@Override
	public Plan create(Op op, DatasetGraph dataset, Binding inputBinding,
		Context context) {
		KbGraphStore store = (KbGraphStore)dataset;
		KbQueryEngine engine = new KbQueryEngine(op, store, inputBinding, context);
		return engine.getPlan();
	}
}
