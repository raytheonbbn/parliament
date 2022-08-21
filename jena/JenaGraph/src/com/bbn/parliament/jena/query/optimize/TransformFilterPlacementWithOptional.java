// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.query.optimize;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.algebra.OpVars;
import com.hp.hpl.jena.sparql.algebra.op.OpBGP;
import com.hp.hpl.jena.sparql.algebra.op.OpConditional;
import com.hp.hpl.jena.sparql.algebra.op.OpFilter;
import com.hp.hpl.jena.sparql.algebra.op.OpQuadPattern;
import com.hp.hpl.jena.sparql.algebra.op.OpSequence;
import com.hp.hpl.jena.sparql.algebra.op.OpTable;
import com.hp.hpl.jena.sparql.algebra.optimize.TransformFilterPlacement;
import com.hp.hpl.jena.sparql.core.BasicPattern;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.expr.Expr;
import com.hp.hpl.jena.sparql.expr.ExprList;
import com.hp.hpl.jena.sparql.util.VarUtils;

// TODO: Remove and use TransformFilterPlacement in ARQ 2.8.8
public class TransformFilterPlacementWithOptional extends TransformFilterPlacement {
	static boolean doFilterPlacement = true ;

	public static Op transform(ExprList exprs, BasicPattern bgp)
	{
		if ( ! doFilterPlacement )
			return OpFilter.filter(exprs, new OpBGP(bgp)) ;

		Op op = transformFilterBGP(exprs, new HashSet<Var>(), bgp) ;
		// Remaining filters? e.g. ones mentioning var s not used anywhere.
		op = buildFilter(exprs, op) ;
		return op ;
	}

	public static Op transform(ExprList exprs, Node graphNode, BasicPattern bgp)
	{
		if ( ! doFilterPlacement )
			return OpFilter.filter(exprs, new OpQuadPattern(graphNode, bgp)) ;
		Op op =  transformFilterQuadPattern(exprs, new HashSet<Var>(), graphNode, bgp);
		op = buildFilter(exprs, op) ;
		return op ;
	}

	public TransformFilterPlacementWithOptional() {
	}

	@Override
	public Op transform(OpFilter opFilter, Op x)
	{
		if ( ! doFilterPlacement )
			return super.transform(opFilter, x) ;

		// Destructive use of exprs - copy it.
		ExprList exprs = new ExprList(opFilter.getExprs()) ;
		Set<Var> varsScope = new HashSet<>() ;

		Op op = transform(exprs, varsScope, x) ;
		if ( op == x )
			// Didn't do anything.
			return super.transform(opFilter, x) ;

		// Remaining exprs
		op = buildFilter(exprs, op) ;
		return op ;
	}

	private static Op transform(ExprList exprs, Set<Var> varsScope, Op op) {
		if (op instanceof OpBGP bgpOp) {
			return transformFilterBGP(exprs, varsScope, bgpOp);
		}

		if (op instanceof OpSequence seqOp) {
			return transformFilterSequence(exprs, varsScope, seqOp);
		}

		if (op instanceof OpQuadPattern quadPatternOp) {
			return transformFilterQuadPattern(exprs, varsScope, quadPatternOp);
		}

		if (op instanceof OpConditional conditionalOp) {
			return transformFilterConditional(exprs, varsScope, conditionalOp);
		}

		// Not special - advance the variable scope tracking.
		OpVars.patternVars(op, varsScope) ;
		return op ;
	}

	private static Op transformFilterBGP(ExprList exprs, Set<Var> patternVarsScope, OpBGP x)
	{
		return  transformFilterBGP(exprs, patternVarsScope, x.getPattern()) ;
	}

	private static Op transformFilterBGP(ExprList exprs, Set<Var> patternVarsScope, BasicPattern pattern)
	{
		// Any filters that depend on no variables.
		Op op = insertAnyFilter(exprs, patternVarsScope, null) ;

		for ( Triple triple : pattern )
		{
			OpBGP opBGP = getBGP(op) ;
			if ( opBGP == null )
			{
				// Last thing was not a BGP (so it likely to be a filter)
				// Need to pass the results from that into the next triple.
				// Which is a join and sequence is a special case of join
				// which always evaluates by passing results of the early
				// part into the next element of the sequence.

				opBGP = new OpBGP() ;
				op = OpSequence.create(op, opBGP) ;
			}

			opBGP.getPattern().add(triple) ;
			// Update variables in scope.
			VarUtils.addVarsFromTriple(patternVarsScope, triple) ;

			// Attempt to place any filters
			op = insertAnyFilter(exprs, patternVarsScope, op) ;
		}
		// Leave any remainign filter expressions - don't wrap up any as somethign else may take them.
		return op ;
	}

	/** Find the current OpBGP, or return null. */
	private static OpBGP getBGP(Op op) {
		if (op instanceof OpBGP bgpOp) {
			return bgpOp;
		}

		if (op instanceof OpSequence seqOp) {
			// Is last in OpSequence a BGP?
			List<Op> x = seqOp.getElements();
			if (x.size() > 0) {
				Op opTop = x.get(x.size()-1) ;
				if (opTop instanceof OpBGP bgpOp) {
					return bgpOp;
				}
				// Drop through
			}
		}
		return null;	// Can't find
	}

	private static Op transformFilterQuadPattern(ExprList exprs, Set<Var> patternVarsScope, OpQuadPattern pattern)
	{
		return transformFilterQuadPattern(exprs, patternVarsScope, pattern.getGraphNode(), pattern.getBasicPattern()) ;
	}

	private static Op transformFilterQuadPattern(ExprList exprs, Set<Var> patternVarsScope, Node graphNode, BasicPattern pattern)
	{
		// Any filters that depend on no variables.
		Op op = insertAnyFilter(exprs, patternVarsScope, null) ;
		// Any filters that depend on just the graph node.
		if ( Var.isVar(graphNode) )
		{
			patternVarsScope.add(Var.alloc(graphNode)) ;
			op = insertAnyFilter(exprs, patternVarsScope, op) ;
		}


		for ( Triple triple : pattern )
		{
			OpQuadPattern opQuad = getQuads(op) ;
			if ( opQuad == null )
			{
				opQuad = new OpQuadPattern(graphNode, new BasicPattern()) ;
				op = OpSequence.create(op, opQuad) ;
			}

			opQuad.getBasicPattern().add(triple) ;
			// Update varaibles in scope.
			VarUtils.addVarsFromTriple(patternVarsScope, triple) ;
			// Attempt to place any filters
			op = insertAnyFilter(exprs, patternVarsScope, op) ;
		}
		return op ;
	}

	/** Find the current OpQuadPattern, or return null. */
	private static OpQuadPattern getQuads(Op op) {
		if (op instanceof OpQuadPattern quadPatternOp) {
			return quadPatternOp;
		}

		if (op instanceof OpSequence seqOp) {
			// Is last in OpSequence an BGP?
			List<Op> x = seqOp.getElements();
			if (x.size() > 0) {
				Op opTop = x.get(x.size()-1) ;
				if (opTop instanceof OpQuadPattern topQuadPatternOp) {
					return topQuadPatternOp;
				}
				// Drop through
			}
		}
		return null;	// Can't find
	}

	private static Op transformFilterSequence(ExprList exprs, Set<Var> varScope, OpSequence opSequence)
	{
		List<Op> ops = opSequence.getElements() ;

		// Any filters that depend on no variables.
		Op op = insertAnyFilter(exprs, varScope, null) ;

		for ( Iterator<Op> iter = ops.iterator() ; iter.hasNext() ; )
		{
			Op seqElt = iter.next() ;
			// Process the sequence element.  This may insert filters (sequence or BGP)
			seqElt = transform(exprs, varScope, seqElt) ;
			// Merge into sequence.
			op = OpSequence.create(op, seqElt) ;
			// Place any filters now ready.
			op = insertAnyFilter(exprs, varScope, op) ;
		}
		return op ;
	}

	private static Op transformFilterConditional(ExprList exprs, Set<Var> varScope, OpConditional opConditional) {

		// Any filters that depend on no variables.
		Op op = insertAnyFilter(exprs, varScope, null) ;

		Op left = opConditional.getLeft();

		left = transform(exprs, varScope, left);

		Op right = opConditional.getRight();

		op = new OpConditional(left, right);

		op = insertAnyFilter(exprs, varScope, op);
		return op;

	}
	// ---- Utilities

	/** For any expression now in scope, wrap the op with a filter */
	private static Op insertAnyFilter(ExprList exprs, Set<Var> patternVarsScope, Op op)
	{
		Op ret = op;
		for ( Iterator<Expr> iter = exprs.iterator() ; iter.hasNext() ; )
		{
			Expr expr = iter.next() ;
			// Cache
			Set<Var> exprVars = expr.getVarsMentioned() ;
			if ( patternVarsScope.containsAll(exprVars) )
			{
				if ( ret == null )
					ret = OpTable.unit() ;
				ret = OpFilter.filter(expr, ret) ;
				iter.remove() ;
			}
		}
		return ret ;
	}

	/** Place expressions around an Op */
	private static Op buildFilter(ExprList exprs, Op op)
	{
		if ( exprs.isEmpty() )
			return op ;

		Op ret = op;
		for ( Iterator<Expr> iter = exprs.iterator() ; iter.hasNext() ; )
		{
			Expr expr = iter.next() ;
			if ( ret == null )
				ret = OpTable.unit() ;
			ret = OpFilter.filter(expr, ret) ;
			iter.remove();
		}
		return ret ;
	}
}
