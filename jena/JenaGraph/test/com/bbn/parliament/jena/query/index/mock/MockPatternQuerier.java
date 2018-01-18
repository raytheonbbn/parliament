package com.bbn.parliament.jena.query.index.mock;

import java.util.ArrayList;
import java.util.List;

import com.bbn.parliament.jena.query.index.IndexPatternQuerier;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.sparql.core.BasicPattern;
import com.hp.hpl.jena.sparql.core.Substitute;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.engine.binding.BindingFactory;
import com.hp.hpl.jena.sparql.engine.binding.BindingMap;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIterPlainWrapper;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIterRepeatApply;
import com.hp.hpl.jena.sparql.util.IterLib;

public class MockPatternQuerier implements IndexPatternQuerier {

   public static String NAMESPACE = "http://example.org/mock#";
   private boolean estimated;
   private boolean queried;
   private boolean examined;
   private static int counter = 0;
   private int numItems;
   private boolean hasBinding;

   public MockPatternQuerier(int numItems) {
      this.numItems = numItems;
   }

   @Override
   public long estimate(BasicPattern pattern) {
      estimated = true;
      return numItems;
   }

   public boolean hasBinding() {
      return hasBinding;
   }

   @Override
   public QueryIterator query(final BasicPattern pattern, QueryIterator input,
         final ExecutionContext context) {
      queried = true;
      QueryIterator ret = new QueryIterRepeatApply(input, context) {

         @Override
         protected QueryIterator nextStage(Binding binding) {
            hasBinding = !binding.isEmpty();
            BasicPattern bgp = Substitute.substitute(pattern, binding);
            List<Binding> bindings = new ArrayList<>();
            for (int i = 0; i < numItems; i++) {
               BindingMap b = BindingFactory.create(binding);
               boolean changed = false;
               for (Triple t : bgp) {
                  if (t.getSubject().isVariable()) {
                     b.add(Var.alloc(t.getSubject()), Node.createAnon(AnonId.create("node" + (counter++))));
                     changed = true;
                  }
                  if (t.getObject().isVariable()) {
                     b.add(Var.alloc(t.getObject()), Node.createAnon(AnonId.create("node" + (counter++))));
                     changed = true;
                  }
               }
               if (changed) {
                  bindings.add(b);
               }
            }
            if (bindings.size() > 0) {
               return new QueryIterPlainWrapper(bindings.iterator(), context);
            } else {
               return IterLib.result(binding, context);
            }
         }
      };
      return ret;
   }

   @Override
   public BasicPattern examine(BasicPattern pattern) {
      examined = true;
      BasicPattern p = new BasicPattern();
      for (Triple t : pattern) {
         Node predicate = t.getPredicate();
         if (predicate.isURI() && NAMESPACE.equals(predicate.getNameSpace())) {
            p.add(t);
         }
      }
      return p;
   }

   /**
    * @return the isEstimated
    */
   public boolean isEstimated() {
      return estimated;
   }

   /**
    * @return the isQueried
    */
   public boolean isQueried() {
      return queried;
   }

   /**
    * @return the isExamined
    */
   public boolean isExamined() {
      return examined;
   }
}
