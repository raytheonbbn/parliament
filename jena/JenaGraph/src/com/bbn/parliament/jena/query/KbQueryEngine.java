package com.bbn.parliament.jena.query;

import com.bbn.parliament.jena.graph.KbGraphStore;
import com.bbn.parliament.jena.query.optimize.KbOptimize;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.core.Substitute;
import com.hp.hpl.jena.sparql.engine.QueryEngineRegistry;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.engine.main.QueryEngineMain;
import com.hp.hpl.jena.sparql.util.Context;

/**
 * A query engine for Parliament. The <code>KbQueryEngine</code> is a simple
 * extension of <code>QueryEngineMain</code>. The real processing occurs via the
 * {@link KbOptimize} and {@link KbOpExecutor} classes.
 *
 * @author rbattle
 *
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
