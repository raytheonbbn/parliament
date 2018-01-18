package com.bbn.parliament.jena.query.optimize;

import java.util.ArrayList;
import java.util.List;

import com.bbn.parliament.jena.graph.KbGraph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.sparql.core.BasicPattern;
import com.hp.hpl.jena.sparql.engine.optimizer.reorder.ReorderProc;

public class DefaultCountTransformation extends AbstractCountTransformation {

   public DefaultCountTransformation(KbGraph graph) {
      super(graph);
   }

   @Override
   public ReorderProc reorderIndexes(BasicPattern pattern) {
      return new ReorderProc() {

         @Override
         public BasicPattern reorder(BasicPattern bgp) {
            List<Triple> orderedTriples = orderByCounts(bgp.getList(),
                                                        new ArrayList<Node>(),
                                                        1,
                                                        false);
            BasicPattern result = new BasicPattern();
            for (Triple t : orderedTriples) {
               result.add(t);
            }
            return result;
         }
      };
   }

}
