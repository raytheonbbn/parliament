// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
package com.bbn.parliament.jena.query;

import com.bbn.parliament.jena.Kb;
import com.bbn.parliament.jena.graph.KbGraph;
import com.bbn.parliament.jena.query.index.pfunction.algebra.OpIndexPropFunc;
import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.algebra.op.OpBGP;
import com.hp.hpl.jena.sparql.algebra.op.OpExt;
import com.hp.hpl.jena.sparql.algebra.op.OpFilter;
import com.hp.hpl.jena.sparql.algebra.op.OpJoin;
import com.hp.hpl.jena.sparql.algebra.op.OpLeftJoin;
import com.hp.hpl.jena.sparql.algebra.op.OpSequence;
import com.hp.hpl.jena.sparql.core.BasicPattern;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIterFilterExpr;
import com.hp.hpl.jena.sparql.engine.main.JoinClassifier;
import com.hp.hpl.jena.sparql.engine.main.LeftJoinClassifier;
import com.hp.hpl.jena.sparql.engine.main.OpExecutor;
import com.hp.hpl.jena.sparql.engine.main.OpExecutorFactory;
import com.hp.hpl.jena.sparql.engine.main.iterator.QueryIterJoin;
import com.hp.hpl.jena.sparql.engine.main.iterator.QueryIterLeftJoin;
import com.hp.hpl.jena.sparql.engine.main.iterator.QueryIterOptionalIndex;
import com.hp.hpl.jena.sparql.expr.Expr;
import com.hp.hpl.jena.sparql.expr.ExprList;

/**
 * An algebra executor for Parliament. The <code>KbOpExecutor</code> processes
 * BGPs, Index Property Functions, Filters, and Joins. Other algebra operations
 * are delegated to the base <code>OpExecutor</code>.
 *
 * @see SolverUtil
 * @author rbattle
 */
public class KbOpExecutor extends OpExecutor {

   /**
    * The factory for creating {@link KbOpExecutor}s. This is set as the default
    * factory for queries by the initialization method in {@link Kb}.
    */
   public final static OpExecutorFactory KbOpExecutorFactory = new OpExecutorFactory() {
      @Override
      public OpExecutor create(ExecutionContext execCxt) {
         return new KbOpExecutor(execCxt);
      }
   };

   private final boolean isForKB;
   private final KbGraph graph;

   protected KbOpExecutor(ExecutionContext execCxt) {
      super(execCxt);

      isForKB = (execCxt.getActiveGraph() instanceof KbGraph);
      if (isForKB) {
         graph = (KbGraph) execCxt.getActiveGraph();
      } else {
         graph = null;
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected QueryIterator execute(OpBGP opBGP, QueryIterator input) {
      if (!isForKB) {
         return super.execute(opBGP, input);
      }

      BasicPattern pattern = opBGP.getPattern();

      return SolverUtil.solve(pattern, input, execCxt, graph,
                              SolverUtil.DEFAULT_SOLVER_EXECUTOR);

   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected QueryIterator execute(OpExt opExt, QueryIterator input) {
      if (opExt instanceof OpIndexPropFunc) {
         OpIndexPropFunc opIndexPropFunc = (OpIndexPropFunc) opExt;
         Op subOp = opIndexPropFunc.getSubOp();
         QueryIterator it = input;
         if (subOp instanceof OpBGP) {
            // add pattern to index so that execution can get optimized
            opIndexPropFunc.getPattern().addAll(((OpBGP)subOp).getPattern());
         } else {
            it = executeOp(subOp, input);
         }

         return opIndexPropFunc.eval(it, execCxt);
      }
      return super.execute(opExt, input);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected QueryIterator execute(OpFilter opFilter, QueryIterator input) {
      if (opFilter.getSubOp() instanceof OpBGP) {
         return processFilter(opFilter, (OpBGP) opFilter.getSubOp(), input);
      } else if (opFilter.getSubOp() instanceof OpSequence) {
         OpSequence opSeq = (OpSequence) opFilter.getSubOp();
         QueryIterator ret = input;
         for (Op op : opSeq.getElements()) {
            if (op instanceof OpBGP) {
               ret = processFilter(opFilter, (OpBGP) op, ret);
            } else {
               ret = executeOp(op, ret);
            }
         }
         for (Expr expr : opFilter.getExprs()) {
            ret = new QueryIterFilterExpr(ret, expr, execCxt);
         }
         return ret;
      }
      return super.execute(opFilter, input);
   }


   private QueryIterator processFilter(OpFilter opFilter, OpBGP subOp,
         QueryIterator input) {
      return SolverUtil.solve(opFilter.getExprs(), subOp.getPattern(), input,
                              execCxt);

   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected QueryIterator execute(OpJoin opJoin, QueryIterator input) {
      // Look one level in for any filters with out-of-scope variables.
      boolean canDoLinear = JoinClassifier.isLinear(opJoin);

      if (canDoLinear)
         // Streamed evaluation
         return stream(opJoin.getLeft(), opJoin.getRight(), input);

      // Can't do purely indexed (e.g. a filter referencing a variable out of
      // scope is in the way)
      // To consider: partial substitution for improved performance (but does it
      // occur for real?)

      QueryIterator left = executeOp(opJoin.getLeft(), input);
      QueryIterator right = executeOp(opJoin.getRight(), root());
      QueryIterator qIter = new QueryIterJoin(left, right, execCxt);
      return qIter;
      // Worth doing anything about join(join(..))?
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected QueryIterator execute(OpLeftJoin opLeftJoin, QueryIterator input) {
      ExprList exprs = opLeftJoin.getExprs();
      if (exprs != null)
         exprs.prepareExprs(execCxt.getContext());

      // Do an indexed substitute into the right if possible.
      boolean canDoLinear = LeftJoinClassifier.isLinear(opLeftJoin);

      if (canDoLinear) {
         // Pass left into right for substitution before right side evaluation.
         // In an indexed left join, the LHS bindings are visible to the
         // RHS execution so the expression is evaluated by moving it to be
         // a filter over the RHS pattern.

         Op opLeft = opLeftJoin.getLeft();
         Op opRight = opLeftJoin.getRight();
         if (exprs != null)
            opRight = OpFilter.filter(exprs, opRight);
         QueryIterator left = executeOp(opLeft, input);
         QueryIterator qIter = new QueryIterOptionalIndex(left, opRight,
                                                          execCxt);
         return qIter;
      }

      // Not index-able.
      // Do it by sub-evaluation of left and right then left join.
      // Can be expensive if RHS returns a lot.
      // To consider: partial substitution for improved performance (but does it
      // occur for real?)

      QueryIterator left = executeOp(opLeftJoin.getLeft(), input);
      QueryIterator right = executeOp(opLeftJoin.getRight(), root());
      QueryIterator qIter = new QueryIterLeftJoin(left, right, exprs, execCxt);
      return qIter;
   }

   // Pass iterator from left directly into the right.
   protected QueryIterator stream(Op opLeft, Op opRight, QueryIterator input) {
      QueryIterator left = executeOp(opLeft, input);
      QueryIterator right = executeOp(opRight, left);
      return right;
   }
}
