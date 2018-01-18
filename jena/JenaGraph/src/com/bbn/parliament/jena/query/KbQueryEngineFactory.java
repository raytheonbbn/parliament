package com.bbn.parliament.jena.query;

import com.bbn.parliament.jena.graph.KbGraphStore;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import com.hp.hpl.jena.sparql.engine.Plan;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.util.Context;

public class KbQueryEngineFactory implements
   com.hp.hpl.jena.sparql.engine.QueryEngineFactory {

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
