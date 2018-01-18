package com.bbn.parliament.jena.query.index.pfunction;

import java.util.Collections;
import java.util.List;

import com.bbn.parliament.jena.graph.KbGraphStore;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.pfunction.PropFuncArg;
import com.hp.hpl.jena.sparql.pfunction.PropFuncArgType;
import com.hp.hpl.jena.sparql.pfunction.PropertyFunctionEval;
import com.hp.hpl.jena.sparql.util.IterLib;

public abstract class PFGraphsAsSubject extends PropertyFunctionEval {

   /**
    * Get a list of one or more node objects provided to the property function.
    *
    * @param pfArg
    *           The property function.
    * @return The list of node objects provided to the property function.
    */
   private static final List<Node> getNodeArguments(PropFuncArg pfArg) {
      List<Node> toReturn = Collections.emptyList();

      // This function is necessary to handle both
      // single or multiple additional entities.
      if (pfArg.isList()) {
         toReturn = pfArg.getArgList();
      } else if (pfArg.isNode()) {
         toReturn = Collections.singletonList(pfArg.getArg());
      }

      return toReturn;
   }

   protected PFGraphsAsSubject(PropFuncArgType objectType) {
      super(PropFuncArgType.PF_ARG_EITHER, objectType);
   }

   protected abstract boolean checkObject(Node node);

   protected abstract boolean processGraphObject(Binding binding, Node graphName, Graph graph,
         Node object, ExecutionContext context);

   @Override
   public QueryIterator execEvaluated(Binding binding, PropFuncArg subject,
         Node predicate, PropFuncArg object, ExecutionContext execCxt) {
      DatasetGraph dsg = execCxt.getDataset();

      if (!(dsg instanceof KbGraphStore)) {
         throw new JenaException(predicate
               + " may only be run against Parliament KBs.");
      }
      KbGraphStore kbGraphStore = (KbGraphStore) dsg;
      List<Node> graphs = getNodeArguments(subject);
      for (Node g : graphs) {
         if (!g.isURI()) {
            throw new JenaException("The subject must be the URI of a graph");
         }
         String uri = g.getURI();
         if (!KbGraphStore.DEFAULT_GRAPH_URI.equals(uri)) {
            Graph graph = kbGraphStore.getGraph(g);
            if (null == graph) {
               throw new JenaException("The subject must be the URI of a graph");
            }
         }

      }

      List<Node> objects = getNodeArguments(object);
      for (Node o : objects) {
         if (!checkObject(o)) {
            throw new JenaException(
                                    String.format("'%s' is an invalid object for %s",
                                                  o, predicate));
         }
      }

      boolean success = false;
      for (Node g : graphs) {
         Graph graph = null;
         if (KbGraphStore.DEFAULT_GRAPH_NODE.equals(g)) {
            graph = kbGraphStore.getDefaultGraph();
         } else {
            graph = kbGraphStore.getGraph(g);
         }
         for (Node o : objects) {
            success = processGraphObject(binding, g, graph, o, execCxt);
            if (!success) {
               break;
            }
         }
      }
      success = true;

      Node result = ResourceFactory.createPlainLiteral(success ? "Success"
                                                             : "Failure")
            .asNode();

      return IterLib.oneResult(binding, Var.alloc("result"), result, execCxt);
   }

}
