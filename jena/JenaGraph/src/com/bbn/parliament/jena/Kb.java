package com.bbn.parliament.jena;

import com.bbn.parliament.jena.query.KbStageGenerator;
import com.bbn.parliament.jena.query.index.pfunction.EnableIndexing;
import com.bbn.parliament.jena.query.optimize.KbOptimize;
import com.hp.hpl.jena.query.ARQ;
import com.hp.hpl.jena.sparql.engine.main.QC;
import com.hp.hpl.jena.sparql.engine.main.StageBuilder;
import com.hp.hpl.jena.sparql.engine.main.StageGenerator;
import com.hp.hpl.jena.sparql.pfunction.PropertyFunctionRegistry;

public class Kb {

   private static boolean initialized = false;

   static {
      doInitialization();
   }

   private static synchronized void doInitialization() {
      // Called at start.
      if (initialized)
         return;
      initialized = true;
      ARQ.init();
      // insert our op executor into the SPARQL engine
      QC.setFactory(ARQ.getContext(),
                    com.bbn.parliament.jena.query.KbOpExecutor.KbOpExecutorFactory);
      StageGenerator orig = (StageGenerator) ARQ.getContext()
            .get(ARQ.stageGenerator);
      StageGenerator generator = new KbStageGenerator(orig);
      StageBuilder.setGenerator(ARQ.getContext(), generator);

      com.bbn.parliament.jena.query.KbQueryEngine.register();
      KbOptimize.register();

      registerPropertyFunctions();
   }

   private static synchronized void registerPropertyFunctions() {
      PropertyFunctionRegistry.get().put(EnableIndexing.URI,
                                         EnableIndexing.class);
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
